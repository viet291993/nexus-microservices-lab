#!/bin/sh
set -eu

# ==========================================
# SCRIPT CẤU HÌNH TỰ ĐỘNG CDC (CDC PROVISIONING) - ROBUST VERSION
# ==========================================

apk add --no-cache curl jq >/dev/null 2>&1 || { echo "❌ Lỗi: Không cài được curl/jq"; exit 1; }

# Validate required environment variables (POSIX sh)
for var in POSTGRES_USER POSTGRES_PASSWORD ELASTIC_PASSWORD; do
  eval val=\$$var
  if [ -z "${val:-}" ]; then
    echo "❌ Lỗi: Biến môi trường $var chưa được thiết lập."
    exit 1
  fi
done

# Safe defaults cho DLQ (dùng bởi elasticsearch-sink connector)
# tolerance=all: chuyển message lỗi sang DLQ thay vì dừng connector
DLQ_REPLICATION_FACTOR="${DLQ_REPLICATION_FACTOR:-1}"
DLQ_TOLERANCE="${DLQ_TOLERANCE:-all}"
export DLQ_REPLICATION_FACTOR DLQ_TOLERANCE

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

render_connector_payload() {
  file="$1"
  jq \
    --arg postgresUser "$POSTGRES_USER" \
    --arg postgresPassword "$POSTGRES_PASSWORD" \
    --arg elasticPassword "$ELASTIC_PASSWORD" \
    --arg dlqTolerance "$DLQ_TOLERANCE" \
    --arg dlqReplicationFactor "$DLQ_REPLICATION_FACTOR" \
    '
      .config |= (
        if has("database.user") then .["database.user"] = $postgresUser else . end |
        if has("database.password") then .["database.password"] = $postgresPassword else . end |
        if has("connection.password") then .["connection.password"] = $elasticPassword else . end |
        if has("errors.tolerance") then .["errors.tolerance"] = $dlqTolerance else . end |
        if has("errors.deadletterqueue.topic.replication.factor") then
          .["errors.deadletterqueue.topic.replication.factor"] = $dlqReplicationFactor
        else .
        end
      )
    ' "$file"
}

register_connector() {
  NAME=$1
  FILE=$2
  echo "🔍 Kiểm tra Connector: $NAME"
  if curl -s -f "http://kafka-connect:8083/connectors/$NAME" > /dev/null; then
    echo "⚠️ Connector $NAME đã tồn tại, bỏ qua đăng ký mới."
  else
    echo "🚀 Đang đăng ký Connector: $NAME..."
    # Render connector JSON safely (supports special characters)
    TMP_FILE="$(mktemp)"
    render_connector_payload "$FILE" > "$TMP_FILE"

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
