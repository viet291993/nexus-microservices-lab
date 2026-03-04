import {
  Injectable,
  Logger,
  OnModuleDestroy,
  OnModuleInit,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Eureka } from 'eureka-js-client';

@Injectable()
export class EurekaService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(EurekaService.name);
  private client: Eureka | null = null;

  constructor(private readonly configService: ConfigService) {}

  onModuleInit(): void {
    const appPort = Number(this.configService.get<string>('PORT') ?? 8083);
    const serviceHost =
      this.configService.get<string>('SERVICE_HOST') ??
      this.configService.get<string>('HOSTNAME') ??
      'localhost';
    const eurekaHost =
      this.configService.get<string>('EUREKA_HOST') ?? 'localhost';
    const eurekaPort = Number(
      this.configService.get<string>('EUREKA_PORT') ?? 8761,
    );

    this.client = new Eureka({
      instance: {
        app: 'INVENTORY-SERVICE',
        // Giữ pattern giống Spring Boot: <host-or-ip>:<app-name>:<port>
        // Ví dụ: 10.0.0.5:inventory-service:8083
        instanceId: `${serviceHost}:inventory-service:${appPort}`,
        hostName: serviceHost,
        ipAddr: serviceHost,
        statusPageUrl: `http://${serviceHost}:${appPort}/health`,
        healthCheckUrl: `http://${serviceHost}:${appPort}/health`,
        port: {
          $: appPort,
          '@enabled': true,
        },
        vipAddress: 'INVENTORY-SERVICE',
        dataCenterInfo: {
          '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
          name: 'MyOwn',
        },
      },
      eureka: {
        host: eurekaHost,
        port: eurekaPort,
        servicePath: '/eureka/apps/',
      },
    });

    this.client.start((error?: Error) => {
      if (error) {
        this.logger.error(
          '❌ [EUREKA] Đăng ký INVENTORY-SERVICE thất bại',
          error,
        );
        return;
      }
      this.logger.log(
        '✅ [EUREKA] INVENTORY-SERVICE đã đăng ký với Eureka Server',
      );
    });
  }

  onModuleDestroy(): void {
    if (!this.client) {
      return;
    }

    this.client.stop((error?: Error) => {
      if (error) {
        this.logger.error(
          '❌ [EUREKA] Lỗi khi deregister INVENTORY-SERVICE khỏi Eureka',
          error,
        );
      } else {
        this.logger.log(
          '🛑 [EUREKA] INVENTORY-SERVICE đã deregister khỏi Eureka',
        );
      }
    });
  }
}
