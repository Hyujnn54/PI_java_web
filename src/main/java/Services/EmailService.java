package Services;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class EmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm");

    // Gmail SMTP Configuration
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SMTP_USER = "tunisiatour0@gmail.com";
    private static final String SMTP_PASS = "etnbvnqqdejttdsc";
    private static final String SMTP_FROM = "RecruitmentTeam <tunisiatour0@gmail.com>";
    private static final boolean SMTP_STARTTLS = true;

    public static boolean sendApplicationConfirmation(String toEmail, String candidateName, String offerTitle, LocalDateTime appliedAt) {
        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("Email not sent: candidate email is missing.");
            return false;
        }

        SmtpConfig config = new SmtpConfig(SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SMTP_FROM, SMTP_STARTTLS);
        System.out.println("SMTP Config - Host: " + config.host + ", User: " + config.user + ", From: " + config.fromEmail + ", Configured: " + config.isConfigured());

        if (!config.isConfigured()) {
            System.err.println("Email not sent: SMTP configuration is missing.");
            return false;
        }

        String subject = "Application Received - " + offerTitle;
        String body = buildApplicationBody(candidateName, offerTitle, appliedAt);

        try {
            Session session = Session.getInstance(config.toProperties(), config.authenticator());
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.fromEmail()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("Application confirmation email sent to " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static String buildApplicationBody(String candidateName, String offerTitle, LocalDateTime appliedAt) {
        String nameLine = candidateName != null && !candidateName.isBlank() ? candidateName : "Candidate";
        String dateLine = appliedAt != null ? appliedAt.format(DATE_FORMAT) : "today";

        return "Hello " + nameLine + ",\n\n" +
            "Thank you for applying for the " + offerTitle + " position. We have received your application on " + dateLine + ".\n\n" +
            "Our recruitment team will review your profile and get back to you if your experience matches the role's requirements.\n" +
            "In the meantime, feel free to keep your application details up to date.\n\n" +
            "Best regards,\n" +
            "The Recruitment Team";
    }

    private record SmtpConfig(String host, String port, String user, String pass, String fromEmail, boolean startTls) {
        boolean isConfigured() {
            return host != null && !host.isBlank() && fromEmail != null && !fromEmail.isBlank();
        }

        Properties toProperties() {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", user != null && !user.isBlank());
            props.put("mail.smtp.starttls.enable", String.valueOf(startTls));
            return props;
        }

        Authenticator authenticator() {
            if (user == null || user.isBlank()) {
                return null;
            }
            return new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass != null ? pass : "");
                }
            };
        }
    }
}







