import OrderStatus from './OrderStatus';
import OrderEventType from './OrderEventType';
interface OrderEventPayload {
  orderId: string;
  productId: string;
  quantity: number;
  reservedStatus?: OrderStatus;
  eventType: OrderEventType;
  additionalProperties?: Map<string, any>;
}
export default OrderEventPayload;
