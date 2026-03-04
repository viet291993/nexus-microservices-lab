import OrderStatus from './OrderStatus';
import OrderEventType from './OrderEventType';
import { IsOptional, IsDefined, IsString, IsNumber, IsBoolean } from 'class-validator';
export default class OrderEventPayload {
    @IsDefined()
    @IsString()
  private _orderId: string;
    @IsDefined()
    @IsString()
  private _productId: string;
    @IsDefined()
    @IsNumber()
  private _quantity: number;
    @IsOptional()
  private _reservedStatus?: OrderStatus;
    @IsDefined()
  private _eventType: OrderEventType;
    @IsOptional()
  private _additionalProperties?: Map<string, any>;

  constructor(input: {
    orderId: string,
    productId: string,
    quantity: number,
    reservedStatus?: OrderStatus,
    eventType: OrderEventType,
    additionalProperties?: Map<string, any>,
  }) {
    this._orderId = input.orderId;
    this._productId = input.productId;
    this._quantity = input.quantity;
    this._reservedStatus = input.reservedStatus;
    this._eventType = input.eventType;
    this._additionalProperties = input.additionalProperties;
  }

  get orderId(): string { return this._orderId; }
  set orderId(orderId: string) { this._orderId = orderId; }

  get productId(): string { return this._productId; }
  set productId(productId: string) { this._productId = productId; }

  get quantity(): number { return this._quantity; }
  set quantity(quantity: number) { this._quantity = quantity; }

  get reservedStatus(): OrderStatus | undefined { return this._reservedStatus; }
  set reservedStatus(reservedStatus: OrderStatus | undefined) { this._reservedStatus = reservedStatus; }

  get eventType(): OrderEventType { return this._eventType; }
  set eventType(eventType: OrderEventType) { this._eventType = eventType; }

  get additionalProperties(): Map<string, any> | undefined { return this._additionalProperties; }
  set additionalProperties(additionalProperties: Map<string, any> | undefined) { this._additionalProperties = additionalProperties; }
}