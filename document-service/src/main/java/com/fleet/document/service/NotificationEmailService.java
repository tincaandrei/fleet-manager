package com.fleet.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from:noreply@doccufleet.local}")
    private String fromAddress;

    /**
     * Notification emails are best-effort: failures are logged and never
     * propagated, so a broken SMTP setup cannot break exports or schedulers.
     */
    public void send(String to, String subject, String body) {
        if (!enabled) {
            log.debug("Notification email disabled; skipping email to {}", to);
            return;
        }
        if (!StringUtils.hasText(to)) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Notification email enabled but no JavaMailSender is configured; skipping email to {}", to);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Failed to send notification email to {}", to, exception);
        }
    }
}
