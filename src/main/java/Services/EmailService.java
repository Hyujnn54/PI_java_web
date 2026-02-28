package Services;

import Models.interview.Interview;
import Services.interview.InterviewService;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

/**
 * Email Service — two sending paths:
 *
 * 1. sendInterviewReminder()  — uses Brevo REST API (interview reminders, 24h before)
 * 2. sendApplicationConfirmation() — uses Gmail SMTP (application received confirmation)
 *
 * Configure:
 *   email.properties  → brevo.api.key, email.sender.email, email.sender.name
 */
public class EmailService {

    // -------------------------------------------------------------------------
    // Brevo config (interview reminders)
    // -------------------------------------------------------------------------

    private static final Properties emailConfig = loadEmailConfiguration();

    private static final String BREVO_API_KEY   = emailConfig.getProperty("brevo.api.key", "");
    private static final String SENDER_EMAIL    = emailConfig.getProperty("email.sender.email", "");
    private static final String SENDER_NAME     = emailConfig.getProperty("email.sender.name", "Talent Bridge");
    private static final String REPLY_TO_EMAIL  = SENDER_EMAIL;
    private static final String BREVO_ENDPOINT  = "https://api.brevo.com/v3/smtp/email";

    /**
     * When set, ALL outgoing emails are redirected to this address (dev/demo mode).
     * Loaded from email.properties → email.test.recipient
     * Set to blank ("") to send to the real recipient.
     */
    private static final String TEST_RECIPIENT  = emailConfig.getProperty("email.test.recipient", "");

    // -------------------------------------------------------------------------
    // SMTP config (application confirmations — teammate's feature)
    // -------------------------------------------------------------------------

    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final String SMTP_PORT     = "587";
    private static final String SMTP_USER     = emailConfig.getProperty("smtp.user",
                                                    emailConfig.getProperty("email.sender.email", ""));
    private static final String SMTP_PASS     = emailConfig.getProperty("smtp.pass",
                                                    emailConfig.getProperty("email.password", ""));
    private static final String SMTP_FROM     = SENDER_NAME + " <" + SMTP_USER + ">";
    private static final boolean SMTP_STARTTLS = true;

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm");

    // -------------------------------------------------------------------------
    // Config loader
    // -------------------------------------------------------------------------

    private static Properties loadEmailConfiguration() {
        Properties props = new Properties();
        try (InputStream in = EmailService.class.getClassLoader()
                .getResourceAsStream("email.properties")) {
            if (in == null) {
                System.out.println("[EmailService] email.properties not found - SIMULATION MODE.");
                return props;
            }
            props.load(in);
            String key = props.getProperty("brevo.api.key", "");
            if (key.isBlank()) {
                System.out.println("[EmailService] brevo.api.key is empty - SIMULATION MODE.");
            } else {
                System.out.println("[EmailService] Brevo API key loaded (" + key.length() + " chars).");
            }
        } catch (IOException e) {
            System.err.println("[EmailService] Failed to load email.properties: " + e.getMessage());
        }
        return props;
    }

    // =========================================================================
    // PUBLIC API — Interview Acceptance Notification
    // =========================================================================

    /**
     * Send a congratulatory acceptance email with job details via Brevo.
     */
    public static void sendAcceptanceNotification(String toEmail, String candidateName,
                                                   String jobTitle, String jobLocation,
                                                   String contractType, String jobDescription) {
        String effectiveEmail = resolveRecipient(toEmail);
        String effectiveName  = candidateName;
        if (!effectiveEmail.equals(toEmail)) {
            System.out.println("[EmailService] DEV REDIRECT acceptance: " + toEmail + " → " + effectiveEmail);
            effectiveName = candidateName + " (redirect from " + toEmail + ")";
        }
        try {
            String subject  = "Felicitations ! Vous etes retenu(e) — " + jobTitle + " | Talent Bridge";
            String textBody = buildAcceptanceTextBody(candidateName, jobTitle, jobLocation, contractType);
            String htmlBody = buildAcceptanceHtmlBody(candidateName, jobTitle, jobLocation, contractType, jobDescription);
            sendViaBrevo(effectiveEmail, effectiveName, subject, textBody, htmlBody);
            System.out.println("[EmailService] Acceptance notification sent to: " + effectiveEmail);
        } catch (Exception e) {
            System.err.println("[EmailService] Acceptance email failed: " + e.getMessage());
        }
    }

    private static String buildAcceptanceTextBody(String name, String jobTitle,
                                                    String location, String contract) {
        return "Bonjour " + name + ",\n\n"
            + "Toute l'equipe Talent Bridge est ravie de vous informer que votre candidature "
            + "pour le poste de « " + jobTitle + " » a ete retenue !\n\n"
            + "Details du poste :\n"
            + "-----------------------------------\n"
            + "Poste    : " + jobTitle + "\n"
            + (location != null && !location.isBlank() ? "Lieu     : " + location + "\n" : "")
            + (contract != null && !contract.isBlank() ? "Contrat  : " + contract + "\n" : "")
            + "-----------------------------------\n\n"
            + "Notre equipe prendra contact avec vous tres prochainement pour vous communiquer "
            + "les prochaines etapes et les details pratiques.\n\n"
            + "Encore une fois, toutes nos felicitations !\n\n"
            + "Cordialement,\nL'equipe Talent Bridge";
    }

    private static String buildAcceptanceHtmlBody(String candidateName, String jobTitle,
                                                    String jobLocation, String contractType,
                                                    String jobDescription) {
        String name     = escapeHtml(candidateName  != null ? candidateName  : "Candidat");
        String title    = escapeHtml(jobTitle        != null ? jobTitle        : "");
        String loc      = escapeHtml(jobLocation     != null ? jobLocation     : "");
        String contract = escapeHtml(contractType    != null ? contractType    : "");
        String descSnip = jobDescription != null && !jobDescription.isBlank()
                ? escapeHtml(jobDescription.length() > 300
                        ? jobDescription.substring(0, 297) + "…" : jobDescription)
                : "";

        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'></head>");
        h.append("<body style='margin:0;padding:0;background:#EBF0F8;"
                + "font-family:\"Segoe UI\",Helvetica,Arial,sans-serif;'>");

        // Outer wrapper
        h.append("<table width='100%' cellpadding='0' cellspacing='0' "
                + "style='background:#EBF0F8;padding:36px 0;'><tr><td align='center'>");
        h.append("<table width='600' cellpadding='0' cellspacing='0' "
                + "style='max-width:600px;width:100%;'>");

        // ── Header banner ──────────────────────────────────────────────────────
        h.append("<tr><td style='"
                + "background:linear-gradient(135deg,#1565C0 0%,#5BA3F5 60%,#2ECC71 100%);"
                + "border-radius:16px 16px 0 0;padding:42px 40px 36px;text-align:center;'>");
        h.append("<div style='font-size:48px;'>🎉</div>");
        h.append("<div style='font-size:13px;color:rgba(255,255,255,.75);"
                + "letter-spacing:2px;margin-bottom:8px;'>TALENT BRIDGE</div>");
        h.append("<div style='font-size:28px;font-weight:800;color:#fff;"
                + "line-height:1.2;'>Felicitations !</div>");
        h.append("<div style='font-size:15px;color:rgba(255,255,255,.88);"
                + "margin-top:8px;'>Votre candidature a ete retenue</div>");
        h.append("</td></tr>");

        // ── Body ───────────────────────────────────────────────────────────────
        h.append("<tr><td style='background:#ffffff;padding:40px;'>");

        // Greeting
        h.append("<p style='font-size:16px;color:#1E293B;margin:0 0 18px;'>"
                + "Bonjour <strong>").append(name).append("</strong>,</p>");
        h.append("<p style='font-size:15px;color:#475569;line-height:1.7;margin:0 0 24px;'>"
                + "Nous avons le grand plaisir de vous informer que votre candidature pour le poste de "
                + "<strong style='color:#1565C0;'>").append(title).append("</strong>"
                + " a ete <strong style='color:#2ECC71;'>retenue</strong> ! "
                + "Toute l'equipe vous adresse ses plus chaleureuses felicitations.</p>");

        // Job details card
        h.append("<table width='100%' cellpadding='0' cellspacing='0' style='"
                + "background:#F7FAFF;border:1px solid #DCEEFB;"
                + "border-radius:12px;margin-bottom:28px;overflow:hidden;'>");
        h.append("<tr><td colspan='2' style='"
                + "background:linear-gradient(to right,#1565C0,#5BA3F5);"
                + "padding:12px 20px;'>"
                + "<span style='color:white;font-size:13px;font-weight:700;"
                + "letter-spacing:1px;'>DETAILS DU POSTE</span></td></tr>");

        acceptanceInfoRow(h, "💼", "Poste",   title);
        if (!loc.isBlank())      acceptanceInfoRow(h, "📍", "Lieu",     loc);
        if (!contract.isBlank()) acceptanceInfoRow(h, "📄", "Contrat",  contract);

        h.append("</table>");

        // Description snippet
        if (!descSnip.isBlank()) {
            h.append("<div style='background:#F0F9FF;border-left:4px solid #5BA3F5;"
                    + "padding:16px 20px;border-radius:0 8px 8px 0;margin-bottom:28px;'>");
            h.append("<div style='font-size:12px;font-weight:700;color:#1565C0;"
                    + "margin-bottom:8px;'>DESCRIPTION DU POSTE</div>");
            h.append("<p style='font-size:13px;color:#475569;line-height:1.7;margin:0;'>")
             .append(descSnip).append("</p>");
            h.append("</div>");
        }

        // Next steps
        h.append("<div style='background:#F0FFF4;border:1px solid #A7F3D0;"
                + "border-radius:12px;padding:20px;margin-bottom:28px;'>");
        h.append("<div style='font-size:13px;font-weight:700;color:#065F46;"
                + "margin-bottom:10px;'>PROCHAINES ETAPES</div>");
        h.append("<ul style='margin:0;padding-left:20px;color:#065F46;font-size:13px;line-height:1.9;'>");
        h.append("<li>Notre equipe vous contactera tres prochainement</li>");
        h.append("<li>Vous recevrez les details pratiques par email ou telephone</li>");
        h.append("<li>Preparez vos documents (piece d'identite, diplomes, etc.)</li>");
        h.append("</ul></div>");

        // Sign-off
        h.append("<p style='font-size:15px;color:#475569;line-height:1.7;margin:0 0 6px;'>"
                + "Encore toutes nos felicitations, et bienvenue dans votre nouvelle aventure !</p>");
        h.append("<p style='font-size:14px;color:#1565C0;font-weight:600;margin:0;'>"
                + "L'equipe Talent Bridge</p>");

        h.append("</td></tr>");

        // ── Footer ─────────────────────────────────────────────────────────────
        h.append("<tr><td style='background:#F7FAFF;border-radius:0 0 16px 16px;"
                + "padding:20px 40px;text-align:center;'>");
        h.append("<p style='font-size:11px;color:#94A3B8;margin:0;'>"
                + "Cet email a ete envoye automatiquement par Talent Bridge. "
                + "Merci de ne pas y repondre directement.</p>");
        h.append("</td></tr></table></td></tr></table></body></html>");

        return h.toString();
    }

    private static void acceptanceInfoRow(StringBuilder h, String icon, String label, String value) {
        h.append("<tr><td style='padding:12px 20px;font-size:18px;width:42px;'>")
         .append(icon).append("</td>");
        h.append("<td style='padding:12px 0;font-size:12px;font-weight:700;color:#64748B;"
                + "width:120px;'>").append(escapeHtml(label)).append("</td>");
        h.append("<td style='padding:12px 20px 12px 0;font-size:14px;color:#1E293B;font-weight:600;'>")
         .append(value).append("</td></tr>");
    }

    // =========================================================================
    // PUBLIC API — Interview Reminders (Brevo)
    // =========================================================================

    /**
     * Send a 24h interview reminder to a candidate via Brevo REST API.
     */
    public static void sendInterviewReminder(Interview interview, String recipientEmail, String candidateName) {
        // In dev/demo mode, redirect to the configured test recipient
        String effectiveEmail = resolveRecipient(recipientEmail);
        String effectiveName  = candidateName;
        if (!effectiveEmail.equals(recipientEmail)) {
            System.out.println("[EmailService] DEV REDIRECT: " + recipientEmail + " → " + effectiveEmail);
            effectiveName = candidateName + " (redirect from " + recipientEmail + ")";
        }
        try {
            String subject  = "Rappel : Votre entretien est prevu demain - Talent Bridge";
            String textBody = buildTextBody(interview, candidateName);
            String htmlBody = buildHtmlBody(interview, candidateName);
            sendViaBrevo(effectiveEmail, effectiveName, subject, textBody, htmlBody);
            System.out.println("[EmailService] Reminder sent via Brevo to: " + effectiveEmail);
        } catch (Exception e) {
            System.err.println("[EmailService] Brevo failed (" + e.getMessage() + "), falling back to Gmail SMTP...");
            try {
                boolean sent = Services.application.EmailServiceApplication
                        .sendReminderEmail(effectiveEmail, candidateName, interview);
                System.out.println("[EmailService] Fallback Gmail SMTP reminder " + (sent ? "sent ✓" : "FAILED ✗") + " to: " + effectiveEmail);
            } catch (Exception ex) {
                System.err.println("[EmailService] Fallback also failed: " + ex.getMessage());
            }
        }
    }

    /**
     * Returns TEST_RECIPIENT if configured, otherwise returns the original email.
     * This ensures dev/demo emails always reach a real inbox.
     */
    private static String resolveRecipient(String originalEmail) {
        if (TEST_RECIPIENT != null && !TEST_RECIPIENT.isBlank()) {
            return TEST_RECIPIENT;
        }
        return originalEmail;
    }

    /** Overload without name. */
    public static void sendInterviewReminder(Interview interview, String recipientEmail) {
        sendInterviewReminder(interview, recipientEmail, "Candidat");
    }

    /**
     * Send a test reminder to a CUSTOM email (not from DB).
     * Uses the first upcoming interview from DB for content, but sends to the given address.
     */
    public static void sendTestTo(String customEmail, String customName) {
        System.out.println("[EmailService] sendTestTo: " + customEmail);
        try {
            List<Interview> interviews = InterviewService.getAll();
            Interview target = interviews.stream()
                .filter(i -> i.getId() != null && i.getScheduledAt() != null)
                .findFirst().orElse(null);

            if (target == null) {
                // Build a dummy interview for the test
                target = new Interview(1L, 1L,
                        LocalDateTime.now().plusDays(1).withHour(14).withMinute(0),
                        60, "ONLINE");
                target.setMeetingLink("https://meet.example.com/test");
                target.setNotes("Test de notification Talent Bridge");
            }

            String name = (customName != null && !customName.isBlank()) ? customName : "Test Utilisateur";
            sendInterviewReminder(target, customEmail, name);
            System.out.println("[EmailService] Test email sent to: " + customEmail);
        } catch (Exception e) {
            System.err.println("[EmailService] sendTestTo failed: " + e.getMessage());
        }
    }

    /**
     * DB-based test: finds the first upcoming interview and sends a real reminder.
     */
    public static void sendTestFromDatabase() {
        System.out.println("[EmailService] Looking for a real interview in the DB to test with...");
        try {
            List<Interview> interviews = InterviewService.getAll();
            Interview target = interviews.stream()
                .filter(i -> i.getId() != null && i.getScheduledAt() != null
                          && i.getScheduledAt().isAfter(LocalDateTime.now().minusHours(1)))
                .findFirst().orElse(null);

            if (target == null) {
                System.out.println("[EmailService] No upcoming interviews in DB - nothing to test.");
                return;
            }

            String[] contact = getCandidateContactFromDb(target.getApplicationId());
            if (contact == null || contact[0] == null || contact[0].isBlank()) {
                System.out.println("[EmailService] No email found for interview #" + target.getId());
                return;
            }

            String email = contact[0];
            String name  = contact[1] != null ? contact[1] : "Candidat";
            System.out.println("[EmailService] Sending test reminder for interview #"
                + target.getId() + " to " + email + " (" + name + ")");
            sendInterviewReminder(target, email, name);

        } catch (Exception e) {
            System.err.println("[EmailService] DB test failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // PUBLIC API — Application Confirmation (SMTP / teammate feature)
    // =========================================================================

    /**
     * Send application received confirmation via Gmail SMTP.
     * Called by the applications module when a candidate submits an application.
     */
    public static boolean sendApplicationConfirmation(String toEmail, String candidateName,
                                                       String offerTitle, LocalDateTime appliedAt) {
        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("[EmailService] sendApplicationConfirmation: email is missing.");
            return false;
        }

        if (SMTP_USER.isBlank()) {
            System.err.println("[EmailService] SMTP user not configured — skipping application confirmation.");
            return false;
        }

        String subject = "Application Received - " + offerTitle;
        String body    = buildApplicationBody(candidateName, offerTitle, appliedAt);

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host",           SMTP_HOST);
            props.put("mail.smtp.port",           SMTP_PORT);
            props.put("mail.smtp.auth",           "true");
            props.put("mail.smtp.starttls.enable", String.valueOf(SMTP_STARTTLS));

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("[EmailService] Application confirmation sent to " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send application confirmation: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // DB helpers
    // =========================================================================

    private static String[] getCandidateContactFromDb(Long applicationId) {
        if (applicationId == null) return null;
        String sql = "SELECT u.email, u.first_name, u.last_name " +
                     "FROM job_application ja " +
                     "JOIN users u ON ja.candidate_id = u.id " +
                     "WHERE ja.id = ?";
        try {
            Connection conn = Utils.MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, applicationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String email = rs.getString("email");
                        String fn    = rs.getString("first_name");
                        String ln    = rs.getString("last_name");
                        String name  = ((fn != null ? fn : "") + " " + (ln != null ? ln : "")).trim();
                        if (name.isEmpty()) name = "Candidat";
                        return new String[]{email, name};
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[EmailService] DB lookup error: " + e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // Brevo HTTP send
    // =========================================================================

    private static void sendViaBrevo(String toEmail, String toName,
                                     String subject, String textBody, String htmlBody)
            throws IOException, InterruptedException {

        if (BREVO_API_KEY.isBlank()) {
            printSimulation(toEmail, subject, textBody);
            return;
        }

        String payload = buildBrevoPayload(toEmail, toName, subject, textBody, htmlBody);

        System.out.println("[EmailService] Sending via Brevo to: " + toName + " <" + toEmail + ">");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BREVO_ENDPOINT))
            .header("accept",       "application/json")
            .header("content-type", "application/json")
            .header("api-key",      BREVO_API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        HttpResponse<String> response =
            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        String body = response.body();

        System.out.println("[EmailService] HTTP status : " + status);
        System.out.println("[EmailService] Response    : " + body);

        if (status >= 200 && status < 300) {
            System.out.println("[EmailService] Email accepted by Brevo (HTTP " + status + ").");
        } else if (status == 401) {
            throw new IOException("Brevo 401: Invalid API key.");
        } else {
            throw new IOException("Brevo HTTP " + status + ": " + body);
        }
    }

    // =========================================================================
    // Payload & body builders
    // =========================================================================

    private static String buildBrevoPayload(String toEmail, String toName,
                                            String subject, String textBody, String htmlBody) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"sender\":{\"name\":\"").append(escapeJson(SENDER_NAME))
          .append("\",\"email\":\"").append(escapeJson(SENDER_EMAIL)).append("\"},");
        sb.append("\"to\":[{\"email\":\"").append(escapeJson(toEmail))
          .append("\",\"name\":\"").append(escapeJson(toName)).append("\"}],");
        if (!REPLY_TO_EMAIL.isBlank()) {
            sb.append("\"replyTo\":{\"email\":\"").append(escapeJson(REPLY_TO_EMAIL))
              .append("\",\"name\":\"").append(escapeJson(SENDER_NAME)).append("\"},");
        }
        sb.append("\"subject\":\"").append(escapeJson(subject)).append("\",");
        sb.append("\"textContent\":\"").append(escapeJson(textBody)).append("\",");
        sb.append("\"htmlContent\":\"").append(escapeJson(htmlBody)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String buildApplicationBody(String candidateName, String offerTitle, LocalDateTime appliedAt) {
        String nameLine = candidateName != null && !candidateName.isBlank() ? candidateName : "Candidate";
        String dateLine = appliedAt != null ? appliedAt.format(DATE_FORMAT) : "today";
        return "Hello " + nameLine + ",\n\n" +
            "Thank you for applying for the " + offerTitle + " position. We have received your application on " + dateLine + ".\n\n" +
            "Our recruitment team will review your profile and get back to you soon.\n\n" +
            "Best regards,\nThe Talent Bridge Team";
    }

    private static String buildTextBody(Interview interview, String candidateName) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a' HH:mm");
        StringBuilder b = new StringBuilder();
        b.append("Bonjour ").append(candidateName).append(",\n\n");
        b.append("Ceci est un rappel pour votre entretien prevu demain.\n\n");
        b.append("Details :\n");
        b.append("--------------------------------------------------\n");
        b.append("Date et Heure : ").append(interview.getScheduledAt().format(fmt)).append("\n");
        b.append("Duree         : ").append(interview.getDurationMinutes()).append(" minutes\n");
        b.append("Mode          : ").append(interview.getMode()).append("\n");
        if ("ONLINE".equals(interview.getMode()) && interview.getMeetingLink() != null) {
            b.append("Lien          : ").append(interview.getMeetingLink()).append("\n");
        } else if ("ON_SITE".equals(interview.getMode()) && interview.getLocation() != null) {
            b.append("Lieu          : ").append(interview.getLocation()).append("\n");
        }
        if (interview.getNotes() != null && !interview.getNotes().isBlank()) {
            b.append("Notes         : ").append(interview.getNotes()).append("\n");
        }
        b.append("--------------------------------------------------\n\n");
        b.append("Bonne chance !\n\nCordialement,\nL'equipe Talent Bridge");
        return b.toString();
    }

    private static String buildHtmlBody(Interview interview, String candidateName) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy 'a' HH:mm",
            java.util.Locale.FRENCH);
        String mode = "ONLINE".equals(interview.getMode()) ? "En ligne (visioconference)" : "Sur site (presentiel)";
        boolean isOnline = "ONLINE".equals(interview.getMode())
            && interview.getMeetingLink() != null && !interview.getMeetingLink().isBlank();
        boolean isOnSite = "ON_SITE".equals(interview.getMode())
            && interview.getLocation() != null && !interview.getLocation().isBlank();

        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>");
        h.append("<body style='margin:0;padding:0;background:#f0f2f5;font-family:Arial,Helvetica,sans-serif;'>");
        h.append("<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f2f5;padding:30px 0;'><tr><td align='center'>");
        h.append("<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;'>");
        // Header
        h.append("<tr><td style='background:linear-gradient(135deg,#4f46e5 0%,#7c3aed 100%);border-radius:12px 12px 0 0;padding:36px 40px;text-align:center;'>");
        h.append("<div style='font-size:13px;color:rgba(255,255,255,.7);'>Talent Bridge</div>");
        h.append("<div style='font-size:26px;font-weight:700;color:#fff;'>Rappel d'entretien</div>");
        h.append("</td></tr>");
        // Body
        h.append("<tr><td style='background:#fff;padding:40px;'>");
        h.append("<p style='font-size:16px;color:#1e1e2e;'>Bonjour <strong>")
         .append(escapeHtml(candidateName)).append("</strong>,</p>");
        h.append("<p style='font-size:15px;color:#4b5563;'>Votre entretien est programme <strong>demain</strong>.</p>");
        // Details card
        h.append("<table width='100%' style='background:#f8f7ff;border:1px solid #e5e7eb;border-radius:10px;margin-bottom:28px;'>");
        infoRow(h, "&#128197;", "Date et heure", interview.getScheduledAt().format(fmt));
        infoRow(h, "&#9200;",   "Duree",         interview.getDurationMinutes() + " minutes");
        infoRow(h, "&#127909;", "Format",        mode);
        if (isOnSite) infoRow(h, "&#128205;", "Lieu", escapeHtml(interview.getLocation()));
        else if (isOnline) infoRow(h, "&#128279;", "Lien",
            "<a href='" + escapeHtml(interview.getMeetingLink()) + "'>" + escapeHtml(interview.getMeetingLink()) + "</a>");
        if (interview.getNotes() != null && !interview.getNotes().isBlank())
            infoRow(h, "&#128203;", "Notes", escapeHtml(interview.getNotes()));
        h.append("</table>");
        if (isOnline) {
            h.append("<table width='100%'><tr><td align='center'>");
            h.append("<a href='").append(escapeHtml(interview.getMeetingLink()))
             .append("' style='display:inline-block;background:#4f46e5;color:#fff;text-decoration:none;font-size:16px;font-weight:700;padding:15px 44px;border-radius:50px;'>Rejoindre l'entretien</a>");
            h.append("</td></tr></table>");
        }
        h.append("<p style='color:#4b5563;'>Bonne chance !<br>L'equipe Talent Bridge</p>");
        h.append("</td></tr>");
        h.append("<tr><td style='background:#f8f7ff;border-radius:0 0 12px 12px;padding:20px 40px;text-align:center;'>");
        h.append("<p style='font-size:12px;color:#9ca3af;'>Email automatique - Talent Bridge</p></td></tr>");
        h.append("</table></td></tr></table></body></html>");
        return h.toString();
    }

    private static void infoRow(StringBuilder h, String icon, String label, String value) {
        h.append("<tr><td style='padding:12px 18px;font-size:18px;'>").append(icon).append("</td>");
        h.append("<td style='padding:12px 0;font-size:13px;font-weight:700;color:#6b7280;width:140px;'>").append(escapeHtml(label)).append("</td>");
        h.append("<td style='padding:12px 18px 12px 0;font-size:14px;color:#1e1e2e;'>").append(value).append("</td></tr>");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static String escapeJson(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String escapeHtml(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void printSimulation(String toEmail, String subject, String body) {
        System.out.println("\n======== EMAIL SIMULATION (no Brevo API key) ========");
        System.out.println("To      : " + toEmail);
        System.out.println("Subject : " + subject);
        System.out.println("Body    :\n" + body);
        System.out.println("=====================================================");
    }
}
