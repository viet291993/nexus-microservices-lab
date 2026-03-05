declare module 'eureka-js-client' {
  export interface EurekaInstanceConfig {
    app: string;
    instanceId: string;
    hostName: string;
    ipAddr: string;
    statusPageUrl: string;
    healthCheckUrl: string;
    port: {
      $: number;
      '@enabled': boolean;
    };
    vipAddress: string;
    dataCenterInfo: {
      '@class': string;
      name: string;
    };
  }

  export interface EurekaClientConfig {
    instance: EurekaInstanceConfig;
    eureka: {
      host: string;
      port: number;
      servicePath: string;
    };
  }

  export class Eureka {
    constructor(config: EurekaClientConfig);
    start(callback: (error?: Error) => void): void;
    stop(callback: (error?: Error) => void): void;
  }
}
