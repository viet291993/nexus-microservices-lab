import { Controller, Get } from '@nestjs/common';

/**
 * Controller chịu trách nhiệm cung cấp endpoint kiểm tra sức khỏe tổng thể của service.
 *
 * - Mục đích:
 *   - Cho các hệ thống giám sát (Eureka, Kubernetes, Docker, Prometheus, v.v.) gọi vào
 *     để kiểm tra xem service còn "sống" hay không.
 *   - Làm endpoint chung cho toàn bộ Inventory Service, không gắn trực tiếp với domain "kho hàng".
 *
 * - Đường dẫn:
 *   - GET /health
 *   - Trả về JSON đơn giản để các tool health-check có thể parse dễ dàng.
 */
@Controller()
export class HealthController {
  /**
   * Endpoint health đơn giản.
   *
   * - Trả về:
   *   - status: 'UP' => Service đang chạy bình thường.
   * - Có thể mở rộng sau này:
   *   - Kiểm tra kết nối MongoDB, Kafka, hoặc các dịch vụ phụ trợ khác.
   */
  @Get('health')
  getHealth() {
    return { status: 'UP' };
  }
}
