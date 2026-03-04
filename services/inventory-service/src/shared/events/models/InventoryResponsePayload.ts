import InventoryEventType from './InventoryEventType';
interface InventoryResponsePayload {
  orderId: string;
  productId: string;
  quantity: number;
  eventType: InventoryEventType;
  message?: string;
  additionalProperties?: Map<string, any>;
}
export default InventoryResponsePayload;
