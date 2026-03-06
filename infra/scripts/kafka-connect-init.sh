#!/usr/bin/env bash
set -euo pipefail
DEBEZIUM_VERSION="${DEBEZIUM_VERSION:-2.5.4}"
ES_SINK_VERSION="${ES_SINK_VERSION:-15.1.1}"

echo "🛠️ Đang cài đặt Debezium Postgres Connector v$DEBEZIUM_VERSION..."
confluent-hub install --no-prompt "debezium/debezium-connector-postgresql:$DEBEZIUM_VERSION"

echo "🛠️ Đang cài đặt Elasticsearch Sink Connector v$ES_SINK_VERSION..."
confluent-hub install --no-prompt "confluentinc/kafka-connect-elasticsearch:$ES_SINK_VERSION"

echo "🚀 Đang liệt kê các plugin đã cài đặt:"
ls -R /usr/share/confluent-hub-components

echo "🚀 Khởi chạy Kafka Connect..."
exec /etc/confluent/docker/run
