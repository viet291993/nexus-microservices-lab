/**
 * Điểm khởi động (Entry Point) của ứng dụng Inventory Service - NestJS.
 *
 * Khác với mode HTTP truyền thống (app.listen), ở đây chúng ta tạo một
 * "Hybrid Application" vừa lắng nghe HTTP vừa lắng nghe Kafka Transport.
 *
 * Hybrid giúp Inventory Service đồng thời:
 *   - Nhận request REST API thông thường (ví dụ: GET /inventory/health).
 *   - Lắng nghe Message từ Kafka Topic "order-events-topic" (từ Order Service Java).
 */

import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { ValidationPipe } from '@nestjs/common';
import { AppModule } from './app.module';

async function bootstrap(): Promise<void> {
  // Bước 1: Tạo ứng dụng HTTP bình thường (Express engine).
  const app = await NestFactory.create(AppModule);

  // Kích hoạt ValidationPipe toàn cục. 
  // transform: true cực kỳ quan quan trọng để class-transformer có thể 
  // chuyển Plain JSON từ Kafka sang Instance của Class (DTO) nhằm thực thi các decorator validation.
  app.useGlobalPipes(new ValidationPipe({ transform: true }));

  // Bước 2: Gắn thêm một "tai nghe" Kafka vào ứng dụng (Hybrid Microservice).
  // Từ giờ, mỗi khi có message xuất hiện trên Kafka Topic, NestJS sẽ tự động
  // gọi đến handler tương ứng (decorator @EventPattern / @MessagePattern).
  app.connectMicroservice<MicroserviceOptions>({
    transport: Transport.KAFKA,
    options: {
      client: {
        clientId: process.env.KAFKA_CLIENT_ID || 'inventory-service-client',
        brokers: (process.env.KAFKA_BROKERS || 'localhost:9092').split(','),
      },
      consumer: {
        groupId: process.env.KAFKA_GROUP_ID || 'inventory-service-group',
      },
    },
  });

  // Bước 3: Khởi động TẤT CẢ microservice transports (Kafka) trước.
  await app.startAllMicroservices();

  // Bước 4: Cuối cùng mới mở cổng HTTP (port 8082).
  const port = process.env.PORT || 8082;
  await app.listen(port);
  console.log(`🚀 [INVENTORY] Inventory Service đang chạy tại http://localhost:${port}`);
  console.log(`📡 [INVENTORY] Kafka Consumer đã sẵn sàng lắng nghe topic "order-events-topic"`);
}

bootstrap();
