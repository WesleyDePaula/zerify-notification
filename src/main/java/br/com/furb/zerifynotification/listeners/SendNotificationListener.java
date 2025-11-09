package br.com.furb.zerifynotification.listeners;

import br.com.furb.zerifynotification.domain.SendEmailInput;
import br.com.furb.zerifynotification.services.EmailSenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class SendNotificationListener {

    @Autowired
    private EmailSenderService emailSenderService;

    @RabbitListener(queues = "${rabbit.notification_queue}")
    public void sendNotification(SendEmailInput message){
        log.info("Sending email to {}", message.userEmail());
        emailSenderService.send(message);

    }

}
