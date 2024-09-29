package com.commerce.backend.service;

import com.commerce.backend.pojo.SendCommentNotification;
import com.commerce.backend.pojo.WriteArticle;
import com.commerce.backend.pojo.WriteComment;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQSender {
    private final RabbitTemplate rabbitTemplate;

    public RabbitMQSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(WriteArticle articleNotification) {
        rabbitTemplate.convertAndSend("notification", articleNotification.toString());
    }

    public void send(WriteComment message) {
        rabbitTemplate.convertAndSend("notification", message.toString());
    }

    public void send(SendCommentNotification message) {
        rabbitTemplate.convertAndSend("send_notification_exchange", "", message.toString());
    }
}
