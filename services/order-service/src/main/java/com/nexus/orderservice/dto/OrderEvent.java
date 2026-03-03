package com.nexus.orderservice.dto;

/**
 * DTO (Data Transfer Object) đại diện cho một Sự kiện Đơn hàng (Order Event).
 * Đây chính là "bưu kiện" dữ liệu mà Order Service sẽ đóng gói rồi ném vào Kafka Topic.
 * Inventory Service (NestJS) ở bên kia sẽ mở gói này ra để biết cần xử lý đơn hàng nào.
 *
 * Lưu ý: Sử dụng Java Record (từ Java 16+) thay vì Class truyền thống.
 * Record tự động sinh getter, equals, hashCode, toString - cực kỳ gọn gàng cho DTO.
 *
 * @param orderId   Mã định danh duy nhất của đơn hàng (UUID String).
 * @param productId Mã sản phẩm khách hàng đặt mua.
 * @param quantity  Số lượng sản phẩm trong đơn.
 * @param status    Trạng thái hiện tại của đơn: PENDING (Chờ xử lý), CONFIRMED (Đã xác nhận), CANCELLED (Đã hủy).
 * @param eventType Loại sự kiện Saga: ORDER_CREATED (Mới tạo), ORDER_CANCELLED (Bị hủy do Inventory thất bại).
 */
public record OrderEvent(
        String orderId,
        String productId,
        int quantity,
        String status,
        String eventType
) {
}
