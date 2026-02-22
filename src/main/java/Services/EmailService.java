package Services;

import Models.Interview;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Email Service for sending interview reminders
 * Configuration loaded from email.properties file
 */
public class EmailService {

    private static final Properties emailConfig = loadEmailConfiguration();

    // Configuration loaded from properties file
    private static final String SMTP_HOST = emailConfig.getProperty("email.smtp.host", "smtp.gmail.com");
    private static final String SMTP_PORT = emailConfig.getProperty("email.smtp.port", "587");
    private static final String SENDER_EMAIL = emailConfig.getProperty("email.sender.email", "shaco54lol@gmail.com");
    private static final String SENDER_PASSWORD = emailConfig.getProperty("email.sender.password", "");
    private static final String TEST_EMAIL = emailConfig.getProperty("email.test.recipient", SENDER_EMAIL);

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Load email configuration from properties file
     */
    private static Properties loadEmailConfiguration() {
        Properties props = new Properties();

        try (InputStream input = EmailService.class.getClassLoader()
                .getResourceAsStream("email.properties")) {

            if (input == null) {
                System.out.println("⚠️  email.properties not found. Using default values (simulation mode).");
                System.out.println("💡 Tip: Copy email.properties.template to email.properties and configure your credentials.");
                return props; // Return empty properties, will use defaults
            }

            props.load(input);
            System.out.println("✅ Email configuration loaded successfully from email.properties");

            // Debug: Show if password is configured (without exposing it)
            String password = props.getProperty("email.sender.password", "");
            if (password.isEmpty()) {
                System.out.println("⚠️  Email password is empty - running in SIMULATION MODE");
            } else {
                System.out.println("✅ Email password configured (length: " + password.length() + " chars) - REAL EMAIL MODE");
            }

        } catch (IOException e) {
            System.err.println("❌ Error loading email configuration: " + e.getMessage());
            System.out.println("💡 Using default values (simulation mode).");
        }

        return props;
    }

    /**
     * Send an email reminder for an upcoming interview
     */
    public static void sendInterviewReminder(Interview interview, String recipientEmail) {
        try {
            String subject = "Rappel: Entretien prévu demain";
            String body = buildReminderEmailBody(interview);

            sendEmail(recipientEmail, subject, body);
            System.out.println("Email reminder sent successfully to: " + recipientEmail);
        } catch (Exception e) {
            System.err.println("Failed to send email reminder: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Build the email body for interview reminder
     */
    private static String buildReminderEmailBody(Interview interview) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

        StringBuilder body = new StringBuilder();
        body.append("Bonjour,\n\n");
        body.append("Ceci est un rappel pour votre entretien prévu demain.\n\n");
        body.append("Détails de l'entretien:\n");
        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        body.append("📅 Date et Heure: ").append(interview.getScheduledAt().format(formatter)).append("\n");
        body.append("⏱️ Durée: ").append(interview.getDurationMinutes()).append(" minutes\n");
        body.append("📍 Mode: ").append(interview.getMode()).append("\n");

        if ("ONLINE".equals(interview.getMode()) && interview.getMeetingLink() != null) {
            body.append("🔗 Lien de réunion: ").append(interview.getMeetingLink()).append("\n");
        } else if ("ON_SITE".equals(interview.getMode()) && interview.getLocation() != null) {
            body.append("📍 Lieu: ").append(interview.getLocation()).append("\n");
        }

        if (interview.getNotes() != null && !interview.getNotes().isEmpty()) {
            body.append("\n📝 Notes: ").append(interview.getNotes()).append("\n");
        }

        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        body.append("Merci de confirmer votre présence.\n\n");
        body.append("Cordialement,\n");
        body.append("L'équipe Talent Bridge");

        return body.toString();
    }

    /**
     * Send an email using SMTP
     */
    private static void sendEmail(String toEmail, String subject, String body) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        // For testing without actual credentials
        if (SENDER_PASSWORD == null || SENDER_PASSWORD.isEmpty()) {
            System.out.println("\n=== EMAIL SIMULATION MODE ===");
            System.out.println("⚠️  No email sent - password not configured!");
            System.out.println("To: " + toEmail);
            System.out.println("Subject: " + subject);
            System.out.println("Body:\n" + body);
            System.out.println("============================\n");
            System.out.println("💡 To send real emails, configure email.sender.password in email.properties");
            return;
        }

        // Real email sending mode
        System.out.println("📧 Sending real email via SMTP to: " + toEmail);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
        System.out.println("✅ Email sent successfully via SMTP!");
    }

    /**
     * Test method to send a test email immediately
     */
    public static void sendTestEmail() {
        try {
            String subject = "Test - Talent Bridge Email Service";
            String body = "Ceci est un email de test du système Talent Bridge.\n\n" +
                         "Si vous recevez cet email, le service fonctionne correctement!\n\n" +
                         "Cordialement,\n" +
                         "L'équipe Talent Bridge";

            sendEmail(TEST_EMAIL, subject, body);
            System.out.println("Test email sent successfully!");
        } catch (Exception e) {
            System.err.println("Failed to send test email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shutdown the scheduler
     */
    public static void shutdown() {
        scheduler.shutdown();
    }
}






