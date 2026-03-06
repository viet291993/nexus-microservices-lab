#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=========================================="
echo "    NEXUS MICROSERVICES - CLEANUP SCRIPT"
echo "=========================================="

echo "[1/4] Dừng toàn bộ các tiến trình Java/Spring Boot đang chạy..."
# Tìm và kill các tiến trình java có chứa đường dẫn nexus-microservices-lab
PIDS=$(pgrep -f "java.*nexus-microservices-lab" || true)

if [ -z "$PIDS" ]; then
    echo "  -> Không có tiến trình Spring Boot nào đang chạy."
else
    echo "  -> Đang gửi SIGTERM (Graceful shutdown) tới PIDs: $PIDS"
    for pid in $PIDS; do kill -15 "$pid" 2>/dev/null || true; done
    for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
        sleep 1
        STILL_RUNNING=$(pgrep -f "java.*nexus-microservices-lab" || true)
        [ -z "$STILL_RUNNING" ] && break
    done
    if [ -n "$STILL_RUNNING" ]; then
        echo "  -> Đang ép buộc dừng (kill -9) còn lại..."
        for pid in $STILL_RUNNING; do kill -9 "$pid" 2>/dev/null || true; done
    fi
    echo "  -> Đã dọn dẹp xong tiến trình."
fi

echo ""
echo "[2/4] Dừng và xóa toàn bộ Docker Containers..."
docker compose -f "$REPO_ROOT/infra/docker-compose.yml" down
echo "  -> Đã hạ toàn bộ Infra (PostgreSQL, Kafka, Redis...)."

echo ""
echo "[3/4] Dọn dẹp các thư mục build (target/)..."
# Chạy Maven clean để xoá toàn bộ thư mục target do Spring Boot sinh ra
(cd "$REPO_ROOT" && mvn clean -q)
echo "  -> Đã xóa sạch thư mục 'target/' ở tất cả microservices."

echo ""
echo "[4/4] Dọn vụn file log (/tmp/)..."
rm -f /tmp/config-server.log
rm -f /tmp/eureka-server.log
rm -f /tmp/order-service.log
rm -f /tmp/api-gateway.log
echo "  -> Đã dọn dẹp files log trong /tmp/."

echo ""
echo "=========================================="
echo " ✅ HOÀN TẤT DỌN DẸP TOÀN BỘ DỰ ÁN !"
echo "=========================================="
