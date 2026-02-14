package com.ecommerce.project.service;

import com.ecommerce.project.model.Order;
import com.ecommerce.project.model.OrderStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@ecomverse.com}")
    private String fromEmail;

    @Value("${app.name:EComVerse}")
    private String appName;

    @Async
    public void sendOrderConfirmationEmail(String to, Order order) {
        try {
            Context context = new Context();
            context.setVariable("orderId", order.getOrderId());
            context.setVariable("orderDate", order.getOrderDate());
            context.setVariable("totalAmount", order.getTotalAmount());
            context.setVariable("appName", appName);

            String subject = "Order Confirmation - #" + order.getOrderId();
            String htmlContent = generateEmailContent("order-confirmation", context);

            sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email: " + e.getMessage());
        }
    }

    @Async
    public void sendShippingNotificationEmail(String to, Order order, String trackingNumber, String carrier) {
        try {
            Context context = new Context();
            context.setVariable("orderId", order.getOrderId());
            context.setVariable("trackingNumber", trackingNumber);
            context.setVariable("carrier", carrier);
            context.setVariable("appName", appName);

            String subject = "Your Order Has Shipped - #" + order.getOrderId();
            String htmlContent = generateEmailContent("shipping-notification", context);

            sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            System.err.println("Failed to send shipping notification email: " + e.getMessage());
        }
    }

    @Async
    public void sendOrderStatusUpdateEmail(String to, Order order, OrderStatus newStatus) {
        try {
            Context context = new Context();
            context.setVariable("orderId", order.getOrderId());
            context.setVariable("status", newStatus.getDisplayName());
            context.setVariable("statusDescription", newStatus.getDescription());
            context.setVariable("appName", appName);

            String subject = "Order Update - #" + order.getOrderId();
            String htmlContent = generateEmailContent("order-status-update", context);

            sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            System.err.println("Failed to send order status update email: " + e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String resetUrl) {
        try {
            Context context = new Context();
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("appName", appName);

            String subject = "Password Reset Request";
            String htmlContent = generateEmailContent("password-reset", context);

            sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String username) {
        try {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("appName", appName);

            String subject = "Welcome to " + appName + "!";
            String htmlContent = generateEmailContent("welcome", context);

            sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
    }

    private String generateEmailContent(String templateName, Context context) {
        if (templateEngine != null) {
            try {
                return templateEngine.process("email/" + templateName, context);
            } catch (Exception e) {
                return generateFallbackContent(templateName, context);
            }
        }
        return generateFallbackContent(templateName, context);
    }

    private String generateFallbackContent(String templateName, Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>").append(appName).append("</h1>");

        switch (templateName) {
            case "order-confirmation":
                sb.append("<p>Thank you for your order #").append(context.getVariable("orderId")).append("</p>");
                sb.append("<p>Total: $").append(context.getVariable("totalAmount")).append("</p>");
                break;
            case "shipping-notification":
                sb.append("<p>Your order #").append(context.getVariable("orderId")).append(" has shipped!</p>");
                sb.append("<p>Tracking: ").append(context.getVariable("trackingNumber")).append("</p>");
                break;
            case "password-reset":
                sb.append("<p>Click the link below to reset your password:</p>");
                sb.append("<a href='").append(context.getVariable("resetUrl")).append("'>Reset Password</a>");
                break;
            default:
                sb.append("<p>Email notification from ").append(appName).append("</p>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        if (mailSender == null) {
            System.out.println("Email would be sent to: " + to);
            System.out.println("Subject: " + subject);
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
