package Services.user;

import Models.interview.Interview;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.util.Properties;

public class EmailService {

    // ✅ Gmail account
    private static final String FROM_EMAIL = "talentbridge.app@gmail.com";

    // ✅ Google App Password (16 chars)
    private static final String APP_PASSWORD = "bkeqrffipwtgykdr";

    private Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        // optional debug
        // props.put("mail.debug", "true");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });
    }

    // ✅ the missing method (your error is here)
    void sendEmail(String toEmail, String subject, String body) throws Exception {
        Message msg = new MimeMessage(buildSession());

        msg.setFrom(new InternetAddress(FROM_EMAIL, "TalentBridge"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject(subject);
        msg.setText(body);

        Transport.send(msg);
        System.out.println("✅ Email sent to " + toEmail);
    }

    public void sendResetCode(String toEmail, String code) throws Exception {
        String subject = "TalentBridge - Password Reset Code";
        String body = "Your password reset code is: " + code + "\n\n"
                + "This code expires in 10 minutes.\n"
                + "If you didn’t request this, ignore this email.";

        sendEmail(toEmail, subject, body);
    }

    public void sendLoginSuccess(String toEmail, String firstName) throws Exception {
        String subject = "✅ Login Successful - TalentBridge";
        String name = (firstName == null || firstName.isBlank()) ? "User" : firstName;

        String body = """
                Hello %s,

                You have successfully logged in to TalentBridge.
                Time: %s

                If this wasn't you, please change your password immediately.

                - TalentBridge
                """.formatted(name, LocalDateTime.now());

        sendEmail(toEmail, subject, body);
    }

    public static void sendAcceptanceNotification(
            String toEmail, String fullName, String jobTitle,
            String location, String contractType, String description) throws Exception {
        String subject = "🎉 Congratulations! Your application has been accepted - TalentBridge";
        String name = (fullName == null || fullName.isBlank()) ? "Candidate" : fullName;
        String body = """
                Dear %s,

                We are pleased to inform you that your application for the following position has been ACCEPTED:

                Position  : %s
                Location  : %s
                Contract  : %s
                Details   : %s

                Our team will be in touch shortly with next steps.

                Best regards,
                TalentBridge Team
                """.formatted(
                name,
                jobTitle   != null ? jobTitle   : "N/A",
                location   != null ? location   : "N/A",
                contractType != null ? contractType : "N/A",
                description != null ? description.substring(0, Math.min(200, description.length())) : "N/A"
        );
        new EmailService().sendEmail(toEmail, subject, body);
    }

    /**
     * Sends a 24-hour interview reminder to the candidate.
     * Called statically by InterviewReminderScheduler.
     */
    public static void sendInterviewReminder(
            Interview interview, String toEmail, String candidateName) throws Exception {

        String name = (candidateName == null || candidateName.isBlank()) ? "Candidat" : candidateName;
        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
        String when = interview.getScheduledAt() != null
                ? interview.getScheduledAt().format(fmt) : "N/A";
        String mode = interview.getMode() != null ? interview.getMode() : "N/A";
        String link = interview.getMeetingLink();
        String location = interview.getLocation();

        String subject = "⏰ Rappel : Entretien demain — TalentBridge";
        String body = """
                Bonjour %s,

                Ceci est un rappel : vous avez un entretien prévu demain.

                📅 Date/Heure : %s
                💼 Mode       : %s
                %s

                Préparez-vous bien et bonne chance !

                — TalentBridge
                """.formatted(
                name,
                when,
                mode,
                (link != null && !link.isBlank())
                        ? "🔗 Lien       : " + link
                        : (location != null && !location.isBlank()
                                ? "📍 Lieu       : " + location
                                : "")
        );

        new EmailService().sendEmail(toEmail, subject, body);
    }

    /**
     * Sends an event registration status change notification to the candidate.
     * status = "CONFIRMED", "REJECTED", "CANCELED", etc.
     * extraNote may be null.
     */
    public static void sendEventStatusNotification(
            String toEmail, String fullName, String eventTitle,
            String eventDate, String eventLocation, String status,
            String extraNote, String eventType, String meetLink) throws Exception {

        String name = (fullName == null || fullName.isBlank()) ? "Candidat" : fullName;

        String statusLine;
        String emoji;
        switch (status != null ? status.toUpperCase() : "") {
            case "CONFIRMED" -> { emoji = "✅"; statusLine = "CONFIRMÉE"; }
            case "REJECTED"  -> { emoji = "❌"; statusLine = "REFUSÉE";   }
            case "CANCELED"  -> { emoji = "🚫"; statusLine = "ANNULÉE";   }
            default          -> { emoji = "ℹ️";  statusLine = status != null ? status : "MISE À JOUR"; }
        }

        String subject = emoji + " Inscription " + statusLine + " — "
                + (eventTitle != null ? eventTitle : "Événement") + " | TalentBridge";

        String locationOrLink = (meetLink != null && !meetLink.isBlank())
                ? "🔗 Lien : " + meetLink
                : (eventLocation != null && !eventLocation.isBlank()
                        ? "📍 Lieu : " + eventLocation
                        : "");

        String extra = (extraNote != null && !extraNote.isBlank())
                ? "\nRemarque : " + extraNote : "";

        String body = """
                Bonjour %s,

                Le statut de votre inscription à l'événement suivant a été mis à jour :

                📌 Événement  : %s
                🗂️  Type        : %s
                📅 Date/Heure : %s
                %s
                🔔 Statut      : %s %s
                %s
                Pour toute question, contactez l'équipe TalentBridge.

                — TalentBridge
                """.formatted(
                name,
                eventTitle   != null ? eventTitle   : "N/A",
                eventType    != null ? eventType     : "N/A",
                eventDate    != null ? eventDate     : "N/A",
                locationOrLink,
                emoji, statusLine,
                extra
        );

        new EmailService().sendEmail(toEmail, subject, body);
    }

    public static void sendEventRegistrationConfirmation(
            String toEmail, String fullName, String eventTitle,
            String eventDate, String location, String eventType) throws Exception {

        String name = (fullName == null || fullName.isBlank()) ? "Candidat" : fullName;
        String subject = "✅ Inscription enregistrée — "
                + (eventTitle != null ? eventTitle : "Événement") + " | TalentBridge";
        String body = """
                Bonjour %s,

                Votre inscription à l'événement suivant a bien été enregistrée et est en attente de confirmation :

                📌 Événement  : %s
                🗂️  Type        : %s
                📅 Date/Heure : %s
                📍 Lieu        : %s

                Vous recevrez une confirmation dès que le recruteur aura validé votre inscription.

                Merci de votre intérêt et bonne chance !

                — TalentBridge
                """.formatted(
                name,
                eventTitle != null ? eventTitle : "N/A",
                eventType  != null ? eventType  : "N/A",
                eventDate  != null ? eventDate  : "N/A",
                location   != null && !location.isBlank() ? location : "En ligne / À définir"
        );
        new EmailService().sendEmail(toEmail, subject, body);
    }
}