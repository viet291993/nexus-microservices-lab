/**
 * Controller xử lý Kafka Event cho Inventory Service.
 *
 * Đây là "trái tim" của Saga Participant (Bên tham gia giao dịch phân tán).
 * Controller này KHÔNG phục vụ HTTP request, mà lắng nghe Message từ Kafka Topic.
 *
 * Luồng xử lý:
 *   1. Order Service (Java) gửi event "ORDER_CREATED" vào topic "saga-orders-topic".
 *   2. Controller này nhận event đó qua decorator @EventPattern.
 *   3. Gọi InventoryService.deductStock() để trừ kho trong MongoDB.
 *   4. Gửi phản hồi (INVENTORY_CONFIRMED hoặc INVENTORY_FAILED) ngược lại
 *      vào topic "saga-inventory-response" để Order Service nhận và xử lý tiếp.
 */

import { Controller, Inject, Logger } from '@nestjs/common';
import { EventPattern, Payload, ClientKafka } from '@nestjs/microservices';
import { InventoryService } from './inventory.service';

// Interface mô tả cấu trúc OrderEvent nhận từ Java (phải khớp với Java Record OrderEvent).
interface OrderEvent {
    orderId: string;
    productId: string;
    quantity: number;
    status: string;
    eventType: string;
}

@Controller()
export class InventoryController {
    private readonly logger = new Logger(InventoryController.name);

    constructor(
        private readonly inventoryService: InventoryService,
        // Inject Kafka Client để gửi message phản hồi ngược lại cho Order Service.
        @Inject('KAFKA_SERVICE') private readonly kafkaClient: ClientKafka,
    ) { }

    /**
     * Được NestJS tự động gọi khi ứng dụng khởi động.
     * Đăng ký subscribe topic phản hồi để Kafka Client sẵn sàng gửi message.
     */
    async onModuleInit(): Promise<void> {
        // Kết nối Kafka Producer (để gửi phản hồi).
        await this.kafkaClient.connect();
        this.logger.log('📡 [KAFKA] Kafka Producer đã kết nối, sẵn sàng gửi phản hồi.');
    }

    /**
     * Handler lắng nghe event từ topic "saga-orders-topic".
     *
     * @EventPattern: Decorator đặc biệt của NestJS Microservices.
     * Khác với @MessagePattern (request-reply), @EventPattern chỉ nhận event một chiều (fire-and-forget).
     * Phù hợp với kiến trúc Saga Choreography: mỗi bên tự xử lý rồi tự phát sóng kết quả.
     *
     * @param orderEvent Dữ liệu OrderEvent từ Java Order Service (đã được deserialize từ JSON).
     */
    @EventPattern('saga-orders-topic')
    async handleOrderCreated(@Payload() orderEvent: OrderEvent): Promise<void> {
        this.logger.log(
            `📩 [CONSUMER] Nhận event từ Order Service: orderId=${orderEvent.orderId}, ` +
            `productId=${orderEvent.productId}, quantity=${orderEvent.quantity}, ` +
            `eventType=${orderEvent.eventType}`,
        );

        // Chỉ xử lý event loại ORDER_CREATED (bỏ qua các loại khác).
        if (orderEvent.eventType !== 'ORDER_CREATED') {
            this.logger.warn(`⚠️ [CONSUMER] Bỏ qua event không phải ORDER_CREATED: ${orderEvent.eventType}`);
            return;
        }

        // Gọi Service trừ kho trong MongoDB.
        const result = await this.inventoryService.deductStock(
            orderEvent.productId,
            orderEvent.quantity,
        );

        // Đóng gói phản hồi gửi ngược lại Order Service qua topic "saga-inventory-response".
        const responseEvent = {
            orderId: orderEvent.orderId,
            productId: orderEvent.productId,
            quantity: orderEvent.quantity,
            eventType: result.success ? 'INVENTORY_CONFIRMED' : 'INVENTORY_FAILED',
            message: result.message,
        };

        // Bắn phản hồi vào Kafka topic "saga-inventory-response".
        this.kafkaClient.emit('saga-inventory-response', responseEvent);

        this.logger.log(
            `📤 [PRODUCER] Đã gửi phản hồi "${responseEvent.eventType}" cho orderId=${orderEvent.orderId}`,
        );
    }
}
