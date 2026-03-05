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
    const appPortRaw = this.configService.get<string>('PORT') ?? '8083';
    const appPort = parseInt(appPortRaw, 10);
    const serviceHost =
      this.configService.get<string>('SERVICE_HOST') ??
      this.configService.get<string>('HOSTNAME') ??
      'localhost';
    const eurekaHost =
      this.configService.get<string>('EUREKA_HOST') ?? 'localhost';
    const eurekaPortRaw = this.configService.get<string>('EUREKA_PORT') ?? '8761';
    const eurekaPort = parseInt(eurekaPortRaw, 10);

    if (isNaN(appPort) || appPort < 1 || appPort > 65535) {
      this.logger.warn(`⚠️ [EUREKA] Invalid PORT: ${appPortRaw}. Falling back to 8083.`);
    }
    if (isNaN(eurekaPort) || eurekaPort < 1 || eurekaPort > 65535) {
      this.logger.warn(`⚠️ [EUREKA] Invalid EUREKA_PORT: ${eurekaPortRaw}. Falling back to 8761.`);
    }

    const finalAppPort = isNaN(appPort) ? 8083 : appPort;
    const finalEurekaPort = isNaN(eurekaPort) ? 8761 : eurekaPort;

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
          $: finalAppPort,
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
        port: finalEurekaPort,
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
