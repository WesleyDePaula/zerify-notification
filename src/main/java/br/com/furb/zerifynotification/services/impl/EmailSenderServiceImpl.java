package br.com.furb.zerifynotification.services.impl;

import br.com.furb.zerifynotification.domain.SendEmailInput;
import br.com.furb.zerifynotification.services.EmailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderServiceImpl implements EmailSenderService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void send(SendEmailInput emailInput) {
        var message = new SimpleMailMessage();

        // TODO: Alterar para pegar value do .properties
        message.setFrom("zerifyapp@gmail.com");
        message.setTo(emailInput.userEmail());
        message.setSubject(emailInput.subject());
        message.setText(emailInput.message());
        mailSender.send(message);
    }

}
