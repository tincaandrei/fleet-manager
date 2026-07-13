package com.fleet.document.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduling was previously enabled only through RabbitMqConfig, which is
 * conditional on app.rabbitmq.enabled; this keeps scheduled jobs (expiry
 * alerts) running even when RabbitMQ is disabled.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
