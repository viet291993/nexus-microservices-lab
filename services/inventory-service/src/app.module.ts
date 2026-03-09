/**
 * Module chính của ứng dụng Inventory Service (NestJS).
 *
 * Module này đăng ký:
 *   - ConfigModule: Đọc file .env để nạp biến môi trường (MONGODB_URI, KAFKA_BROKERS...).
 *   - MongooseModule: Kết nối tới MongoDB (Database NoSQL lưu trữ thông tin kho hàng).
 *   - InventoryModule: Module con chứa toàn bộ logic nghiệp vụ xử lý kho.
 */

import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { MongooseModule } from '@nestjs/mongoose';
import { EurekaModule } from './eureka/eureka.module';
import { HealthModule } from './infra/health/health.module';
import { InventoryModule } from './inventory/inventory.module';

@Module({
  imports: [
    // Nạp biến môi trường từ file .env vào process.env.
    // isGlobal: true => Mọi module con đều truy cập được mà không cần import lại.
    ConfigModule.forRoot({
      isGlobal: true,
    }),

    // Kết nối MongoDB sử dụng URI từ biến môi trường MONGODB_URI.
    // useFactory + inject: Lấy giá trị từ ConfigService thay vì hardcode.
    MongooseModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        uri: config.get<string>(
          'MONGODB_URI',
          'mongodb://root:rootpassword@localhost:27017/nexus_inventory?authSource=nexus_inventory',
        ),
        // Tự động thử lại kết nối 5 lần, mỗi lần cách nhau 5 giây nếu MongoDB chưa sẵn sàng (Wait for DB)
        retryAttempts: 5,
        retryDelay: 5000,
      }),
    }),

    // Module nghiệp vụ kho hàng (chứa Kafka Consumer, Schema, Service).
    InventoryModule,

    // Module quản lý đăng ký Eureka cho Inventory Service.
    EurekaModule,

    // Module hạ tầng cung cấp endpoint /health cho toàn bộ service.
    HealthModule,
  ],
})
export class AppModule {}
