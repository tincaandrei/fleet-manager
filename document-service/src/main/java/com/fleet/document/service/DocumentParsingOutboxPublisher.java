package com.fleet.document.service;

import com.fleet.document.config.RabbitMqProperties;
import com.fleet.document.entity.DocumentParsingOutboxEvent;
import com.fleet.document.repository.DocumentParsingOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DocumentParsingOutboxPublisher {

    private final DocumentParsingOutboxRepository outboxRepository;
    private final DocumentParsingOutboxService outboxService;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties properties;

    @Scheduled(fixedDelayString = "${app.rabbitmq.outbox-publish-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<DocumentParsingOutboxEvent> pendingEvents =
                outboxRepository.findPendingForUpdate(PageRequest.of(0, 50));
        for (DocumentParsingOutboxEvent event : pendingEvents) {
            if (!publish(event)) {
                break;
            }
        }
    }

    private boolean publish(DocumentParsingOutboxEvent event) {
        event.setPublishAttempts(event.getPublishAttempts() + 1);
        try {
            CorrelationData correlationData = new CorrelationData(event.getId().toString());
            rabbitTemplate.convertAndSend(
                    properties.getExchange(),
                    properties.getParsingRoutingKey(),
                    outboxService.toMessage(event),
                    correlationData
            );
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(properties.getPublisherConfirmTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ rejected message: " + confirm.getReason());
            }
            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "RabbitMQ returned unroutable message: " + correlationData.getReturned().getReplyText()
                );
            }
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
            outboxRepository.save(event);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            recordFailure(event, exception);
            return false;
        } catch (Exception exception) {
            recordFailure(event, exception);
            return false;
        }
    }

    private void recordFailure(DocumentParsingOutboxEvent event, Exception exception) {
        event.setLastError(truncate(exception.getMessage()));
        outboxRepository.save(event);
        log.warn("Could not publish document parsing outbox event {}", event.getId(), exception);
    }

    private String truncate(String message) {
        if (message == null) {
            return "Unknown RabbitMQ publishing error";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
