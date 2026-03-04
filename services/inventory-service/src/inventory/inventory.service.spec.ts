import { Logger } from '@nestjs/common';
import { Model } from 'mongoose';
import { InventoryService } from './inventory.service';
import { InventoryDocument } from './schemas/inventory.schema';

describe('InventoryService.deductStock', () => {
  let service: InventoryService;
  let inventoryModel: jest.Mocked<Model<InventoryDocument>>;

  beforeEach(() => {
    // Giảm noise log trong khi chạy test.
    jest.spyOn(Logger.prototype, 'log').mockImplementation(() => undefined);
    jest.spyOn(Logger.prototype, 'warn').mockImplementation(() => undefined);

    inventoryModel = {
      findOne: jest.fn(),
      findOneAndUpdate: jest.fn(),
    } as unknown as jest.Mocked<Model<InventoryDocument>>;

    service = new InventoryService(inventoryModel);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('trả về thất bại khi không tìm thấy sản phẩm', async () => {
    const f1Mock = jest.fn().mockResolvedValue(null);
    (inventoryModel.findOneAndUpdate as unknown as jest.Mock) = jest
      .fn()
      .mockReturnValue({ exec: f1Mock } as never);

    const f2Mock = jest.fn().mockResolvedValue(null);
    (inventoryModel.findOne as jest.Mock).mockReturnValue({
      exec: f2Mock,
    } as never);

    const result = await service.deductStock('P-UNKNOWN', 5);

    // eslint-disable-next-line @typescript-eslint/unbound-method
    expect(inventoryModel.findOneAndUpdate).toHaveBeenCalledTimes(1);
    // eslint-disable-next-line @typescript-eslint/unbound-method
    expect(inventoryModel.findOne).toHaveBeenCalledWith({
      productId: 'P-UNKNOWN',
    });
    expect(result.success).toBe(false);
    expect(result.message).toContain('không tồn tại');
  });

  it('trả về thất bại khi tồn kho nhỏ hơn số lượng yêu cầu', async () => {
    // findOneAndUpdate trả về null => không đủ điều kiện quantity >= requested.
    const f1Mock = jest.fn().mockResolvedValue(null);
    (inventoryModel.findOneAndUpdate as unknown as jest.Mock) = jest
      .fn()
      .mockReturnValue({ exec: f1Mock } as never);

    const existingProduct: Partial<InventoryDocument> = {
      productId: 'P001',
      quantity: 3,
    };
    const f2Mock = jest.fn().mockResolvedValue(existingProduct);
    (inventoryModel.findOne as jest.Mock).mockReturnValue({
      exec: f2Mock,
    } as never);

    const result = await service.deductStock('P001', 10);

    expect(result.success).toBe(false);
    expect(result.message).toContain('Hết hàng');
    // Không gọi save trong trường hợp thất bại (vì dùng findOneAndUpdate).
    // eslint-disable-next-line @typescript-eslint/unbound-method
    expect(inventoryModel.findOneAndUpdate).toHaveBeenCalledTimes(1);
  });

  it('trả về thất bại khi quantity bằng 0', async () => {
    const result = await service.deductStock('P001', 0);

    // Không gọi vào database khi dữ liệu đầu vào không hợp lệ.
    // eslint-disable-next-line @typescript-eslint/unbound-method
    expect(inventoryModel.findOne).not.toHaveBeenCalled();
    expect(result.success).toBe(false);
    expect(result.message).toContain('không hợp lệ');
  });

  it('trừ kho và lưu lại MongoDB khi đủ hàng', async () => {
    const updatedProduct: Partial<InventoryDocument> = {
      productId: 'P001',
      quantity: 6,
    };
    const f1Mock = jest.fn().mockResolvedValue(updatedProduct);
    (inventoryModel.findOneAndUpdate as unknown as jest.Mock) = jest
      .fn()
      .mockReturnValue({ exec: f1Mock } as never);

    const result = await service.deductStock('P001', 4);

    expect(result.success).toBe(true);
    expect(result.message).toContain('Đã trừ 4 sản phẩm');
    // eslint-disable-next-line @typescript-eslint/unbound-method
    expect(inventoryModel.findOneAndUpdate).toHaveBeenCalledTimes(1);
    // eslint-disable-next-line @typescript-eslint/unbound-method
    expect(inventoryModel.findOne).not.toHaveBeenCalled();
  });
});
