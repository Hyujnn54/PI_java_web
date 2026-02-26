package Services;

import Models.Interview;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * SMS Service — sends reminders via SMSMobileAPI.
 *
 * SENDER  : always 53757969 (the phone that has SMSMobileAPI app installed,
 *           tied to the API key — cannot be changed in code)
 * RECIPIENT: taken from users.phone in the database
 */
public class SMSService {

    private static final Properties smsConfig = loadSMSConfiguration();

    private static final String  API_URL     = smsConfig.getProperty("sms.api.url", "");
    private static final String  API_KEY     = smsConfig.getProperty("sms.api.key", "");
    private static final boolean SMS_ENABLED = Boolean.parseBoolean(smsConfig.getProperty("sms.enabled", "false"));

    // -------------------------------------------------------------------------
    // Config loader
    // -------------------------------------------------------------------------

    private static Properties loadSMSConfiguration() {
        Properties props = new Properties();
        try (InputStream in = SMSService.class.getClassLoader().getResourceAsStream("sms.properties")) {
            if (in == null) {
                System.out.println("[SMSService] sms.properties not found — SIMULATION MODE.");
                return props;
            }
            props.load(in);
            boolean enabled = Boolean.parseBoolean(props.getProperty("sms.enabled", "false"));
            String key = props.getProperty("sms.api.key", "");
            if (!enabled || key.isBlank()) {
                System.out.println("[SMSService] SMS disabled or API key missing — SIMULATION MODE.");
            } else {
                System.out.println("[SMSService] SMS configured. FROM: 53757969 (API key owner).");
            }
        } catch (IOException e) {
            System.err.println("[SMSService] Failed to load sms.properties: " + e.getMessage());
        }
        return props;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Send an interview reminder SMS.
     *
     * @param interview      interview details
     * @param recipientPhone candidate phone from users.phone (any format)
     * @param candidateName  candidate full name from users table
     */
    public static void sendInterviewReminder(Interview interview,
                                             String recipientPhone,
                                             String candidateName) {
        String message = buildMessage(interview, candidateName);
        sendSMS(recipientPhone, message);
    }

    /** Overload without name. */
    public static void sendInterviewReminder(Interview interview, String recipientPhone) {
        sendInterviewReminder(interview, recipientPhone, "");
    }

    /**
     * DB-based test: takes the first upcoming interview from the DB,
     * reads the candidate's real phone from users.phone, and sends.
     */
    public static void sendTestFromDatabase() {
        System.out.println("[SMSService] Looking for upcoming interview in DB...");
        try {
            java.util.List<Interview> interviews = InterviewService.getAll();
            Interview target = interviews.stream()
                .filter(i -> i.getId() != null && i.getScheduledAt() != null
                          && i.getScheduledAt().isAfter(LocalDateTime.now().minusHours(1)))
                .findFirst().orElse(null);

            if (target == null) {
                System.out.println("[SMSService] No upcoming interviews in DB.");
                return;
            }

            String[] contact = getCandidateContact(target.getApplicationId());
            String phone = contact != null ? contact[0] : null;
            String name  = contact != null ? contact[1] : "";

            if (phone == null || phone.isBlank()) {
                System.out.println("[SMSService] No phone for interview #" + target.getId());
                return;
            }

            System.out.println("[SMSService] Test: sending to " + phone + " (" + name + ")");
            sendInterviewReminder(target, phone, name);

        } catch (Exception e) {
            System.err.println("[SMSService] DB test error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Core send
    // -------------------------------------------------------------------------

    /**
     * Send SMS via SMSMobileAPI (POST).
     *
     * FROM : 53757969 — fixed by the API key, cannot be changed here
     * TO   : toPhoneNumber from DB — stripped to digits only
     */
    private static void sendSMS(String toPhoneNumber, String messageBody) {
        if (!SMS_ENABLED || API_URL.isBlank() || API_KEY.isBlank()) {
            System.out.println("[SMSService] SIMULATION — TO: " + toPhoneNumber);
            System.out.println(messageBody);
            return;
        }

        // Strip everything except digits
        String digits = toPhoneNumber.replaceAll("[^0-9]", "");

        // If 8 local Tunisian digits, prepend country code
        if (digits.length() == 8) {
            digits = "216" + digits;
        }

        // API uses recipients= with + prefix (e.g. +21693346608)
        String recipient = "+" + digits;

        System.out.println("[SMSService] FROM: 53757969 → TO: " + recipient);

        try {
            String body = "apikey=" + URLEncoder.encode(API_KEY, StandardCharsets.UTF_8)
                        + "&recipients=" + URLEncoder.encode(recipient, StandardCharsets.UTF_8)
                        + "&message=" + URLEncoder.encode(messageBody, StandardCharsets.UTF_8);

            System.out.println("[SMSService] POST recipients=" + recipient
                    + " message=[" + messageBody.length() + " chars]");

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            String resp = sb.toString();
            System.out.println("[SMSService] HTTP " + code + " — " + resp);

            if (resp.contains("\"error\":0") || resp.contains("\"error\":\"0\"")
                    || resp.contains("\"sent\":\"1\"")) {
                System.out.println("[SMSService] SMS sent successfully to " + digits);
            } else {
                System.err.println("[SMSService] API error — digits sent: " + digits
                    + " — response: " + resp);
            }

        } catch (Exception e) {
            System.err.println("[SMSService] Exception: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Message builder
    // -------------------------------------------------------------------------

    private static String buildMessage(Interview interview, String candidateName) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a' HH:mm");
        String mode = "ONLINE".equals(interview.getMode()) ? "En ligne" : "Sur site";

        StringBuilder b = new StringBuilder();
        b.append("Talent Bridge - Rappel d'entretien\n");
        b.append("----------------------------------\n");
        if (candidateName != null && !candidateName.isBlank()) {
            b.append("Bonjour ").append(candidateName).append(",\n\n");
        }
        b.append("Votre entretien est prevu demain :\n");
        b.append("Date   : ").append(interview.getScheduledAt().format(fmt)).append("\n");
        b.append("Duree  : ").append(interview.getDurationMinutes()).append(" min\n");
        b.append("Format : ").append(mode).append("\n");
        if ("ONLINE".equals(interview.getMode()) && interview.getMeetingLink() != null
                && !interview.getMeetingLink().isBlank()) {
            b.append("Lien   : ").append(interview.getMeetingLink()).append("\n");
        } else if ("ON_SITE".equals(interview.getMode()) && interview.getLocation() != null
                && !interview.getLocation().isBlank()) {
            b.append("Lieu   : ").append(interview.getLocation()).append("\n");
        }
        b.append("----------------------------------\n");
        b.append("Bonne chance !\nTalent Bridge");
        return b.toString();
    }

    // -------------------------------------------------------------------------
    // DB helper
    // -------------------------------------------------------------------------

    /** Returns [phone, fullName] from users table for a given application. */
    private static String[] getCandidateContact(Long applicationId) {
        if (applicationId == null) return null;
        String sql = "SELECT u.phone, u.first_name, u.last_name "
                   + "FROM job_application ja "
                   + "JOIN users u ON ja.candidate_id = u.id "
                   + "WHERE ja.id = ?";
        try {
            java.sql.Connection conn = Utils.MyDatabase.getInstance().getConnection();
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, applicationId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String phone = rs.getString("phone");
                        String fn    = rs.getString("first_name");
                        String ln    = rs.getString("last_name");
                        String name  = ((fn != null ? fn : "") + " " + (ln != null ? ln : "")).trim();
                        return new String[]{phone, name.isEmpty() ? "Candidat" : name};
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SMSService] DB error: " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Phone utils
    // -------------------------------------------------------------------------

    /** Validates a phone number (any format — will be normalized on send). */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return false;
        String digits = phone.replaceAll("[^0-9]", "");
        // Accept 8-digit local TN, or 11-digit international (216XXXXXXXX)
        return digits.length() == 8 || digits.length() >= 10;
    }

    /** Normalizes to +216XXXXXXXX format for display/logging. */
    public static String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 8) return "+216" + digits;
        if (digits.startsWith("216") && digits.length() == 11) return "+" + digits;
        if (digits.startsWith("00216")) return "+" + digits.substring(2);
        return phone.trim();
    }
}

