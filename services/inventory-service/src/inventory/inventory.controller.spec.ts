import { Logger } from '@nestjs/common';
import { ClientKafka } from '@nestjs/microservices';
import InventoryEventType from '../shared/events/models/InventoryEventType';
import OrderEventPayload from '../shared/events/models/OrderEventPayload';
import OrderEventType from '../shared/events/models/OrderEventType';
import { InventoryController } from './inventory.controller';
import { InventoryService } from './inventory.service';

describe('InventoryController.handleOrderCreated', () => {
  let controller: InventoryController;
  let inventoryService: jest.Mocked<InventoryService>;
  let kafkaClient: jest.Mocked<ClientKafka>;

  beforeEach(() => {
    jest.spyOn(Logger.prototype, 'log').mockImplementation(() => undefined);
    jest.spyOn(Logger.prototype, 'warn').mockImplementation(() => undefined);

    inventoryService = {
      deductStock: jest.fn(),
    } as unknown as jest.Mocked<InventoryService>;

    kafkaClient = {
      emit: jest.fn(),
      connect: jest.fn().mockResolvedValue(undefined),
    } as unknown as jest.Mocked<ClientKafka>;

    controller = new InventoryController(inventoryService, kafkaClient);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('bỏ qua event không phải ORDER_CREATED', async () => {
    const payload = new OrderEventPayload({
      orderId: 'O123',
      productId: 'P001',
      quantity: 5,
      // Bất kỳ loại event nào khác ORDER_CREATED đều phải bị bỏ qua.
      eventType: OrderEventType.INVENTORY_CONFIRMED,
    });

    await controller.handleOrderCreated(payload);

    // eslint-disable-next-line @typescript-eslint/unbound-method
    expect(inventoryService.deductStock).not.toHaveBeenCalled();
    // eslint-disable-next-line @typescript-eslint/unbound-method
    expect(kafkaClient.emit).not.toHaveBeenCalled();
  });

  it('gửi INVENTORY_CONFIRMED khi trừ kho thành công', async () => {
    const payload = new OrderEventPayload({
      orderId: 'O123',
      productId: 'P001',
      quantity: 5,
      eventType: OrderEventType.ORDER_CREATED,
    });

    inventoryService.deductStock.mockResolvedValue({
      success: true,
      message: 'OK',
    });

    await controller.handleOrderCreated(payload);

    // eslint-disable-next-line @typescript-eslint/unbound-method
    expect(inventoryService.deductStock).toHaveBeenCalledWith('P001', 5);
    // eslint-disable-next-line @typescript-eslint/unbound-method
    expect(kafkaClient.emit).toHaveBeenCalledTimes(1);

    const [topic, response] = (kafkaClient.emit as jest.Mock).mock.calls[0] as [
      string,
      unknown,
    ];

    expect(topic).toBe('inventory-events-topic');
    expect(response).toMatchObject({
      orderId: 'O123',
      productId: 'P001',
      quantity: 5,
      eventType: InventoryEventType.INVENTORY_CONFIRMED,
      message: 'OK',
    });
  });

  it('gửi INVENTORY_FAILED khi trừ kho thất bại', async () => {
    const payload = new OrderEventPayload({
      orderId: 'O123',
      productId: 'P001',
      quantity: 10,
      eventType: OrderEventType.ORDER_CREATED,
    });

    inventoryService.deductStock.mockResolvedValue({
      success: false,
      message: 'Hết hàng',
    });

    await controller.handleOrderCreated(payload);

    // eslint-disable-next-line @typescript-eslint/unbound-method
    expect(inventoryService.deductStock).toHaveBeenCalledWith('P001', 10);
    // eslint-disable-next-line @typescript-eslint/unbound-method
    expect(kafkaClient.emit).toHaveBeenCalledTimes(1);

    const [topic, response] = (kafkaClient.emit as jest.Mock).mock.calls[0] as [
      string,
      unknown,
    ];

    expect(topic).toBe('inventory-events-topic');
    expect(response).toMatchObject({
      orderId: 'O123',
      productId: 'P001',
      quantity: 10,
      eventType: InventoryEventType.INVENTORY_FAILED,
      message: 'Hết hàng',
    });
  });
});
