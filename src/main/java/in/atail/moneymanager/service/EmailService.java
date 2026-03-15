package in.atail.moneymanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String fromEmail;
    private final JavaMailSender emailSender;

    /**
     * 发送电子邮件
     * 使用 SimpleMailMessage 构建并发送邮件，支持指定收件人、主题和邮件内容
     *
     * @param to 收件人的电子邮件地址
     * @param subject 邮件主题
     * @param text 邮件正文内容
     */
    public void sendEmail(String to, String subject, String text) {
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
