package Services;

import Models.Interview;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

/**
 * Email Service - sends interview reminders via Brevo (Sendinblue) REST API.
 *
 * SENDER NOTE:
 *   Brevo requires the sender email to be verified.
 *   We use Brevo's own shared domain (sendinblue.com) as the FROM address
 *   which is always pre-verified, and set replyTo = your configured email
 *   so replies come back to you.
 *
 * Setup:
 *   1. Register free at https://app.brevo.com/
 *   2. Settings -> SMTP & API -> API Keys -> Generate
 *   3. Set brevo.api.key in email.properties
 */
public class EmailService {

    private static final Properties emailConfig = loadEmailConfiguration();

    private static final String BREVO_API_KEY = emailConfig.getProperty("brevo.api.key", "");
    private static final String SENDER_EMAIL  = emailConfig.getProperty("email.sender.email", "");
    private static final String SENDER_NAME   = emailConfig.getProperty("email.sender.name", "Talent Bridge");

    // replyTo = same as sender (replies come back to your gmail)
    private static final String REPLY_TO_EMAIL = SENDER_EMAIL;

    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    // -------------------------------------------------------------------------
    // Configuration
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

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Send a 24h interview reminder to a candidate.
     *
     * @param interview      interview details
     * @param recipientEmail candidate's real email from users table
     * @param candidateName  candidate's full name from users table
     */
    public static void sendInterviewReminder(Interview interview, String recipientEmail, String candidateName) {
        try {
            String subject  = "Rappel : Votre entretien est prevu demain - Talent Bridge";
            String textBody = buildTextBody(interview, candidateName);
            String htmlBody = buildHtmlBody(interview, candidateName);
            sendEmail(recipientEmail, candidateName, subject, textBody, htmlBody);
            System.out.println("[EmailService] Reminder sent to: " + recipientEmail);
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send reminder: " + e.getMessage());
        }
    }

    /** Overload for backward compatibility (no name). */
    public static void sendInterviewReminder(Interview interview, String recipientEmail) {
        sendInterviewReminder(interview, recipientEmail, "Candidat");
    }

    /**
     * DB-based test: finds the first upcoming interview in DB and sends
     * a real reminder to that candidate. Never uses hardcoded addresses.
     */
    public static void sendTestFromDatabase() {
        System.out.println("[EmailService] Looking for a real interview in the DB to test with...");
        try {
            List<Interview> interviews = InterviewService.getAll();
            Interview target = interviews.stream()
                .filter(i -> i.getId() != null && i.getScheduledAt() != null
                          && i.getScheduledAt().isAfter(java.time.LocalDateTime.now().minusHours(1)))
                .findFirst().orElse(null);

            if (target == null) {
                System.out.println("[EmailService] No upcoming interviews in DB - nothing to test.");
                return;
            }

            // Get email + name from users table via application
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

    // -------------------------------------------------------------------------
    // DB helper
    // -------------------------------------------------------------------------

    /**
     * Returns [email, fullName] for the candidate of a given application.
     * Uses users.email and users.phone — real registered data, not CV fields.
     */
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

    // -------------------------------------------------------------------------
    // Core HTTP send
    // -------------------------------------------------------------------------

    private static void sendEmail(String toEmail, String toName,
                                  String subject,
                                  String textBody, String htmlBody)
            throws IOException, InterruptedException {

        if (BREVO_API_KEY.isBlank()) {
            printSimulation(toEmail, subject, textBody);
            return;
        }

        String payload = buildPayload(toEmail, toName, subject, textBody, htmlBody);

        System.out.println("[EmailService] --- Sending email ---");
        System.out.println("[EmailService] To      : " + toName + " <" + toEmail + ">");
        System.out.println("[EmailService] From    : " + SENDER_NAME + " <" + SENDER_EMAIL + ">");
        System.out.println("[EmailService] ReplyTo : " + REPLY_TO_EMAIL);
        System.out.println("[EmailService] Subject : " + subject);

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
            if (body.contains("\"messageId\"")) {
                String msgId = body.replaceAll(".*\"messageId\":\"([^\"]+)\".*", "$1");
                System.out.println("[EmailService] Message ID : " + msgId);
            }
            System.out.println("[EmailService] Email accepted by Brevo (HTTP " + status + ").");
            System.out.println("[EmailService] Check INBOX and SPAM of: " + toEmail);
        } else if (status == 400) {
            System.err.println("[EmailService] ERROR 400: " + body);
            throw new IOException("Brevo 400: " + body);
        } else if (status == 401) {
            System.err.println("[EmailService] ERROR 401: Invalid API key.");
            throw new IOException("Brevo 401: " + body);
        } else {
            throw new IOException("Brevo HTTP " + status + ": " + body);
        }
    }

    // -------------------------------------------------------------------------
    // Payload builder
    // -------------------------------------------------------------------------

    private static String buildPayload(String toEmail, String toName,
                                       String subject,
                                       String textBody, String htmlBody) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        // Sender: Brevo shared domain - always verified
        sb.append("\"sender\":{\"name\":\"").append(escapeJson(SENDER_NAME))
          .append("\",\"email\":\"").append(escapeJson(SENDER_EMAIL)).append("\"},");
        // To: real candidate from DB
        sb.append("\"to\":[{\"email\":\"").append(escapeJson(toEmail))
          .append("\",\"name\":\"").append(escapeJson(toName)).append("\"}],");
        // ReplyTo: your configured email so replies come to you
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

    // -------------------------------------------------------------------------
    // Email body builders
    // -------------------------------------------------------------------------

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
            b.append("IMPORTANT : Rejoignez 10 minutes avant l'heure prevue.\n");
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
            && interview.getMeetingLink() != null
            && !interview.getMeetingLink().isBlank();
        boolean isOnSite = "ON_SITE".equals(interview.getMode())
            && interview.getLocation() != null
            && !interview.getLocation().isBlank();

        StringBuilder h = new StringBuilder();

        // ── Wrapper ──────────────────────────────────────────────────────────
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        h.append("<meta name='viewport' content='width=device-width,initial-scale=1'>");
        h.append("</head><body style='margin:0;padding:0;background:#f0f2f5;font-family:Arial,Helvetica,sans-serif;'>");
        h.append("<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f2f5;padding:30px 0;'><tr><td align='center'>");
        h.append("<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;'>");

        // ── Header band ──────────────────────────────────────────────────────
        h.append("<tr><td style='background:linear-gradient(135deg,#4f46e5 0%,#7c3aed 100%);");
        h.append("border-radius:12px 12px 0 0;padding:36px 40px;text-align:center;'>");
        h.append("<div style='font-size:13px;font-weight:600;color:rgba(255,255,255,.7);letter-spacing:2px;text-transform:uppercase;margin-bottom:8px;'>Talent Bridge</div>");
        h.append("<div style='font-size:26px;font-weight:700;color:#ffffff;'>Rappel d'entretien</div>");
        h.append("<div style='width:40px;height:3px;background:rgba(255,255,255,.4);margin:16px auto 0;border-radius:2px;'></div>");
        h.append("</td></tr>");

        // ── White body ───────────────────────────────────────────────────────
        h.append("<tr><td style='background:#ffffff;padding:40px;'>");

        // Greeting
        h.append("<p style='font-size:16px;color:#1e1e2e;margin:0 0 8px;'>Bonjour <strong>")
         .append(escapeHtml(candidateName)).append("</strong>,</p>");
        h.append("<p style='font-size:15px;color:#4b5563;margin:0 0 28px;line-height:1.6;'>");
        h.append("Nous vous rappelons que votre entretien avec l'equipe <strong>Talent Bridge</strong> ");
        h.append("est programme <strong>demain</strong>. Voici le recapitulatif :</p>");

        // ── Info card ────────────────────────────────────────────────────────
        h.append("<table width='100%' cellpadding='0' cellspacing='0' style='");
        h.append("background:#f8f7ff;border:1px solid #e5e7eb;border-radius:10px;margin-bottom:28px;'>");

        // Date
        infoRow(h,
            "Carre-calendrier",
            "&#128197;",
            "Date et heure",
            interview.getScheduledAt().format(fmt));

        // Duration
        infoRow(h,
            "Carre-horloge",
            "&#9200;",
            "Duree",
            interview.getDurationMinutes() + " minutes");

        // Mode
        infoRow(h, "Carre-mode", "&#127909;", "Format", mode);

        // Location or link
        if (isOnSite) {
            infoRow(h, "Carre-lieu", "&#128205;", "Lieu", escapeHtml(interview.getLocation()));
        } else if (isOnline) {
            infoRow(h, "Carre-lien", "&#128279;",
                "Lien de reunion",
                "<a href='" + escapeHtml(interview.getMeetingLink()) + "' "
                + "style='color:#4f46e5;word-break:break-all;'>"
                + escapeHtml(interview.getMeetingLink()) + "</a>");
        }

        // Notes
        if (interview.getNotes() != null && !interview.getNotes().isBlank()) {
            infoRow(h, "Carre-notes", "&#128203;", "Notes", escapeHtml(interview.getNotes()));
        }

        h.append("</table>");

        // ── CTA button (only for online) ─────────────────────────────────────
        if (isOnline) {
            h.append("<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:28px;'><tr><td align='center'>");
            h.append("<a href='").append(escapeHtml(interview.getMeetingLink())).append("' ");
            h.append("style='display:inline-block;background:linear-gradient(135deg,#4f46e5,#7c3aed);");
            h.append("color:#ffffff;text-decoration:none;font-size:16px;font-weight:700;");
            h.append("padding:15px 44px;border-radius:50px;letter-spacing:.5px;'>");
            h.append("Rejoindre l'entretien &rarr;</a>");
            h.append("</td></tr></table>");

            // Small note about join window
            h.append("<div style='background:#fef9c3;border:1px solid #fde047;border-radius:8px;");
            h.append("padding:14px 18px;margin-bottom:28px;font-size:13px;color:#713f12;'>");
            h.append("<strong>Important :</strong> Le lien sera actif 10 minutes avant l'heure prevue. ");
            h.append("Assurez-vous d'etre pret(e) a rejoindre a l'heure.");
            h.append("</div>");
        }

        // Closing
        h.append("<p style='font-size:15px;color:#4b5563;margin:0 0 6px;'>Nous vous souhaitons bonne chance pour cet entretien.</p>");
        h.append("<p style='font-size:15px;color:#4b5563;margin:0;'>Cordialement,</p>");
        h.append("<p style='font-size:15px;font-weight:700;color:#1e1e2e;margin:4px 0 0;'>L'equipe Talent Bridge</p>");

        h.append("</td></tr>");

        // ── Footer ───────────────────────────────────────────────────────────
        h.append("<tr><td style='background:#f8f7ff;border-top:1px solid #e5e7eb;");
        h.append("border-radius:0 0 12px 12px;padding:20px 40px;text-align:center;'>");
        h.append("<p style='font-size:12px;color:#9ca3af;margin:0;'>");
        h.append("Cet email a ete envoye automatiquement par le systeme Talent Bridge.<br>");
        h.append("Veuillez ne pas repondre directement a cet email.");
        h.append("</p></td></tr>");

        // ── Close wrapper ────────────────────────────────────────────────────
        h.append("</table></td></tr></table></body></html>");
        return h.toString();
    }

    /** Renders one info row in the summary card. */
    private static void infoRow(StringBuilder h, String id, String icon, String label, String value) {
        h.append("<tr id='").append(id).append("'>");
        h.append("<td style='padding:14px 18px;width:36px;font-size:18px;vertical-align:top;'>").append(icon).append("</td>");
        h.append("<td style='padding:14px 0;width:140px;font-size:13px;font-weight:700;color:#6b7280;vertical-align:top;text-transform:uppercase;letter-spacing:.5px;'>")
         .append(escapeHtml(label)).append("</td>");
        h.append("<td style='padding:14px 18px 14px 0;font-size:14px;color:#1e1e2e;vertical-align:top;'>").append(value).append("</td>");
        h.append("</tr>");
    }


    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
        System.out.println("\n======== EMAIL SIMULATION (no API key) ========");
        System.out.println("To      : " + toEmail);
        System.out.println("Subject : " + subject);
        System.out.println("Body    :\n" + body);
        System.out.println("================================================");
    }
}
