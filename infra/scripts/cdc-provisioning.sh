#!/bin/sh

# ==========================================
# SCRIPT CẤU HÌNH TỰ ĐỘNG CDC (CDC PROVISIONING) - ROBUST VERSION
# ==========================================

echo "⏳ Đợi Connect sẵn sàng..."
MAX_RETRIES=30
RETRY=0
# Loop cho đến khi Kafka Connect REST API trả về 200
until curl -s -f http://kafka-connect:8083/connectors > /dev/null; do
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
    # We use a temporary file to hold the substituted content
    TMP_FILE="/tmp/$NAME.json"
    sed "s|\${POSTGRES_USER}|$POSTGRES_USER|g; \
         s|\${POSTGRES_PASSWORD}|$POSTGRES_PASSWORD|g; \
         s|\${DLQ_REPLICATION_FACTOR}|$DLQ_REPLICATION_FACTOR|g; \
         s|\${DLQ_TOLERANCE}|$DLQ_TOLERANCE|g" "$FILE" > "$TMP_FILE"
    
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: application/json" --data @"$TMP_FILE" http://kafka-connect:8083/connectors)
    if [ "$RESPONSE" -eq 201 ] || [ "$RESPONSE" -eq 200 ]; then
      echo "✅ Đăng ký $NAME thành công (HTTP $RESPONSE)."
    else
      echo "❌ Lỗi khi đăng ký $NAME (Mã lỗi: $RESPONSE)."
      exit 1
    fi
  fi
}

register_connector "postgres-source" "/connectors/postgres-source.json"
register_connector "elasticsearch-sink" "/connectors/elasticsearch-sink.json"

printf "\n✅ CDC Provisioning hoàn tất!\n"
