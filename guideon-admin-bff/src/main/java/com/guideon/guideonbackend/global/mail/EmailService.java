package com.guideon.guideonbackend.global.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.invite.from-email}")
    private String fromEmail;

    @Value("${app.invite.from-name}")
    private String fromName;

    @Value("${app.invite.accept-url}")
    private String acceptUrl;

    @Async
    public void sendInviteEmail(String toEmail, String siteName, String token, String expiresAt) {
        Context context = new Context();
        context.setVariable("siteName", siteName);
        context.setVariable("acceptUrl", acceptUrl + "?token=" + token);
        context.setVariable("expiresAt", expiresAt);

        String html = templateEngine.process("invite-email", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("[GUIDEON] " + siteName + " 운영자 초대");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("초대 메일 발송 완료: to={}, site={}", toEmail, siteName);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("초대 메일 발송 실패: to={}, error={}", toEmail, e.getMessage(), e);
        }
    }
}