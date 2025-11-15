package br.com.furb.zerifynotification.services.impl;

import br.com.furb.zerifynotification.domain.SendEmailInput;
import br.com.furb.zerifynotification.services.EmailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderServiceImpl implements EmailSenderService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${email-sender}")
    private String emailSender;

    @Override
    public void send(SendEmailInput emailInput) {
        var message = new SimpleMailMessage();

        message.setFrom(emailSender);
        message.setTo(emailInput.userEmail());
        message.setSubject(emailInput.subject());
        message.setText(emailInput.message());
        mailSender.send(message);
    }

}
