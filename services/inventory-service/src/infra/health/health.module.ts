import { Module } from '@nestjs/common';
import { HealthController } from './health.controller';

/**
 * HealthModule gom nhóm tất cả các thành phần liên quan đến health-check của service.
 *
 * - Chỉ chứa các endpoint/infrastructure phục vụ việc theo dõi trạng thái hệ thống.
 * - Tách biệt khỏi các module domain (ví dụ: InventoryModule) để kiến trúc rõ ràng hơn.
 */
@Module({
  controllers: [HealthController],
})
export class HealthModule {}
