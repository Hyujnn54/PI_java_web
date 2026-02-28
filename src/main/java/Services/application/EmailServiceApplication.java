package Services.application;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

/**
 * Email service dedicated to application-related notifications.
 * Sends confirmation emails to candidates when they apply for a job offer.
 */
public class EmailServiceApplication {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy 'à' HH:mm", Locale.FRENCH);

    // Hardcoded Gmail SMTP configuration
    private static final String  SMTP_HOST    = "smtp.gmail.com";
    private static final String  SMTP_PORT    = "587";
    private static final String  SMTP_USER    = "tunisiatour0@gmail.com";
    private static final String  SMTP_PASS    = "etnbvnqqdejttdsc";
    private static final String  DISPLAY_NAME = "Équipe Recrutement – Talent Bridge";
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

        String subject  = "Candidature reçue – " + offerTitle;
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

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER, DISPLAY_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);

            // Multipart: plain text fallback + HTML
            javax.mail.internet.MimeMultipart multipart = new javax.mail.internet.MimeMultipart("alternative");

            javax.mail.internet.MimeBodyPart textPart = new javax.mail.internet.MimeBodyPart();
            textPart.setContent(textBody, "text/plain; charset=UTF-8");

            javax.mail.internet.MimeBodyPart htmlPart = new javax.mail.internet.MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);
            message.setContent(multipart);

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
    // HTML body
    // -------------------------------------------------------------------------

    private static String buildHtmlBody(String candidateName, String offerTitle, LocalDateTime appliedAt) {
        String name = escapeHtml(candidateName != null && !candidateName.isBlank() ? candidateName : "Candidat");
        String offer = escapeHtml(offerTitle != null ? offerTitle : "");
        String date  = appliedAt != null ? appliedAt.format(DATE_FORMAT) : "aujourd'hui";

        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>");
        h.append("<body style='margin:0;padding:0;background:#f0f2f5;font-family:Arial,Helvetica,sans-serif;'>");
        h.append("<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f2f5;padding:30px 0;'><tr><td align='center'>");
        h.append("<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;'>");

        // Header
        h.append("<tr><td style='background:linear-gradient(135deg,#4f46e5 0%,#7c3aed 100%);border-radius:12px 12px 0 0;padding:36px 40px;text-align:center;'>");
        h.append("<div style='font-size:13px;color:rgba(255,255,255,.7);letter-spacing:1px;text-transform:uppercase;'>Talent Bridge</div>");
        h.append("<div style='font-size:26px;font-weight:700;color:#fff;margin-top:6px;'>Candidature reçue ✓</div>");
        h.append("</td></tr>");

        // Body
        h.append("<tr><td style='background:#fff;padding:40px;'>");
        h.append("<p style='font-size:16px;color:#1e1e2e;margin-top:0;'>Bonjour <strong>").append(name).append("</strong>,</p>");
        h.append("<p style='font-size:15px;color:#4b5563;'>Votre candidature a été <strong>reçue avec succès</strong>. Voici un récapitulatif :</p>");

        // Details card
        h.append("<table width='100%' style='background:#f8f7ff;border:1px solid #e5e7eb;border-radius:10px;margin-bottom:28px;'>");
        infoRow(h, "&#128188;", "Poste",           offer);
        infoRow(h, "&#128197;", "Date de dépôt",   date);
        infoRow(h, "&#128203;", "Statut",           "En cours d'examen");
        h.append("</table>");

        // What happens next
        h.append("<p style='font-size:14px;color:#4b5563;margin-bottom:6px;'><strong>Prochaines étapes :</strong></p>");
        h.append("<ul style='font-size:14px;color:#4b5563;padding-left:20px;line-height:1.8;'>");
        h.append("<li>Notre équipe de recrutement va examiner votre profil.</li>");
        h.append("<li>Si votre expérience correspond à nos besoins, nous vous contacterons pour planifier un entretien.</li>");
        h.append("<li>Vous pouvez suivre l'état de votre candidature dans l'application Talent Bridge.</li>");
        h.append("</ul>");

        // CTA button
        h.append("<table width='100%' style='margin-top:28px;'><tr><td align='center'>");
        h.append("<div style='display:inline-block;background:linear-gradient(135deg,#4f46e5 0%,#7c3aed 100%);color:#fff;font-size:15px;font-weight:700;padding:14px 44px;border-radius:50px;'>Bonne chance !</div>");
        h.append("</td></tr></table>");

        h.append("<p style='color:#4b5563;margin-top:32px;'>Cordialement,<br><strong>L'équipe Talent Bridge</strong></p>");
        h.append("</td></tr>");

        // Footer
        h.append("<tr><td style='background:#f8f7ff;border-radius:0 0 12px 12px;padding:20px 40px;text-align:center;'>");
        h.append("<p style='font-size:12px;color:#9ca3af;margin:0;'>Email automatique — merci de ne pas répondre directement à ce message.</p>");
        h.append("<p style='font-size:12px;color:#9ca3af;margin:4px 0 0;'>© 2026 Talent Bridge</p>");
        h.append("</td></tr>");

        h.append("</table></td></tr></table></body></html>");
        return h.toString();
    }

    // -------------------------------------------------------------------------
    // Plain-text fallback
    // -------------------------------------------------------------------------

    private static String buildTextBody(String candidateName, String offerTitle, LocalDateTime appliedAt) {
        String name  = (candidateName != null && !candidateName.isBlank()) ? candidateName : "Candidat";
        String date  = (appliedAt != null) ? appliedAt.format(DATE_FORMAT) : "aujourd'hui";

        return "Bonjour " + name + ",\n\n"
             + "Votre candidature pour le poste \"" + offerTitle + "\" a été reçue le " + date + ".\n\n"
             + "Notre équipe de recrutement va examiner votre profil. Si votre expérience correspond "
             + "à nos besoins, nous vous contacterons pour planifier un entretien.\n\n"
             + "Vous pouvez suivre l'état de votre candidature dans l'application Talent Bridge.\n\n"
             + "Bonne chance !\n\n"
             + "Cordialement,\n"
             + "L'équipe Talent Bridge\n"
             + "--------------------------------------------------\n"
             + "Email automatique — merci de ne pas répondre directement à ce message.";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void infoRow(StringBuilder h, String icon, String label, String value) {
        h.append("<tr><td style='padding:12px 18px;font-size:18px;'>").append(icon).append("</td>");
        h.append("<td style='padding:12px 0;font-size:13px;font-weight:700;color:#6b7280;width:140px;'>").append(label).append("</td>");
        h.append("<td style='padding:12px 18px 12px 0;font-size:14px;color:#1e1e2e;'>").append(value).append("</td></tr>");
    }

    private static String escapeHtml(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}

