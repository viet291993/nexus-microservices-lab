#!/usr/bin/env bash
set -euo pipefail

# ==========================================
# SCRIPT CẤU HÌNH TỰ ĐỘNG CDC (CDC PROVISIONING) - ROBUST VERSION
# ==========================================

# Validate required environment variables
for var in POSTGRES_USER POSTGRES_PASSWORD ELASTIC_PASSWORD; do
  if [ -z "${!var:-}" ]; then
    echo "❌ Lỗi: Biến môi trường $var chưa được thiết lập."
    exit 1
  fi
done
# Safe defaults cho DLQ (dùng bởi elasticsearch-sink connector)
export DLQ_REPLICATION_FACTOR="${DLQ_REPLICATION_FACTOR:-1}"
export DLQ_TOLERANCE="${DLQ_TOLERANCE:-none}"

echo "⏳ Đợi Connect sẵn sàng..."
MAX_RETRIES=30
RETRY=0
TMP_FILE=""

cleanup_tmp() {
  if [ -n "$TMP_FILE" ] && [ -f "$TMP_FILE" ]; then
    rm -f "$TMP_FILE"
  fi
}

trap cleanup_tmp EXIT
# Loop cho đến khi Kafka Connect REST API trả về 200
until curl -s -f --connect-timeout 5 --max-time 10 http://kafka-connect:8083/connectors > /dev/null; do
  RETRY=$((RETRY + 1))
  if [ "$RETRY" -gt "$MAX_RETRIES" ]; then
    echo "❌ Lỗi: Kafka Connect không sẵn sàng sau $((MAX_RETRIES * 5)) giây."
    exit 1
  fi
  echo "Kafka Connect chưa sẵn sàng (Lần $RETRY/$MAX_RETRIES), đang đợi..."
  sleep 5
done

register_connector() {
  NAME=$1
  FILE=$2
  echo "🔍 Kiểm tra Connector: $NAME"
  if curl -s -f "http://kafka-connect:8083/connectors/$NAME" > /dev/null; then
    echo "⚠️ Connector $NAME đã tồn tại, bỏ qua đăng ký mới."
  else
    echo "🚀 Đang đăng ký Connector: $NAME..."
    # Substitute environment variables in the JSON file
    # Use secure temp file (mktemp) for credential storage
    TMP_FILE=$(mktemp -t "connector-$NAME-XXXXXX.json")
    while IFS= read -r line; do
      line="${line//\$\{POSTGRES_USER\}/$POSTGRES_USER}"
      line="${line//\$\{POSTGRES_PASSWORD\}/$POSTGRES_PASSWORD}"
      line="${line//\$\{DLQ_REPLICATION_FACTOR\}/$DLQ_REPLICATION_FACTOR}"
      line="${line//\$\{DLQ_TOLERANCE\}/$DLQ_TOLERANCE}"
      line="${line//\$\{ELASTIC_PASSWORD\}/$ELASTIC_PASSWORD}"
      printf '%s\n' "$line"
    done < "$FILE" > "$TMP_FILE"

    RESPONSE=$(curl -s -w "%{http_code}" --connect-timeout 5 --max-time 15 -X POST -H "Content-Type: application/json" --data @"$TMP_FILE" http://kafka-connect:8083/connectors)
    rm -f "$TMP_FILE"
    HTTP_CODE=$(printf '%s' "$RESPONSE" | tail -c 3)
    BODY=${RESPONSE%???}
    if [[ "$HTTP_CODE" =~ ^[0-9]+$ ]] && { [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 200 ]; }; then
      echo "✅ Đăng ký $NAME thành công (HTTP $HTTP_CODE)."
    else
      echo "❌ Lỗi khi đăng ký $NAME (Mã lỗi: $HTTP_CODE)."
      echo "🔍 Response body từ Kafka Connect:"
      echo "$BODY"
      exit 1
    fi
  fi
}

register_connector "postgres-source" "/connectors/postgres-source.json"
register_connector "elasticsearch-sink" "/connectors/elasticsearch-sink.json"

printf "\n✅ CDC Provisioning hoàn tất!\n"
