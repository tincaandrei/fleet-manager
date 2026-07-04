package com.fleet.auth.service;

import com.fleet.auth.exception.ApiStatusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    @Override
    public void sendUserInvitationEmail(String to, String setupLink, long expiryHours) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("Set up your DoccuFleet account");
        message.setText("""
                Hello,

                An administrator created a DoccuFleet account for you.

                Set your password using this link:
                %s

                This link expires in %d hours.

                If you did not expect this email, you can ignore it.
                """.formatted(setupLink, expiryHours));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Failed to send invitation email via SMTP host={} port={} from={} to={}",
                    mailHost, mailPort, fromAddress, to, exception);
            throw new ApiStatusException(HttpStatus.BAD_GATEWAY, "MAIL_SEND_FAILED");
        }
    }
}
