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
        String subject = "Verify Your StreamRepo Account";
        String htmlContent = "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Verify Your StreamRepo Account</title>" +
                "<style>" +
                "body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; background-color: #f4f4f4; color: #333; }" +
                ".container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
                ".header { background-color: #1a73e8; padding: 20px; text-align: center; border-top-left-radius: 8px; border-top-right-radius: 8px; }" +
                ".header img { max-width: 150px; }" +
                ".content { padding: 30px; }" +
                ".content h1 { font-size: 24px; margin: 0 0 20px; color: #1a73e8; }" +
                ".content p { font-size: 16px; line-height: 1.6; margin: 0 0 20px; }" +
                ".button { display: inline-block; padding: 12px 24px; background-color: #1a73e8; color: #ffffff !important;; text-decoration: none; border-radius: 4px; font-size: 16px; font-weight: 600; text-align: center; }" +
                ".button:hover { background-color: #1557b0; }" +
                ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; border-bottom-left-radius: 8px; border-bottom-right-radius: 8px; font-size: 14px; color: #666; }" +
                ".footer a { color: #1a73e8; text-decoration: none; }" +
                ".footer a:hover { text-decoration: underline; }" +
                "@media only screen and (max-width: 600px) { .content { padding: 20px; } .button { padding: 10px 20px; font-size: 14px; } }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<img src='https://via.placeholder.com/150x50?text=StreamRepo' alt='StreamRepo Logo'>" +
                "</div>" +
                "<div class='content'>" +
                "<h1>Welcome to StreamRepo!</h1>" +
                "<p>Hello,</p>" +
                "<p>You're one step away from accessing your StreamRepo account. Click the button below to verify your email address and log in. This link is valid for the next 1 hour.</p>" +
                "<a href='" + link + "' class='button'>Verify Your Email</a>" +
                "<p>If the button doesn't work, you can copy and paste this link into your browser:</p>" +
                "<p><a href='" + link + "'>" + link + "</a></p>" +
                "<p>Best regards,<br>The StreamRepo Team</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p><strong>Need help?</strong> Contact us at <a href='mailto:okoroaforkelechi123@gmail.com'>support@streamrepo.com</a>.</p>" +
                "<p>Delivered by StreamRepo &copy; 2025</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

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
            textPart.setText("Verify your StreamRepo account. Click this link (expires in 1 hour): " + link);

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

    @Override
    public void sendPasswordResetLink(String email, String link) {
        String subject = "Reset Your StreamRepo Password";
        String htmlContent = "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Reset Your StreamRepo Password</title>" +
                "<style>" +
                "body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; background-color: #f4f4f4; color: #333; }" +
                ".container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
                ".header { background-color: #1a73e8; padding: 20px; text-align: center; border-top-left-radius: 8px; border-top-right-radius: 8px; }" +
                ".header img { max-width: 150px; }" +
                ".content { padding: 30px; }" +
                ".content h1 { font-size: 24px; margin: 0 0 20px; color: #1a73e8; }" +
                ".content p { font-size: 16px; line-height: 1.6; margin: 0 0 20px; }" +
                ".button { display: inline-block; padding: 12px 24px; background-color: #1a73e8; color: #ffffff !important;; text-decoration: none; border-radius: 4px; font-size: 16px; font-weight: 600; text-align: center; }" +
                ".button:hover { background-color: #1557b0; }" +
                ".footer { background-color: #f8f9fa; padding: 20px; text-align: center; border-bottom-left-radius: 8px; border-bottom-right-radius: 8px; font-size: 14px; color: #666; }" +
                ".footer a { color: #1a73e8; text-decoration: none; }" +
                ".footer a:hover { text-decoration: underline; }" +
                "@media only screen and (max-width: 600px) { .content { padding: 20px; } .button { padding: 10px 20px; font-size: 14px; } }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<img src='https://via.placeholder.com/150x50?text=StreamRepo' alt='StreamRepo Logo'>" +
                "</div>" +
                "<div class='content'>" +
                "<h1>Reset Your Password</h1>" +
                "<p>Hello,</p>" +
                "<p>We received a request to reset your StreamRepo account password. Click the button below to set a new password. This link is valid for the next 1 hour.</p>" +
                "<a href='" + link + "' class='button'>Reset Password</a>" +
                "<p>If you didn't request a password reset, please ignore this email or contact our support team.</p>" +
                "<p><a href='" + link + "'>" + link + "</a></p>" +
                "<p>Best regards,<br>The StreamRepo Team</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p><strong>Need help?</strong> Contact us at <a href='mailto:okoroaforkelechi123@gmail.com'>support@streamrepo.com</a>.</p>" +
                "<p>Delivered by StreamRepo &copy; 2025</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

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
            textPart.setText("Reset your StreamRepo password. Click this link (expires in 1 hour): " + link);

            Multipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);

            message.setContent(multipart);

            Transport.send(message);
            System.out.println("Password reset link sent to " + email);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send password reset link: " + e.getMessage(), e);
        }
    }
}