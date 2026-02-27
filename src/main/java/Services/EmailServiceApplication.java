package Services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Email service dedicated to application-related notifications.
 * Sends confirmation emails to candidates when they apply for a job offer.
 */
public class EmailServiceApplication {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm");

    // Hardcoded Gmail SMTP configuration
    private static final String SMTP_HOST      = "smtp.gmail.com";
    private static final String SMTP_PORT      = "587";
    private static final String SMTP_USER      = "tunisiatour0@gmail.com";
    private static final String SMTP_PASS      = "etnbvnqqdejttdsc";
    private static final String DISPLAY_NAME   = "RecruitmentTeam";
    private static final boolean SMTP_STARTTLS = true;

    /**
     * Send an application confirmation email to a candidate.
     *
     * @param toEmail       recipient email address
     * @param candidateName candidate's full name
     * @param offerTitle    job offer title
     * @param appliedAt     date/time of application
     * @return true if the email was sent successfully, false otherwise
     */
    public static boolean sendApplicationConfirmation(String toEmail,
                                                       String candidateName,
                                                       String offerTitle,
                                                       LocalDateTime appliedAt) {
        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("[EmailServiceApplication] Cannot send email: recipient address is missing.");
            return false;
        }

        String subject = "Application Received – " + offerTitle;
        String body    = buildBody(candidateName, offerTitle, appliedAt);

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host",            SMTP_HOST);
            props.put("mail.smtp.port",            SMTP_PORT);
            props.put("mail.smtp.auth",            "true");
            props.put("mail.smtp.starttls.enable", String.valueOf(SMTP_STARTTLS));
            props.put("mail.smtp.ssl.trust",       SMTP_HOST);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
                }
            });

            // Uncomment the line below to see detailed SMTP debug output:
            // session.setDebug(true);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER, DISPLAY_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(body, "text/plain; charset=UTF-8");

            Transport.send(message);
            System.out.println("[EmailServiceApplication] Confirmation email sent to " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("[EmailServiceApplication] Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String buildBody(String candidateName, String offerTitle, LocalDateTime appliedAt) {
        String name = (candidateName != null && !candidateName.isBlank()) ? candidateName : "Candidate";
        String date = (appliedAt != null) ? appliedAt.format(DATE_FORMAT) : "today";

        return "Dear " + name + ",\n\n"
             + "We are pleased to confirm that your application for the position of \""
             + offerTitle + "\" has been successfully received on " + date + ".\n\n"
             + "Our recruitment team will carefully review your profile. "
             + "If your experience and skills match our requirements, we will reach out to you "
             + "to schedule the next steps in our selection process.\n\n"
             + "In the meantime, we encourage you to keep your application details and CV up to date.\n\n"
             + "Thank you for your interest in joining our team. We wish you the best of luck!\n\n"
             + "Warm regards,\n"
             + "The Recruitment Team\n"
             + "──────────────────────────────\n"
             + "This is an automated message — please do not reply directly to this email.";
    }
}



