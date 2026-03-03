/**
 * Schema MongoDB định nghĩa cấu trúc dữ liệu của một sản phẩm trong kho.
 *
 * Trong MongoDB, mỗi "document" (bản ghi) sẽ có dạng JSON:
 * {
 *   "_id": "ObjectId(...)",
 *   "productId": "PRODUCT-001",
 *   "name": "Áo thun Nexus",
 *   "quantity": 100
 * }
 */

import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

// Kế thừa Document của Mongoose để có đầy đủ các method save(), remove()...
export type InventoryDocument = Inventory & Document;

@Schema({ timestamps: true }) // timestamps: true => Tự động thêm createdAt, updatedAt.
export class Inventory {
    /**
     * Mã sản phẩm duy nhất.
     * unique: true => Không cho phép 2 document có cùng productId trong MongoDB.
     * required: true => Bắt buộc phải có khi tạo mới.
     */
    @Prop({ required: true, unique: true })
    productId: string;

    /** Tên hiển thị của sản phẩm. */
    @Prop({ required: true })
    name: string;

    /**
     * Số lượng tồn kho hiện tại.
     * Khi Saga event ORDER_CREATED bay tới, Consumer sẽ trừ số lượng này.
     * Nếu quantity < orderQuantity => Trả về INVENTORY_FAILED.
     */
    @Prop({ required: true, default: 0 })
    quantity: number;
}

// Tạo Schema từ class Inventory để Mongoose sử dụng.
export const InventorySchema = SchemaFactory.createForClass(Inventory);
