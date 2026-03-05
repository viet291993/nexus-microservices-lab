#!/bin/sh

# ==========================================
# SCRIPT CẤU HÌNH TỰ ĐỘNG CDC (CDC PROVISIONING) - ROBUST VERSION
# ==========================================

echo "⏳ Đợi Connect sẵn sàng..."
# Loop cho đến khi Kafka Connect REST API trả về 200
until curl -s -f http://kafka-connect:8083/connectors > /dev/null; do
  echo "Kafka Connect chưa sẵn sàng, đang đợi..."
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
    RESPONSE=$(curl -s -w "%{http_code}" -X POST -H "Content-Type: application/json" --data @"$FILE" http://kafka-connect:8083/connectors)
    if [ "$RESPONSE" -eq 201 ]; then
      echo "✅ Đăng ký $NAME thành công."
    else
      echo "❌ Lỗi khi đăng ký $NAME (Mã lỗi: $RESPONSE)."
    fi
  fi
}

register_connector "postgres-source" "/connectors/postgres-source.json"
register_connector "elasticsearch-sink" "/connectors/elasticsearch-sink.json"

echo -e "\n✅ CDC Provisioning hoàn tất!"
