package com.fleet.auth.service;

public interface MailService {

    void sendUserInvitationEmail(String to, String setupLink, long expiryHours);
}
