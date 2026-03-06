#!/bin/bash

# ==========================================
# SCRIPT QUẢN LÝ TẬP TRUNG NEXUS LAB (Automation)
# ==========================================

set -euo pipefail
IFS=$'\n\t'
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"

trap 'echo "ERROR: Script failed at line $LINENO in $0"; exit 1' ERR

echo -e "\n--- [ Nexus Lab Manager ] ---\n"

if [ $# -eq 0 ]; then
  echo "Sử dụng: $0 {start|stop|restart|logs|status|clean|prune} [service_name]"
  exit 1
fi

case "${1-}" in
  start)
    echo -e "🚀 Khởi động toàn bộ hạ tầng Lab..."
    docker compose -f "$COMPOSE_FILE" up -d
    echo -e "✅ Đã gửi lệnh khởi chạy. Đang đợi các container đồng bộ..."
    ;;
  stop)
    echo -e "🛑 Đang dừng toàn bộ các dịch vụ..."
    docker compose -f "$COMPOSE_FILE" stop
    ;;
  restart)
    if [ -z "${2:-}" ]; then
      echo "Vui lòng chỉ định tên service để restart. Ví dụ: $0 restart kafka-connect"
      exit 1
    fi
    echo -e "🔄 Đang khởi động lại dịch vụ: $2"
    docker compose -f "$COMPOSE_FILE" restart "$2"
    ;;
  logs)
    if [ -z "${2:-}" ]; then
      echo "💡 Gợi ý: Dùng './manage.sh logs <service_name>' để xem log cụ thể."
      docker compose -f "$COMPOSE_FILE" logs --tail=100 -f
    else
      docker compose -f "$COMPOSE_FILE" logs -f "$2"
    fi
    ;;
  status)
    echo "📊 Trạng thái các Container:"
    docker compose -f "$COMPOSE_FILE" ps
    ;;
  clean)
    echo -e "⚠️ CẢNH BÁO: Xóa toàn bộ Container và DỮ LIỆU (Volumes) của project!"
    read -r -p "Bạn có chắc chắn muốn tiếp tục? (y/N) " confirm
    if [[ "$confirm" =~ ^[yY]$ ]]; then
      docker compose -f "$COMPOSE_FILE" down -v
      echo "🧹 Đã dọn dẹp xong tài nguyên của project."
      echo -e "\n💡 Mẹo: Dùng './manage.sh prune' để xóa luôn các Volume rác khác."
    else
      echo "❌ Đã hủy thao tác clean."
    fi
    ;;
  prune)
    echo -e "⚠️ Thao tác này sẽ dọn dẹp TOÀN BỘ Volume thừa (Anonymous Volumes) không sử dụng trong hệ thống!"
    read -r -p "Bạn có chắc chắn muốn tiếp tục? (y/N) " confirm
    if [[ "$confirm" =~ ^[yY]$ ]]; then
      echo -e "🧹 Đang dọn dẹp TOÀN BỘ Volume thừa (Anonymous Volumes)..."
      docker volume prune -f
      echo "✅ Đã dọn dẹp sạch sẽ các Volume thừa."
    else
      echo "❌ Đã hủy thao tác prune volumes."
    fi
    ;;
  *)
    echo "Sử dụng: $0 {start|stop|restart|logs|status|clean|prune}"
    ;;
esac

echo -e "\n----------------------------\n"
