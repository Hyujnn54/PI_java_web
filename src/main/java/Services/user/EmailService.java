package Services.user;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.time.LocalDateTime;
import java.util.Properties;

/**
 * User/Auth + Event email service — uses SMTP (Gmail).
 *
 * Handles:
 *   - Welcome email on signup
 *   - Password reset code
 *   - Login success alert
 *   - Event registration status notifications
 *
 * INTERVIEW emails → InterviewEmailService (Brevo API) — do NOT add interview logic here.
 * APPLICATION emails → EmailServiceApplication (SMTP) — do NOT add application logic here.
 */
public class EmailService {

    private static final String FROM_EMAIL   = "talentbridge.app@gmail.com";
    private static final String APP_PASSWORD = "bkeqrffipwtgykdr";

    // ── SMTP session ─────────────────────────────────────────────────────────

    private Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");
        return Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });
    }

    void sendEmail(String toEmail, String subject, String htmlBody) throws Exception {
        MimeMessage msg = new MimeMessage(buildSession());
        msg.setFrom(new InternetAddress(FROM_EMAIL, "Talent Bridge"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject(subject, "UTF-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

        MimeMultipart multipart = new MimeMultipart("mixed");
        multipart.addBodyPart(htmlPart);
        msg.setContent(multipart);
        msg.saveChanges();

        Transport.send(msg);
        System.out.println("[EmailService] Email sent to " + toEmail);
    }

    // ── User / Auth emails ────────────────────────────────────────────────────

    /**
     * Welcome email sent once when a new user registers.
     * Triggered by SignUpController after successful account creation.
     */
    public void sendWelcome(String toEmail, String firstName) throws Exception {
        String name    = safe(firstName, "there");
        String subject = "Welcome to Talent Bridge!";
        String body = buildHtml(
            "Welcome to Talent Bridge! \uD83C\uDF89",
            "Hello " + name + ",",
            "<p style='color:#374151;font-size:15px'>Your account has been created successfully. " +
            "You can now explore job offers, apply for positions, and manage your career all in one place.</p>" +
            "<table style='width:100%;border-collapse:collapse;margin:20px 0'>" +
            row("Role", "Candidate / Recruiter") +
            row("Email", toEmail) +
            row("Date", LocalDateTime.now().toString().substring(0, 16).replace("T", " ")) +
            "</table>" +
            "<div style='text-align:center;margin:24px 0'>" +
            "<div style='display:inline-block;background:linear-gradient(135deg,#5BA3F5,#4A90E2);" +
            "color:white;font-size:15px;font-weight:700;padding:14px 44px;border-radius:50px'>" +
            "Start Exploring</div></div>" +
            "<p style='color:#64748B;font-size:13px'>If you did not create this account, please contact us immediately.</p>"
        );
        sendEmail(toEmail, subject, body);
    }

    /**
     * Password reset — sends a 6-digit code.
     * Code expires in 10 minutes (enforced in PasswordResetService).
     */
    public void sendResetCode(String toEmail, String code) throws Exception {
        String subject = "Talent Bridge — Password Reset Code";
        String body = buildHtml(
            "Password Reset",
            "Reset your password",
            "<p style='color:#374151'>We received a request to reset the password for your Talent Bridge account.</p>" +
            "<p style='color:#374151'>Use the code below. It expires in <strong>10 minutes</strong>.</p>" +
            "<div style='text-align:center;margin:28px 0'>" +
            "<div style='display:inline-block;background:#F0F7FF;border:2px dashed #5BA3F5;" +
            "border-radius:12px;padding:16px 40px'>" +
            "<div style='font-size:36px;font-weight:800;color:#5BA3F5;letter-spacing:10px'>" + code + "</div>" +
            "</div></div>" +
            "<p style='color:#64748B;font-size:13px'>If you did not request a password reset, you can safely ignore this email.</p>"
        );
        sendEmail(toEmail, subject, body);
    }

    /**
     * Login alert — sent after every successful login.
     */
    public void sendLoginSuccess(String toEmail, String firstName) throws Exception {
        String name    = safe(firstName, "User");
        String subject = "Login Detected — Talent Bridge";
        String body = buildHtml(
            "Login Successful",
            "Hello " + name + ",",
            "<p style='color:#374151'>A login was detected on your Talent Bridge account.</p>" +
            "<table style='width:100%;border-collapse:collapse;margin:16px 0'>" +
            row("Time", LocalDateTime.now().toString().substring(0, 16).replace("T", " ")) +
            "</table>" +
            "<p style='color:#64748B;font-size:13px'>If this was not you, please reset your password immediately.</p>"
        );
        sendEmail(toEmail, subject, body);
    }

    // ── Event emails ──────────────────────────────────────────────────────────

    /**
     * Sent when a recruiter changes a candidate's registration status (CONFIRMED / REJECTED / CANCELED).
     */
    public static void sendEventStatusNotification(
            String toEmail, String fullName, String eventTitle,
            String eventDate, String eventLocation, String status,
            String extraNote, String eventType, String meetLink) throws Exception {

        String name = safe(fullName, "Candidat");
        String emoji; String statusLine;
        switch (status != null ? status.toUpperCase() : "") {
            case "CONFIRMED" -> { emoji = "\u2705"; statusLine = "CONFIRMED"; }
            case "REJECTED"  -> { emoji = "\u274C"; statusLine = "REJECTED";  }
            case "CANCELED"  -> { emoji = "\uD83D\uDEAB"; statusLine = "CANCELED"; }
            default          -> { emoji = "\u2139\uFE0F"; statusLine = status != null ? status : "UPDATED"; }
        }
        String locationOrLink = (meetLink != null && !meetLink.isBlank())
                ? "<a href='" + esc(meetLink) + "' style='color:#5BA3F5'>Join Meeting</a>"
                : (eventLocation != null ? esc(eventLocation) : "");

        String subject = emoji + " Event Registration " + statusLine + " — " + safe(eventTitle, "Event") + " | Talent Bridge";
        String body = buildHtml(
            "Registration " + statusLine + " " + emoji,
            "Hello " + esc(name) + ",",
            "<p style='color:#374151'>Your registration status has been updated:</p>" +
            "<table style='width:100%;border-collapse:collapse;margin:16px 0'>" +
            row("Event",    eventTitle)  + row("Type",     eventType) +
            row("Date",     eventDate)   + row("Location", locationOrLink) +
            row("Status",   "<strong>" + emoji + " " + statusLine + "</strong>") +
            "</table>" +
            (extraNote != null && !extraNote.isBlank()
                ? "<p style='color:#64748B;font-size:13px'>Note: " + esc(extraNote) + "</p>" : "")
        );
        new EmailService().sendEmail(toEmail, subject, body);
    }

    /**
     * Sent immediately when a candidate registers for an event (status = PENDING / awaiting confirmation).
     */
    public static void sendEventRegistrationConfirmation(
            String toEmail, String fullName, String eventTitle,
            String eventDate, String location, String eventType) throws Exception {

        String name    = safe(fullName, "Candidat");
        String subject = "\u2705 Registration Received — " + safe(eventTitle, "Event") + " | Talent Bridge";
        String body = buildHtml(
            "Registration Received \u2705",
            "Hello " + esc(name) + ",",
            "<p style='color:#374151'>Your registration has been received and is <strong>pending confirmation</strong>.</p>" +
            "<table style='width:100%;border-collapse:collapse;margin:16px 0'>" +
            row("Event",    eventTitle) + row("Type",     eventType) +
            row("Date",     eventDate)  + row("Location", location) +
            "</table>" +
            "<p style='color:#64748B;font-size:13px'>You will be notified once the recruiter confirms your registration.</p>"
        );
        new EmailService().sendEmail(toEmail, subject, body);
    }

    // ── HTML helpers ──────────────────────────────────────────────────────────

    private static String buildHtml(String title, String greeting, String content) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
               "<body style='margin:0;padding:0;background:#EBF0F8;font-family:Arial,sans-serif'>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='background:#EBF0F8;padding:30px 0'><tr><td align='center'>" +
               "<table width='560' cellpadding='0' cellspacing='0' style='max-width:560px;width:100%;background:white;" +
               "border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(91,163,245,0.15)'>" +
               "<tr><td style='background:linear-gradient(135deg,#1E293B,#334155);padding:28px 32px;text-align:center'>" +
               "<span style='font-size:22px;font-weight:800;color:white'>\uD83C\uDF09 Talent Bridge</span>" +
               "</td></tr>" +
               "<tr><td style='padding:32px'>" +
               "<h2 style='color:#1E293B;margin:0 0 4px 0;font-size:20px'>" + title + "</h2>" +
               "<p style='color:#5BA3F5;font-weight:600;margin:0 0 20px 0;font-size:14px'>" + greeting + "</p>" +
               content +
               "<hr style='border:none;border-top:1px solid #E4EBF5;margin:24px 0'/>" +
               "<p style='color:#94A3B8;font-size:12px;text-align:center;margin:0'>" +
               "This is an automated message — please do not reply directly.<br>" +
               "\u00A9 2026 Talent Bridge. All rights reserved.</p>" +
               "</td></tr></table></td></tr></table></body></html>";
    }

    private static String row(String label, String value) {
        return "<tr style='border-bottom:1px solid #F1F5F9'>" +
               "<td style='padding:9px 12px;font-weight:700;color:#64748B;font-size:13px;width:120px'>" + esc(label) + "</td>" +
               "<td style='padding:9px 12px;color:#1E293B;font-size:13px'>" + (value != null ? value : "\u2014") + "</td></tr>";
    }

    private static String safe(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }

    private static String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}

