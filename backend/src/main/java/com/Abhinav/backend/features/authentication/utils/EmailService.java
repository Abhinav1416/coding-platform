package com.Abhinav.backend.features.authentication.utils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String email, String subject, String content) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom("no-reply@linkedin.com", "LinkedIn");
        helper.setTo(email);

        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }


    public void sendTwoFactorCode(String email, String code) {
        String subject = "Your 2FA Code";
        String content = "<p>Use the following code to complete your login:</p>"
                + "<h3>" + code + "</h3>"
                + "<p>This code will expire in 5 minutes.</p>";
        try {
            sendEmail(email, subject, content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send 2FA email", e);
        }
    }
}