package com.multicloud.costmonitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final com.multicloud.costmonitor.repository.SentEmailRepository sentEmailRepository;

    // Use constructor injection with @Autowired(required = false) in case mail sender is not configured
    public EmailService(@Autowired(required = false) JavaMailSender mailSender, 
                        com.multicloud.costmonitor.repository.SentEmailRepository sentEmailRepository) {
        this.mailSender = mailSender;
        this.sentEmailRepository = sentEmailRepository;
    }

    public void sendBudgetAlertEmail(String toEmail, String budgetName, double spentAmount, double limitAmount, double threshold) {
        String subject = "🚨 BUDGET ALERT: Threshold breached for '" + budgetName + "'";
        String content = String.format(
                "Hello,\n\n" +
                "This is an automated alert from your Multi-Cloud Cost Monitoring Dashboard.\n\n" +
                "The budget '%s' has exceeded its configured threshold:\n" +
                "• Budget Limit: $%.2f\n" +
                "• Threshold: %.1f%%\n" +
                "• Current Month Spend: $%.2f\n\n" +
                "Please review your dashboard to inspect resource usage details and check for cost anomalies.\n\n" +
                "Best regards,\n" +
                "Multi-Cloud Cost Monitor System",
                budgetName, limitAmount, threshold, spentAmount
        );

        // Visual CLI Email output for easy testing/demo without SMTP config
        logger.info("\n" +
                "========================================================================\n" +
                "✉️ [SIMULATED EMAIL SENT]\n" +
                "------------------------------------------------------------------------\n" +
                "To:      {}\n" +
                "Subject: {}\n" +
                "Body:\n{}\n" +
                "========================================================================", 
                toEmail, subject, content);

        // Save to in-app simulated email sandbox database
        try {
            com.multicloud.costmonitor.model.SentEmail sentEmail = new com.multicloud.costmonitor.model.SentEmail(toEmail, subject, content);
            sentEmailRepository.save(sentEmail);
        } catch (Exception e) {
            logger.warn("Could not save simulated email record to database sandbox", e);
        }

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(content);
                mailSender.send(message);
                logger.info("Real email notification sent successfully to {}", toEmail);
            } catch (Exception e) {
                logger.warn("Could not send real email (SMTP not configured or offline). Fallback output printed to logs.");
            }
        }
    }
}
