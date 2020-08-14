package com.pulawskk.dbsdelivery.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulawskk.dbsdelivery.config.JmsConfig;
import com.pulawskk.dbsdelivery.model.DeliveryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component
public class DeliveryService {

    final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    private final ThreadPoolExecutor executor;

    public DeliveryService(JmsTemplate jmsTemplate, ObjectMapper objectMapper) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }

    @JmsListener(destination = JmsConfig.QUEUE_DELIVERY_STATUS)
    private void listenForDelivery(Message message) throws JsonProcessingException, JMSException {
        DeliveryEvent deliveryEvent = objectMapper.readValue(message.getBody(String.class), DeliveryEvent.class);

        deliveryEvent.setDeliveryId(UUID.randomUUID().toString());
        deliveryEvent.setOrderStatus("DELIVERING");
        deliveryEvent.setEstimatedTimeOfArrival(String.valueOf(ThreadLocalRandom.current().nextInt(10, 75)));

        Object event = objectMapper.writeValueAsString(deliveryEvent);

        jmsTemplate.convertAndSend(message.getJMSReplyTo(), event);

        log.debug("[DELIVERY] Order with id: {} and deliver id: {} is delivering...",
                deliveryEvent.getOrderId(),
                deliveryEvent.getDeliveryId());

        deliverOrder(deliveryEvent);
    }

    private void deliverOrder(DeliveryEvent deliveryEvent) {
        DeliveryThread deliveryThread = new DeliveryThread(deliveryEvent);
        executor.execute(deliveryThread);
    }

    class DeliveryThread implements Runnable {

        private DeliveryEvent deliveryEvent;

        public DeliveryThread(DeliveryEvent deliveryEvent) {
            this.deliveryEvent = deliveryEvent;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextLong(5_000, 20_000));
                deliveryEvent.setOrderStatus("DELIVERED");

                Object event = objectMapper.writeValueAsString(deliveryEvent);

                jmsTemplate.convertAndSend(JmsConfig.QUEUE_DELIVERY_CONFIRMED, event);
                log.debug("[DELIVERY] Order with id: {} and delivery id: {} has been delivered!",
                        deliveryEvent.getOrderId(),
                        deliveryEvent.getDeliveryId());

            } catch (InterruptedException | JsonProcessingException e) {
                throw new RuntimeException("Delivery process has been interrupted: " + e.getMessage(), e);
            }
        }
    }
}

