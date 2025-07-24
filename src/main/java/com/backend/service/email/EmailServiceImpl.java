package com.backend.service.email;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class EmailServiceImpl implements EmailService {

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${spring.mail.password}")
    private String password;

    @Override
    public void sendMagicLink(String email, String link) {
        String subject = "streamRepo Magic Link";
        String htmlContent = "<h1>Hi,</h1>" +
                "<p>Welcome to streamRepo! We're thrilled to have you on board. To get started, click the link below to log in to your new account. This magic link is valid for the next 3 minutes:</p>" +
                "<a href=\"" + link + "\" style=\"display: inline-block; padding: 10px 20px; background-color: #007BFF; color: white; text-decoration: none; border-radius: 5px;\">Login Now</a>" +
                "<p>Best,<br>The streamRepo Team</p>" +
                "<p><strong>Need help?</strong><br>If you have any questions, contact us at <a href=\"mailto:okoroaforkelechi123@gmail.com\">support@streamrepo.com</a>.</p>" +
                "<p>Delivered by Kelechi Divine</p>";

        Properties props = new Properties();
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sender, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender, "StreamRepo"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject);

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html");

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("Login to streamRepo. Click this link (expires in 3 minutes): " + link);

            Multipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);

            message.setContent(multipart);

            Transport.send(message);
            System.out.println("Magic link sent to " + email);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send magic link: " + e.getMessage(), e);
        }
    }
}
