#!/bin/bash

# ==========================================
# SCRIPT KHỞI TẠO KAFKA CONNECT PLUGIN - V2 (FIXED)
# ==========================================

echo "🛠️ Đang cài đặt Debezium Postgres Connector..."
# Sử dụng :latest để tránh lỗi component not found do version không khớp trên Hub
confluent-hub install --no-prompt debezium/debezium-connector-postgresql:latest

echo "🛠️ Đang cài đặt Elasticsearch Sink Connector..."
confluent-hub install --no-prompt confluentinc/kafka-connect-elasticsearch:latest

echo "🚀 Đang liệt kê các plugin đã cài đặt:"
ls -R /usr/share/confluent-hub-components

echo "🚀 Khởi chạy Kafka Connect..."
/etc/confluent/docker/run
