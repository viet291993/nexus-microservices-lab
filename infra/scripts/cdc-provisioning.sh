#!/bin/sh

# ==========================================
# SCRIPT CẤU HÌNH TỰ ĐỘNG CDC (CDC PROVISIONING)
# ==========================================

echo "⏳ Đợi 10 giây để đảm bảo Connect ổn định hoàn toàn..."
sleep 10

echo "🚀 Đang đăng ký Postgres Source Connector..."
curl -X POST -H "Content-Type: application/json" --data @/connectors/postgres-source.json http://kafka-connect:8083/connectors

echo -e "\n🚀 Đang đăng ký Elasticsearch Sink Connector..."
curl -X POST -H "Content-Type: application/json" --data @/connectors/elasticsearch-sink.json http://kafka-connect:8083/connectors

echo -e "\n✅ CDC Provisioning hoàn tất!"
