/**
 * Module Inventory — Đăng ký tất cả các thành phần liên quan đến nghiệp vụ kho hàng.
 *
 * Bao gồm:
 *   - MongooseModule: Đăng ký Schema "Inventory" để Mongoose biết cách đọc/ghi collection.
 *   - ClientsModule (Kafka): Đăng ký Kafka Producer Client để gửi phản hồi Saga ngược lại.
 *   - InventoryController: Xử lý event từ Kafka Topic.
 *   - InventoryService: Logic nghiệp vụ trừ kho.
 */

import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { ClientsModule, Transport } from '@nestjs/microservices';
import { ConfigService } from '@nestjs/config';
import { Inventory, InventorySchema } from './schemas/inventory.schema';
import { InventoryController } from './inventory.controller';
import { InventoryService } from './inventory.service';

@Module({
    imports: [
        // Đăng ký Schema MongoDB cho collection "inventories".
        MongooseModule.forFeature([
            { name: Inventory.name, schema: InventorySchema },
        ]),

        // Đăng ký Kafka Producer Client (dùng để GỬI phản hồi Saga ngược lại).
        // Token 'KAFKA_SERVICE' sẽ được @Inject('KAFKA_SERVICE') trong Controller.
        ClientsModule.registerAsync([
            {
                name: 'KAFKA_SERVICE',
                inject: [ConfigService],
                useFactory: (config: ConfigService) => ({
                    transport: Transport.KAFKA,
                    options: {
                        client: {
                            clientId: config.get<string>('KAFKA_CLIENT_ID', 'inventory-service-client'),
                            brokers: (config.get<string>('KAFKA_BROKERS', 'localhost:9092')).split(','),
                        },
                        // Producer config: Kafka sẽ chờ tất cả replica xác nhận trước khi coi là gửi thành công.
                        producer: {
                            allowAutoTopicCreation: true,
                        },
                    },
                }),
            },
        ]),
    ],
    controllers: [InventoryController],
    providers: [InventoryService],
})
export class InventoryModule { }
