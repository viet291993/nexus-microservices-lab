import { Logger } from '@nestjs/common';
import { MongoMemoryServer } from 'mongodb-memory-server';
import mongoose from 'mongoose';
import { InventoryService } from './inventory.service';
import {
  Inventory,
  InventoryDocument,
  InventorySchema,
} from './schemas/inventory.schema';

describe('InventoryService.deductStock concurrency', () => {
  let mongod: MongoMemoryServer;
  // Đơn giản hóa type trong test để tránh xung đột generic của Mongoose.
  let inventoryModel: mongoose.Model<Inventory>;
  let service: InventoryService;

  beforeAll(async () => {
    jest.spyOn(Logger.prototype, 'log').mockImplementation(() => undefined);
    jest.spyOn(Logger.prototype, 'warn').mockImplementation(() => undefined);

    mongod = await MongoMemoryServer.create();
    const uri = mongod.getUri();
    await mongoose.connect(uri);

    inventoryModel = mongoose.model<Inventory>(Inventory.name, InventorySchema);
    service = new InventoryService(
      inventoryModel as unknown as mongoose.Model<InventoryDocument>,
    );
  });

  afterAll(async () => {
    await mongoose.connection.dropDatabase();
    await mongoose.connection.close();
    await mongod.stop();
    jest.restoreAllMocks();
  });

  beforeEach(async () => {
    await inventoryModel.deleteMany({});
  });

  it('chỉ một request trừ kho thành công khi chạy song song', async () => {
    await inventoryModel.create({
      productId: 'P-RACE',
      name: 'Race Product',
      quantity: 5,
    });

    const p1 = service.deductStock('P-RACE', 4);
    const p2 = service.deductStock('P-RACE', 4);

    const [r1, r2] = await Promise.all([p1, p2]);

    const updated = await inventoryModel
      .findOne({ productId: 'P-RACE' })
      .exec();

    const successCount = [r1.success, r2.success].filter((s) => s).length;

    expect(successCount).toBe(1);
    expect(updated?.quantity).toBeGreaterThanOrEqual(0);
  });
});
