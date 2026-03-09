import {
  Injectable,
  Logger,
  OnModuleDestroy,
  OnModuleInit,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

/**
 * Custom Eureka Client using native fetch (Node.js 18+)
 * Replaces 'eureka-js-client' to remove deprecated 'request' dependencies.
 */
@Injectable()
export class EurekaService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(EurekaService.name);
  private heartbeatInterval: NodeJS.Timeout | null = null;
  private registrationRetryTimeout: NodeJS.Timeout | null = null;

  private readonly appId = 'INVENTORY-SERVICE';
  private instanceId = '';
  private eurekaUrl = '';
  private registrationData: any = null;

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
    const eurekaPortRaw =
      this.configService.get<string>('EUREKA_PORT') ?? '8761';
    const eurekaPort = parseInt(eurekaPortRaw, 10);

    const finalAppPort =
      isNaN(appPort) || appPort < 1 || appPort > 65535 ? 8083 : appPort;
    const finalEurekaPort =
      isNaN(eurekaPort) || eurekaPort < 1 || eurekaPort > 65535
        ? 8761
        : eurekaPort;

    this.instanceId = `${serviceHost}:inventory-service:${finalAppPort}`;
    this.eurekaUrl = `http://${eurekaHost}:${finalEurekaPort}/eureka/apps`;

    this.registrationData = {
      instance: {
        instanceId: this.instanceId,
        hostName: serviceHost,
        app: this.appId,
        ipAddr: serviceHost,
        status: 'UP',
        overriddenStatus: 'UNKNOWN',
        port: {
          $: finalAppPort,
          '@enabled': 'true',
        },
        securePort: {
          $: 443,
          '@enabled': 'false',
        },
        countryId: 1,
        dataCenterInfo: {
          '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
          name: 'MyOwn',
        },
        leaseInfo: {
          renewalIntervalInSecs: 30,
          durationInSecs: 90,
        },
        metadata: {
          '@class': 'java.util.Collections$EmptyMap',
        },
        vipAddress: this.appId,
        secureVipAddress: this.appId,
        isCoordinatingDiscoveryServer: 'false',
        lastUpdatedTimestamp: Date.now().toString(),
        lastDirtyTimestamp: Date.now().toString(),
        actionType: 'ADDED',
      },
    };

    this.initiateRegistration();
  }

  onModuleDestroy(): void {
    this.stopHeartbeats();
    this.stopRegistrationRetry();
    // Chúng ta không dùng await ở đây vì onModuleDestroy là synchronous, 
    // nhưng deregister là một nỗ lực best-effort khi shutdown.
    this.deregister().catch((err) =>
      this.logger.error(`❌ [EUREKA] Cleanup failed: ${err.message}`),
    );
  }

  private async initiateRegistration(): Promise<void> {
    this.stopRegistrationRetry();
    try {
      await this.register(this.registrationData);
      this.startHeartbeats();
    } catch (error) {
      this.logger.error(
        `❌ [EUREKA] Đăng ký ${this.appId} thất bại: ${error.message}. Thử lại sau 10 giây...`,
      );
      this.registrationRetryTimeout = setTimeout(
        () => this.initiateRegistration(),
        10000,
      );
    }
  }

  private stopRegistrationRetry(): void {
    if (this.registrationRetryTimeout) {
      clearTimeout(this.registrationRetryTimeout);
      this.registrationRetryTimeout = null;
    }
  }

  private async register(data: any): Promise<void> {
    const url = `${this.eurekaUrl}/${this.appId}`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify(data),
      signal: AbortSignal.timeout(5000), // Giới hạn 5s để tránh treo startup
    });

    if (!response.ok && response.status !== 204) {
      const errorText = await response.text();
      throw new Error(`HTTP ${response.status}: ${errorText}`);
    }

    this.logger.log(
      `✅ [EUREKA] ${this.appId} đã đăng ký với Eureka Server (${this.instanceId})`,
    );
  }

  private async deregister(): Promise<void> {
    const url = `${this.eurekaUrl}/${this.appId}/${this.instanceId}`;
    try {
      const response = await fetch(url, {
        method: 'DELETE',
        signal: AbortSignal.timeout(5000),
      });
      if (response.ok || response.status === 204) {
        this.logger.log(`🛑 [EUREKA] ${this.appId} đã deregister khỏi Eureka`);
      } else {
        this.logger.warn(
          `⚠️ [EUREKA] Deregister thất bại (HTTP ${response.status})`,
        );
      }
    } catch (error) {
      this.logger.error(`❌ [EUREKA] Lỗi khi deregister: ${error.message}`);
    }
  }

  private startHeartbeats(): void {
    this.stopHeartbeats(); // Đảm bảo không có interval nào chạy song song
    this.heartbeatInterval = setInterval(async () => {
      try {
        await this.renew();
      } catch (error) {
        this.logger.warn(
          `⚠️ [EUREKA] Heartbeat thất bại: ${error.message}. Đang thử đăng ký lại...`,
        );
        if (error.message.includes('404')) {
          this.initiateRegistration();
        }
      }
    }, 30000);
  }

  private stopHeartbeats(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  private async renew(): Promise<void> {
    const url = `${this.eurekaUrl}/${this.appId}/${this.instanceId}`;
    const response = await fetch(url, {
      method: 'PUT',
      signal: AbortSignal.timeout(5000),
    });

    if (response.status === 404) {
      throw new Error(`HTTP 404: Instance not found`);
    }

    if (!response.ok && response.status !== 204) {
      throw new Error(`HTTP ${response.status}`);
    }
  }
}
