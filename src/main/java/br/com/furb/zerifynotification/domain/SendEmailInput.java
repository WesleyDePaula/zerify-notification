package br.com.furb.zerifynotification.domain;

public record SendEmailInput(String subject, String message, String userEmail) {
}
