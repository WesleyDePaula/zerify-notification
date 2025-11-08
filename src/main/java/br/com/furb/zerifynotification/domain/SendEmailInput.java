package br.com.furb.zerifynotification.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailInput {
    String subject;
    String message;
    String userEmail;
}
