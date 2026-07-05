package com.fleet.document.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(RabbitMqProperties.class)
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqConfig {

    @Bean
    public DirectExchange documentExchange(RabbitMqProperties properties) {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    public DirectExchange documentDeadLetterExchange(RabbitMqProperties properties) {
        return new DirectExchange(properties.getDeadLetterExchange(), true, false);
    }

    @Bean
    public Queue documentParsingQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.getParsingQueue())
                .deadLetterExchange(properties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue documentParsingDeadLetterQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.getDeadLetterQueue()).build();
    }

    @Bean
    public Binding documentParsingBinding(
            Queue documentParsingQueue,
            DirectExchange documentExchange,
            RabbitMqProperties properties
    ) {
        return BindingBuilder.bind(documentParsingQueue)
                .to(documentExchange)
                .with(properties.getParsingRoutingKey());
    }

    @Bean
    public Binding documentParsingDeadLetterBinding(
            Queue documentParsingDeadLetterQueue,
            DirectExchange documentDeadLetterExchange,
            RabbitMqProperties properties
    ) {
        return BindingBuilder.bind(documentParsingDeadLetterQueue)
                .to(documentDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey());
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitMessageConverter);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public Advice documentParsingRetryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(new org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            Advice documentParsingRetryInterceptor
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);
        factory.setAdviceChain(documentParsingRetryInterceptor);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
