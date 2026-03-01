package Services.interview;

import Models.interview.Interview;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

/**
 * Interview-specific email service using the Brevo (ex-Sendinblue) Transactional Email API.
 *
 * This service is COMPLETELY SEPARATE from:
 *   - Services.user.EmailService         (SMTP — user/auth emails)
 *   - Services.application.EmailServiceApplication (SMTP — application emails)
 *
 * Used by:
 *   - InterviewReminderScheduler  → 24h reminder
 *   - InterviewService / Controllers → interview scheduled confirmation
 */
public class InterviewEmailService {

    // ── Brevo API config ─────────────────────────────────────────────────────
    private static final String BREVO_API_URL  = "https://api.brevo.com/v3/smtp/email";
    private static final String BREVO_API_KEY;
    private static final String SENDER_EMAIL;
    private static final String SENDER_NAME    = "Talent Bridge";

    static {
        Properties props = new Properties();
        try (InputStream in = InterviewEmailService.class
                .getClassLoader().getResourceAsStream("email.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) {}

        // Allow override from email.properties; fall back to hardcoded key
        BREVO_API_KEY  = props.getProperty("brevo.api.key",
                "xkeysib-REPLACE_WITH_YOUR_BREVO_KEY");
        SENDER_EMAIL   = props.getProperty("email.sender.email",
                "talentbridge.app@gmail.com");
    }

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy 'à' HH:mm", Locale.FRENCH);

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Send a 24-hour interview reminder via Brevo.
     * Called by InterviewReminderScheduler for the CANDIDATE.
     */
    public static boolean sendReminder(Interview interview,
                                       String toEmail,
                                       String candidateName) {
        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("[InterviewEmailService] sendReminder: email is missing.");
            return false;
        }
        String name    = safe(candidateName, "Candidat");
        String subject = "⏰ Rappel : Votre entretien est prévu demain — Talent Bridge";
        String html    = buildReminderHtml(name, interview, false);
        String text    = buildReminderText(name, interview);
        return sendViaBrevo(toEmail, name, subject, html, text);
    }

    /**
     * Send a 24-hour interview reminder to the RECRUITER via Brevo.
     * Called by InterviewReminderScheduler for the RECRUITER.
     */
    public static boolean sendReminderToRecruiter(Interview interview,
                                                   String toEmail,
                                                   String recruiterName,
                                                   String candidateName) {
        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("[InterviewEmailService] sendReminderToRecruiter: email is missing.");
            return false;
        }
        String name    = safe(recruiterName, "Recruteur");
        String subject = "⏰ Rappel : Entretien prévu demain avec " + safe(candidateName, "un candidat") + " — Talent Bridge";
        String html    = buildReminderHtml(name, interview, true);
        String text    = buildReminderText(name, interview);
        return sendViaBrevo(toEmail, name, subject, html, text);
    }

    /**
     * Send an interview-scheduled confirmation via Brevo.
     * Called right after a recruiter schedules an interview.
     */
    public static boolean sendScheduledConfirmation(Interview interview,
                                                     String toEmail,
                                                     String candidateName,
                                                     String offerTitle) {
        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("[InterviewEmailService] sendScheduledConfirmation: email is missing.");
            return false;
        }
        String name    = safe(candidateName, "Candidat");
        String offer   = safe(offerTitle, "le poste");
        String subject = "📅 Entretien planifié — " + offer + " | Talent Bridge";
        String html    = buildConfirmationHtml(name, offer, interview);
        String text    = buildConfirmationText(name, offer, interview);
        return sendViaBrevo(toEmail, name, subject, html, text);
    }

    /**
     * Send an acceptance notification to the candidate via Brevo.
     * Called by InterviewFeedbackService when decision = ACCEPTED.
     */
    public static boolean sendAcceptanceNotification(
            String toEmail, String candidateName,
            String jobTitle, String location,
            String contractType, String description) {
        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("[InterviewEmailService] sendAcceptanceNotification: email is missing.");
            return false;
        }
        String name    = safe(candidateName, "Candidat");
        String subject = "🎉 Félicitations ! Votre candidature a été acceptée — Talent Bridge";
        String html    = buildAcceptanceHtml(name, jobTitle, location, contractType, description);
        String text    = buildAcceptanceText(name, jobTitle, location, contractType);
        return sendViaBrevo(toEmail, name, subject, html, text);
    }

    // ── Brevo HTTP sender ─────────────────────────────────────────────────────

    private static boolean sendViaBrevo(String toEmail, String toName,
                                         String subject, String html, String text) {
        try {
            // Build JSON payload
            String jsonPayload = buildJson(toEmail, toName, subject, html, text);

            URL url = new URL(BREVO_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept",       "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("api-key",      BREVO_API_KEY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status >= 200 && status < 300) {
                System.out.println("[InterviewEmailService] ✅ Brevo email sent to " + toEmail
                        + " (HTTP " + status + ")");
                return true;
            } else {
                // Read error body
                InputStream err = conn.getErrorStream();
                String body = err != null ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "";
                System.err.println("[InterviewEmailService] ❌ Brevo HTTP " + status
                        + " for " + toEmail + " — " + body);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[InterviewEmailService] ❌ Exception sending to "
                    + toEmail + ": " + e.getMessage());
            return false;
        }
    }

    /** Build minimal Brevo transactional email JSON — no external library needed. */
    private static String buildJson(String toEmail, String toName,
                                     String subject, String html, String text) {
        return "{"
            + "\"sender\":{"
            +   "\"name\":" + jsonStr(SENDER_NAME) + ","
            +   "\"email\":" + jsonStr(SENDER_EMAIL)
            + "},"
            + "\"to\":[{"
            +   "\"email\":" + jsonStr(toEmail) + ","
            +   "\"name\":"  + jsonStr(toName)
            + "}],"
            + "\"subject\":" + jsonStr(subject) + ","
            + "\"htmlContent\":" + jsonStr(html) + ","
            + "\"textContent\":" + jsonStr(text)
            + "}";
    }

    // ── HTML / text body builders ─────────────────────────────────────────────

    private static String buildReminderHtml(String name, Interview interview, boolean isRecruiter) {
        String date = interview.getScheduledAt() != null
                ? interview.getScheduledAt().format(DATE_FMT) : "demain";
        String mode = "ONLINE".equals(interview.getMode())
                ? "En ligne (visioconférence)" : "Sur site (présentiel)";
        boolean isOnline = "ONLINE".equals(interview.getMode())
                && interview.getMeetingLink() != null && !interview.getMeetingLink().isBlank();
        boolean isOnSite = "ON_SITE".equals(interview.getMode())
                && interview.getLocation() != null && !interview.getLocation().isBlank();

        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>");
        h.append("<body style='margin:0;padding:0;background:#f0f2f5;font-family:Arial,sans-serif'>");
        h.append("<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f2f5;padding:30px 0'><tr><td align='center'>");
        h.append("<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>");

        h.append("<tr><td style='background:linear-gradient(135deg,#f59e0b,#d97706);border-radius:12px 12px 0 0;padding:36px 40px;text-align:center'>");
        h.append("<div style='font-size:13px;color:rgba(255,255,255,.8);text-transform:uppercase;letter-spacing:1px'>Talent Bridge</div>");
        h.append("<div style='font-size:26px;font-weight:700;color:#fff;margin-top:6px'>⏰ Rappel d'entretien</div>");
        h.append("<div style='font-size:14px;color:rgba(255,255,255,.9);margin-top:8px'>L'entretien a lieu <strong>demain</strong></div>");
        h.append("</td></tr>");

        h.append("<tr><td style='background:#fff;padding:40px'>");
        h.append("<p style='font-size:16px;color:#1e1e2e;margin-top:0'>Bonjour <strong>").append(esc(name)).append("</strong>,</p>");
        if (isRecruiter)
            h.append("<p style='font-size:15px;color:#4b5563'>Rappel : vous avez un entretien prévu <strong>demain</strong>.</p>");
        else
            h.append("<p style='font-size:15px;color:#4b5563'>Rappel : votre entretien est prévu <strong>demain</strong>.</p>");

        h.append("<table width='100%' style='background:#fffbeb;border:1px solid #fde68a;border-radius:10px;margin-bottom:24px'>");
        row(h, "📅", "Date",   date);
        row(h, "⏱",  "Durée",  interview.getDurationMinutes() + " minutes");
        row(h, "🎥",  "Format", mode);
        if (isOnSite) row(h, "📍", "Lieu",  esc(interview.getLocation()));
        if (isOnline) row(h, "🔗", "Lien",
                "<a href='" + esc(interview.getMeetingLink()) + "' style='color:#d97706'>" + esc(interview.getMeetingLink()) + "</a>");
        if (interview.getNotes() != null && !interview.getNotes().isBlank())
            row(h, "📝", "Notes", esc(interview.getNotes()));
        h.append("</table>");

        if (isOnline) {
            h.append("<table width='100%' style='margin-bottom:24px'><tr><td align='center'>");
            h.append("<a href='").append(esc(interview.getMeetingLink()))
             .append("' style='display:inline-block;background:linear-gradient(135deg,#f59e0b,#d97706);")
             .append("color:#fff;text-decoration:none;font-size:15px;font-weight:700;padding:14px 44px;border-radius:50px'>")
             .append("🔗 Rejoindre l'entretien</a>");
            h.append("</td></tr></table>");
        }

        h.append("<p style='color:#4b5563;margin-top:24px'>").append(isRecruiter ? "Bonne journée !" : "Bonne chance !").append("<br><strong>L'équipe Talent Bridge</strong></p>");
        h.append("</td></tr>");
        footer(h, "#fffbeb");
        h.append("</table></td></tr></table></body></html>");
        return h.toString();
    }

    private static String buildAcceptanceHtml(String name, String jobTitle, String location,
                                               String contractType, String description) {
        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>");
        h.append("<body style='margin:0;padding:0;background:#f0f2f5;font-family:Arial,sans-serif'>");
        h.append("<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f2f5;padding:30px 0'><tr><td align='center'>");
        h.append("<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>");

        // Green header for acceptance
        h.append("<tr><td style='background:linear-gradient(135deg,#10b981,#059669);border-radius:12px 12px 0 0;padding:36px 40px;text-align:center'>");
        h.append("<div style='font-size:13px;color:rgba(255,255,255,.8);text-transform:uppercase;letter-spacing:1px'>Talent Bridge</div>");
        h.append("<div style='font-size:26px;font-weight:700;color:#fff;margin-top:6px'>🎉 Félicitations !</div>");
        h.append("<div style='font-size:14px;color:rgba(255,255,255,.9);margin-top:8px'>Votre candidature a été <strong>acceptée</strong></div>");
        h.append("</td></tr>");

        h.append("<tr><td style='background:#fff;padding:40px'>");
        h.append("<p style='font-size:16px;color:#1e1e2e;margin-top:0'>Bonjour <strong>").append(esc(name)).append("</strong>,</p>");
        h.append("<p style='font-size:15px;color:#4b5563'>Nous avons le plaisir de vous informer que votre candidature a été <strong style='color:#10b981'>ACCEPTÉE</strong>.</p>");

        h.append("<table width='100%' style='background:#f0fdf4;border:1px solid #bbf7d0;border-radius:10px;margin-bottom:24px'>");
        row(h, "💼", "Poste",    esc(jobTitle));
        row(h, "📍", "Lieu",     esc(location));
        row(h, "📋", "Contrat",  esc(contractType));
        if (description != null && !description.isBlank()) {
            String shortDesc = description.length() > 250 ? description.substring(0, 250) + "…" : description;
            row(h, "📄", "Détails", esc(shortDesc));
        }
        h.append("</table>");

        h.append("<div style='background:#ecfdf5;border-left:4px solid #10b981;border-radius:8px;padding:16px 20px;margin-bottom:24px'>");
        h.append("<p style='color:#065f46;font-size:14px;margin:0'><strong>Prochaines étapes :</strong> Notre équipe vous contactera très prochainement pour convenir des modalités de prise de poste et vous fournir tous les détails nécessaires.</p>");
        h.append("</div>");

        h.append("<p style='color:#4b5563;margin-top:24px'>Toutes nos félicitations !<br><strong>L'équipe Talent Bridge</strong></p>");
        h.append("</td></tr>");
        footer(h, "#f0fdf4");
        h.append("</table></td></tr></table></body></html>");
        return h.toString();
    }

    private static String buildAcceptanceText(String name, String jobTitle, String location,
                                               String contractType) {
        return "Bonjour " + name + ",\n\n" +
               "Félicitations ! Votre candidature a été ACCEPTÉE.\n\n" +
               "Poste    : " + safe(jobTitle, "N/A") + "\n" +
               "Lieu     : " + safe(location, "N/A") + "\n" +
               "Contrat  : " + safe(contractType, "N/A") + "\n\n" +
               "Notre équipe vous contactera très prochainement.\n\n" +
               "Toutes nos félicitations !\nL'équipe Talent Bridge";
    }

    private static String buildReminderText(String name, Interview interview) {
        String date = interview.getScheduledAt() != null
                ? interview.getScheduledAt().format(DATE_FMT) : "demain";
        String mode = "ONLINE".equals(interview.getMode())
                ? "En ligne (visioconférence)" : "Sur site (présentiel)";
        StringBuilder b = new StringBuilder();
        b.append("Bonjour ").append(name).append(",\n\n");
        b.append("Rappel : votre entretien est prévu demain.\n\n");
        b.append("Date/Heure : ").append(date).append("\n");
        b.append("Durée      : ").append(interview.getDurationMinutes()).append(" minutes\n");
        b.append("Format     : ").append(mode).append("\n");
        if ("ONLINE".equals(interview.getMode()) && interview.getMeetingLink() != null)
            b.append("Lien       : ").append(interview.getMeetingLink()).append("\n");
        else if ("ON_SITE".equals(interview.getMode()) && interview.getLocation() != null)
            b.append("Lieu       : ").append(interview.getLocation()).append("\n");
        if (interview.getNotes() != null && !interview.getNotes().isBlank())
            b.append("Notes      : ").append(interview.getNotes()).append("\n");
        b.append("\nBonne chance !\nL'équipe Talent Bridge");
        return b.toString();
    }

    private static String buildConfirmationHtml(String name, String offerTitle, Interview interview) {
        String date = interview.getScheduledAt() != null
                ? interview.getScheduledAt().format(DATE_FMT) : "date à confirmer";
        String mode = "ONLINE".equals(interview.getMode())
                ? "En ligne (visioconférence)" : "Sur site (présentiel)";
        boolean isOnline = "ONLINE".equals(interview.getMode())
                && interview.getMeetingLink() != null && !interview.getMeetingLink().isBlank();
        boolean isOnSite = "ON_SITE".equals(interview.getMode())
                && interview.getLocation() != null && !interview.getLocation().isBlank();

        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>");
        h.append("<body style='margin:0;padding:0;background:#f0f2f5;font-family:Arial,sans-serif'>");
        h.append("<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f2f5;padding:30px 0'><tr><td align='center'>");
        h.append("<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>");

        // Header — blue/indigo
        h.append("<tr><td style='background:linear-gradient(135deg,#4f46e5,#7c3aed);border-radius:12px 12px 0 0;padding:36px 40px;text-align:center'>");
        h.append("<div style='font-size:13px;color:rgba(255,255,255,.8);text-transform:uppercase;letter-spacing:1px'>Talent Bridge</div>");
        h.append("<div style='font-size:26px;font-weight:700;color:#fff;margin-top:6px'>📅 Entretien planifié</div>");
        h.append("</td></tr>");

        // Body
        h.append("<tr><td style='background:#fff;padding:40px'>");
        h.append("<p style='font-size:16px;color:#1e1e2e;margin-top:0'>Bonjour <strong>")
         .append(esc(name)).append("</strong>,</p>");
        h.append("<p style='font-size:15px;color:#4b5563'>Votre entretien pour le poste <strong>")
         .append(esc(offerTitle)).append("</strong> a été <strong>confirmé</strong>&nbsp;:</p>");

        // Detail card
        h.append("<table width='100%' style='background:#f8f7ff;border:1px solid #e5e7eb;border-radius:10px;margin-bottom:24px'>");
        row(h, "💼", "Poste",  esc(offerTitle));
        row(h, "📅", "Date",   date);
        row(h, "⏱", "Durée",  interview.getDurationMinutes() + " minutes");
        row(h, "🎥", "Format", mode);
        if (isOnSite) row(h, "📍", "Lieu", esc(interview.getLocation()));
        if (isOnline) row(h, "🔗", "Lien",
                "<a href='" + esc(interview.getMeetingLink()) + "' style='color:#4f46e5'>"
                + esc(interview.getMeetingLink()) + "</a>");
        if (interview.getNotes() != null && !interview.getNotes().isBlank())
            row(h, "📝", "Notes", esc(interview.getNotes()));
        h.append("</table>");

        if (isOnline) {
            h.append("<table width='100%' style='margin-bottom:24px'><tr><td align='center'>");
            h.append("<a href='").append(esc(interview.getMeetingLink()))
             .append("' style='display:inline-block;background:linear-gradient(135deg,#4f46e5,#7c3aed);")
             .append("color:#fff;text-decoration:none;font-size:15px;font-weight:700;padding:14px 44px;border-radius:50px'>")
             .append("🔗 Rejoindre l'entretien</a>");
            h.append("</td></tr></table>");
        }

        h.append("<p style='font-size:14px;color:#4b5563'>Un rappel vous sera envoyé <strong>24h avant</strong> l'entretien.</p>");
        h.append("<p style='color:#4b5563;margin-top:24px'>Bonne chance !<br><strong>L'équipe Talent Bridge</strong></p>");
        h.append("</td></tr>");
        footer(h, "#f8f7ff");
        h.append("</table></td></tr></table></body></html>");
        return h.toString();
    }

    private static String buildConfirmationText(String name, String offerTitle, Interview interview) {
        String date = interview.getScheduledAt() != null
                ? interview.getScheduledAt().format(DATE_FMT) : "date à confirmer";
        String mode = "ONLINE".equals(interview.getMode())
                ? "En ligne (visioconférence)" : "Sur site (présentiel)";
        StringBuilder b = new StringBuilder();
        b.append("Bonjour ").append(name).append(",\n\n");
        b.append("Votre entretien pour \"").append(offerTitle).append("\" a été planifié.\n\n");
        b.append("Date/Heure : ").append(date).append("\n");
        b.append("Durée      : ").append(interview.getDurationMinutes()).append(" minutes\n");
        b.append("Format     : ").append(mode).append("\n");
        if ("ONLINE".equals(interview.getMode()) && interview.getMeetingLink() != null)
            b.append("Lien       : ").append(interview.getMeetingLink()).append("\n");
        else if ("ON_SITE".equals(interview.getMode()) && interview.getLocation() != null)
            b.append("Lieu       : ").append(interview.getLocation()).append("\n");
        if (interview.getNotes() != null && !interview.getNotes().isBlank())
            b.append("Notes      : ").append(interview.getNotes()).append("\n");
        b.append("\nUn rappel vous sera envoyé 24h avant l'entretien.");
        b.append("\n\nBonne chance !\nL'équipe Talent Bridge");
        return b.toString();
    }

    // ── HTML helpers ──────────────────────────────────────────────────────────

    private static void row(StringBuilder h, String icon, String label, String value) {
        h.append("<tr>")
         .append("<td style='padding:10px 14px;font-size:17px'>").append(icon).append("</td>")
         .append("<td style='padding:10px 0;font-size:13px;font-weight:700;color:#6b7280;width:130px'>")
         .append(label).append("</td>")
         .append("<td style='padding:10px 14px 10px 0;font-size:14px;color:#1e1e2e'>")
         .append(value != null ? value : "—").append("</td>")
         .append("</tr>");
    }

    private static void footer(StringBuilder h, String bg) {
        h.append("<tr><td style='background:").append(bg)
         .append(";border-radius:0 0 12px 12px;padding:20px 40px;text-align:center'>")
         .append("<p style='font-size:12px;color:#9ca3af;margin:0'>")
         .append("Email automatique — merci de ne pas répondre directement.</p>")
         .append("<p style='font-size:12px;color:#9ca3af;margin:4px 0 0'>© 2026 Talent Bridge</p>")
         .append("</td></tr>");
    }

    private static String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String safe(String v, String fallback) {
        return (v != null && !v.isBlank()) ? v : fallback;
    }

    /** Escape a string value for embedding inside a JSON string literal. */
    private static String jsonStr(String v) {
        if (v == null) return "\"\"";
        // Escape backslash first, then quotes, then newlines/tabs
        String escaped = v
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}





