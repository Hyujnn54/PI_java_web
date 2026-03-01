package Services.application;

import Models.interview.Interview;
import Services.interview.InterviewEmailService;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

/**
 * Email service for APPLICATION notifications only (SMTP via Gmail).
 * INTERVIEW emails -> InterviewEmailService (Brevo API).
 * APPLICATION confirmation -> handled here via SMTP.
 */
public class EmailServiceApplication {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy HH:mm", Locale.FRENCH);

    private static final String  SMTP_HOST     = "smtp.gmail.com";
    private static final String  SMTP_PORT     = "587";
    private static final String  SMTP_USER     = "tunisiatour0@gmail.com";
    private static final String  SMTP_PASS     = "etnbvnqqdejttdsc";
    private static final String  DISPLAY_NAME  = "Talent Bridge Recrutement";
    private static final boolean SMTP_STARTTLS = true;

    private static final String TEST_RECIPIENT = loadTestRecipient();

    private static String loadTestRecipient() {
        try (InputStream in = EmailServiceApplication.class.getClassLoader()
                .getResourceAsStream("email.properties")) {
            if (in == null) return "";
            Properties p = new Properties();
            p.load(in);
            String r = p.getProperty("email.test.recipient", "").trim();
            if (!r.isBlank()) System.out.println("[EmailServiceApplication] Test recipient: " + r);
            return r;
        } catch (Exception e) {
            return "";
        }
    }

    private static String resolveRecipient(String original) {
        if (TEST_RECIPIENT != null && !TEST_RECIPIENT.isBlank()) return TEST_RECIPIENT;
        return original;
    }

    // -------------------------------------------------------------------------
    // Application confirmation email (SMTP)
    // -------------------------------------------------------------------------

    public static boolean sendApplicationConfirmation(String toEmail,
                                                       String candidateName,
                                                       String offerTitle,
                                                       LocalDateTime appliedAt) {
        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("[EmailServiceApplication] sendApplicationConfirmation: email missing.");
            return false;
        }
        String effectiveEmail = resolveRecipient(toEmail);
        String subject  = "Candidature recue - " + offerTitle;
        String textBody = buildTextBody(candidateName, offerTitle, appliedAt);
        String htmlBody = buildHtmlBody(candidateName, offerTitle, appliedAt);
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

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER, DISPLAY_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(effectiveEmail));
            message.setSubject(subject, "UTF-8");

            MimeMultipart multipart = new MimeMultipart("alternative");
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(textBody, "text/plain; charset=UTF-8");
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);
            message.setContent(multipart);
            message.saveChanges();

            Transport.send(message);
            System.out.println("[EmailServiceApplication] Confirmation sent to " + effectiveEmail);
            return true;
        } catch (Exception e) {
            System.err.println("[EmailServiceApplication] Failed to send to " + effectiveEmail + ": " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Interview methods -> delegate to InterviewEmailService (Brevo)
    // -------------------------------------------------------------------------

    public static boolean sendInterviewScheduledConfirmation(String toEmail,
                                                              String candidateName,
                                                              String offerTitle,
                                                              Interview interview) {
        return InterviewEmailService.sendScheduledConfirmation(interview, toEmail, candidateName, offerTitle);
    }

    public static boolean sendReminderEmail(String toEmail, String candidateName, Interview interview) {
        return InterviewEmailService.sendReminder(interview, toEmail, candidateName);
    }

    // -------------------------------------------------------------------------
    // HTML / text builders (application only)
    // -------------------------------------------------------------------------

    private static String buildHtmlBody(String candidateName, String offerTitle, LocalDateTime appliedAt) {
        String name  = esc(candidateName != null && !candidateName.isBlank() ? candidateName : "Candidat");
        String offer = esc(offerTitle != null ? offerTitle : "");
        String date  = appliedAt != null ? appliedAt.format(DATE_FORMAT) : "aujourd'hui";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
             + "<body style='margin:0;padding:0;background:#f0f2f5;font-family:Arial,sans-serif'>"
             + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f2f5;padding:30px 0'><tr><td align='center'>"
             + "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>"
             // header
             + "<tr><td style='background:linear-gradient(135deg,#4f46e5,#7c3aed);border-radius:12px 12px 0 0;padding:36px 40px;text-align:center'>"
             + "<div style='font-size:13px;color:rgba(255,255,255,.7);letter-spacing:1px;text-transform:uppercase'>Talent Bridge</div>"
             + "<div style='font-size:26px;font-weight:700;color:#fff;margin-top:6px'>Candidature re&#231;ue &#9989;</div>"
             + "</td></tr>"
             // body
             + "<tr><td style='background:#fff;padding:40px'>"
             + "<p style='font-size:16px;color:#1e1e2e;margin-top:0'>Bonjour <strong>" + name + "</strong>,</p>"
             + "<p style='font-size:15px;color:#4b5563'>Votre candidature pour le poste <strong>" + offer + "</strong> a &#233;t&#233; re&#231;ue le <strong>" + date + "</strong>.</p>"
             + "<table width='100%' style='background:#f8f7ff;border:1px solid #e5e7eb;border-radius:10px;margin-bottom:28px'>"
             + row("&#128188;", "Poste",  offer)
             + row("&#128197;", "Date",   date)
             + row("&#128203;", "Statut", "En cours d'examen")
             + "</table>"
             + "<p style='font-size:14px;color:#4b5563'><strong>Prochaines &#233;tapes :</strong></p>"
             + "<ul style='font-size:14px;color:#4b5563;padding-left:20px;line-height:1.8'>"
             + "<li>Notre &#233;quipe va examiner votre profil.</li>"
             + "<li>Si votre exp&#233;rience correspond, nous vous contacterons pour un entretien.</li>"
             + "<li>Vous pouvez suivre l'&#233;tat de votre candidature dans l'application.</li>"
             + "</ul>"
             + "<p style='color:#4b5563;margin-top:32px'>Bonne chance !<br><strong>L'&#233;quipe Talent Bridge</strong></p>"
             + "</td></tr>"
             // footer
             + "<tr><td style='background:#f8f7ff;border-radius:0 0 12px 12px;padding:20px 40px;text-align:center'>"
             + "<p style='font-size:12px;color:#9ca3af;margin:0'>Email automatique &#8212; merci de ne pas r&#233;pondre directement.</p>"
             + "<p style='font-size:12px;color:#9ca3af;margin:4px 0 0'>&#169; 2026 Talent Bridge</p>"
             + "</td></tr>"
             + "</table></td></tr></table></body></html>";
    }

    private static String row(String icon, String label, String value) {
        return "<tr>"
             + "<td style='padding:10px 14px;font-size:18px'>" + icon + "</td>"
             + "<td style='padding:10px 0;font-size:13px;font-weight:700;color:#6b7280;width:140px'>" + label + "</td>"
             + "<td style='padding:10px 14px 10px 0;font-size:14px;color:#1e1e2e'>" + (value != null ? value : "-") + "</td>"
             + "</tr>";
    }

    private static String buildTextBody(String candidateName, String offerTitle, LocalDateTime appliedAt) {
        String name = (candidateName != null && !candidateName.isBlank()) ? candidateName : "Candidat";
        String date = (appliedAt != null) ? appliedAt.format(DATE_FORMAT) : "aujourd'hui";
        return "Bonjour " + name + ",\n\n"
             + "Votre candidature pour \"" + offerTitle + "\" a ete recue le " + date + ".\n\n"
             + "Notre equipe va examiner votre profil.\n\n"
             + "Bonne chance !\nL'equipe Talent Bridge";
    }

    private static String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
