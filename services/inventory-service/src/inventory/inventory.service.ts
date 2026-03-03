/**
 * Service xử lý nghiệp vụ kho hàng.
 *
 * Chứa logic cốt lõi:
 *   - Tìm sản phẩm theo productId trong MongoDB.
 *   - Kiểm tra số lượng tồn kho.
 *   - Trừ kho (Deduct) nếu đủ hàng.
 */

import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Inventory, InventoryDocument } from './schemas/inventory.schema';

@Injectable()
export class InventoryService {
    private readonly logger = new Logger(InventoryService.name);

    constructor(
        // Inject Model Mongoose để thao tác CRUD với collection "inventories" trên MongoDB.
        @InjectModel(Inventory.name) private inventoryModel: Model<InventoryDocument>,
    ) { }

    /**
     * Xử lý trừ kho cho một đơn hàng.
     *
     * Logic Saga:
     *   1. Tìm sản phẩm theo productId.
     *   2. Nếu không tìm thấy => Trả về { success: false } (INVENTORY_FAILED).
     *   3. Nếu tồn kho < số lượng yêu cầu => Trả về { success: false }.
     *   4. Nếu đủ hàng => Trừ kho, lưu lại MongoDB, trả về { success: true }.
     *
     * @param productId Mã sản phẩm cần trừ.
     * @param quantity  Số lượng cần trừ.
     * @returns Object chứa kết quả { success: boolean, message: string }.
     */
    async deductStock(
        productId: string,
        quantity: number,
    ): Promise<{ success: boolean; message: string }> {
        this.logger.log(
            `📦 [INVENTORY] Đang kiểm tra kho cho productId=${productId}, quantity=${quantity}`,
        );

        // Bước 1: Tìm sản phẩm trong MongoDB.
        const product = await this.inventoryModel.findOne({ productId }).exec();

        if (!product) {
            this.logger.warn(
                `❌ [INVENTORY] Không tìm thấy sản phẩm với productId=${productId}`,
            );
            return { success: false, message: `Sản phẩm ${productId} không tồn tại trong kho.` };
        }

        // Bước 2: Kiểm tra số lượng tồn kho.
        if (product.quantity < quantity) {
            this.logger.warn(
                `❌ [INVENTORY] Không đủ hàng! Tồn kho=${product.quantity}, Yêu cầu=${quantity}`,
            );
            return {
                success: false,
                message: `Hết hàng! Tồn kho hiện tại: ${product.quantity}, yêu cầu: ${quantity}.`,
            };
        }

        // Bước 3: Trừ kho và lưu lại MongoDB.
        product.quantity -= quantity;
        await product.save();

        this.logger.log(
            `✅ [INVENTORY] Trừ kho thành công! productId=${productId}, còn lại=${product.quantity}`,
        );
        return { success: true, message: `Đã trừ ${quantity} sản phẩm. Còn lại: ${product.quantity}.` };
    }
}
