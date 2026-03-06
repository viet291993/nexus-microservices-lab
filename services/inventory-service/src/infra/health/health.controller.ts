import { Controller, Get, HttpException, HttpStatus } from '@nestjs/common';
import { InjectConnection } from '@nestjs/mongoose';
import { Connection } from 'mongoose';

/**
 * Controller chịu trách nhiệm cung cấp endpoint kiểm tra sức khỏe tổng thể của service.
 */
@Controller()
export class HealthController {
  constructor(@InjectConnection() private readonly connection: Connection) {}

  /**
   * Endpoint health đơn giản (Liveness check).
   */
  @Get('health')
  getHealth() {
    return { status: 'UP', timestamp: new Date().toISOString() };
  }

  /**
   * Endpoint readiness (Readiness check).
   * Kiểm tra xem các dependency quan trọng (như MongoDB) đã sẵn sàng chưa.
   */
  @Get('readiness')
  getReadiness() {
    const isDbConnected =
      this.connection && this.connection.readyState === 1; // 1 = connected

    if (!isDbConnected) {
      throw new HttpException(
        {
          status: 'DOWN',
          timestamp: new Date().toISOString(),
          database: 'DISCONNECTED',
        },
        HttpStatus.SERVICE_UNAVAILABLE,
      );
    }

    return {
      status: 'UP',
      timestamp: new Date().toISOString(),
      database: 'CONNECTED',
    };
  }
}
