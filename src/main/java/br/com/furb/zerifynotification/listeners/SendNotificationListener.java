package br.com.furb.zerifynotification.listeners;

import br.com.furb.zerifynotification.domain.SendEmailInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class SendNotificationListener {

    @RabbitListener(queues = "${rabbit.notification_queue}")
    public void sendNotification(SendEmailInput message){
        log.info("Sending email... {}", message);
    }

}
