import InventoryEventType from './InventoryEventType';
import { IsOptional, IsDefined, IsString, IsNumber, IsBoolean } from 'class-validator';
export default class InventoryResponsePayload {
    @IsDefined()
    @IsString()
  private _orderId: string;
    @IsDefined()
    @IsString()
  private _productId: string;
    @IsDefined()
    @IsNumber()
  private _quantity: number;
    @IsDefined()
  private _eventType: InventoryEventType;
    @IsOptional()
    @IsString()
  private _message?: string;
    @IsOptional()
  private _additionalProperties?: Map<string, any>;

  constructor(input: {
    orderId: string,
    productId: string,
    quantity: number,
    eventType: InventoryEventType,
    message?: string,
    additionalProperties?: Map<string, any>,
  }) {
    this._orderId = input.orderId;
    this._productId = input.productId;
    this._quantity = input.quantity;
    this._eventType = input.eventType;
    this._message = input.message;
    this._additionalProperties = input.additionalProperties;
  }

  get orderId(): string { return this._orderId; }
  set orderId(orderId: string) { this._orderId = orderId; }

  get productId(): string { return this._productId; }
  set productId(productId: string) { this._productId = productId; }

  get quantity(): number { return this._quantity; }
  set quantity(quantity: number) { this._quantity = quantity; }

  get eventType(): InventoryEventType { return this._eventType; }
  set eventType(eventType: InventoryEventType) { this._eventType = eventType; }

  get message(): string | undefined { return this._message; }
  set message(message: string | undefined) { this._message = message; }

  get additionalProperties(): Map<string, any> | undefined { return this._additionalProperties; }
  set additionalProperties(additionalProperties: Map<string, any> | undefined) { this._additionalProperties = additionalProperties; }
}