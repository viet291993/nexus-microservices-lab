/**
 * GENERATED CODE - DO NOT MODIFY BY HAND
 * This file was generated from shared/event-schemas/asyncapi.yaml
 */

export class OrderEventPayload {
  /**
   * Mã định danh duy nhất của Đơn hàng (UUID)
   */
  orderId!: string;
  /**
   * Mã định danh của Sản phẩm
   */
  productId!: string;
  /**
   * Số lượng sản phẩm cần đặt hoặc trừ kho
   */
  quantity!: number;
  /**
   * Trạng thái của đơn hàng trong hệ thống (Dùng để update database)
   */
  status?: 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'FAILED';
  /**
   * Dấu hiệu nhận biết loại Event để Consumer chuyển hướng xử lý (Routing)
   */
  eventType!: 'ORDER_CREATED' | 'INVENTORY_CONFIRMED' | 'INVENTORY_FAILED';
}

export class InventoryResponsePayload {
  /**
   * Mã định danh duy nhất của Đơn hàng (UUID)
   */
  orderId!: string;
  /**
   * Mã định danh của Sản phẩm
   */
  productId!: string;
  /**
   * Số lượng sản phẩm đã xử lý
   */
  quantity!: number;
  /**
   * Loại event phản hồi (INVENTORY_CONFIRMED hoặc INVENTORY_FAILED)
   */
  eventType!: 'INVENTORY_CONFIRMED' | 'INVENTORY_FAILED';
  /**
   * Thông điệp mô tả kết quả xử lý từ Inventory Service
   */
  message?: string;
}

