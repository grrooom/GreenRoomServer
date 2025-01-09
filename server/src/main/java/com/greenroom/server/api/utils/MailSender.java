package com.greenroom.server.api.utils;

import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.exception.CustomException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailSender {

    private final JavaMailSender mailSender;

    public void sendEmail(String to,String text) {
        MimeMessage message = mailSender.createMimeMessage();
        String mailContent
                = "<html>"
                +"<body style = \"align:center\">"
                + "<h2>그린룸 이메일 인증 링크입니다</h2>"
                + "<a href = \""+text+"\">인증 링크</a>"
                + "<p>해당 링크를 통해 앱에 접속하여 이메일 인증을 완료 해 주세요.</p>"
                + "</body>"
                + "</html>";
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("그린룸 이메일 인증을 진행해 주세요");
            helper.setText(mailContent, true);
            mailSender.send(message);
        } catch (MailException e ) {
            throw new CustomException(ResponseCodeEnum.FAIL_TO_SEND_EMAIL,e.getMessage());
        }
        catch (MessagingException e){
            throw new CustomException(ResponseCodeEnum.INVALID_EMAIL_FORMAT,e.getMessage());
        }
    }
}