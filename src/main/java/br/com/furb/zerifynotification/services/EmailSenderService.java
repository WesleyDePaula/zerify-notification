package br.com.furb.zerifynotification.services;

import br.com.furb.zerifynotification.domain.SendEmailInput;

public interface EmailSenderService {

    void send(SendEmailInput emailInput);

}
