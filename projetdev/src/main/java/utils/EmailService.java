package utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    private final String username = "rayanbenamor207@gmail.com";
    private final String password = "cbct gwko ndnz pwbd"; // App Password

    public void sendStatusUpdateEmail(entities.EventRegistration registration) {
        String toEmail = registration.getEmail();
        String eventTitle = registration.getEvent() != null ? registration.getEvent().getTitle() : "votre événement";
        String userName = registration.getFirstName() + " " + registration.getLastName();
        entities.AttendanceStatusEnum status = registration.getAttendanceStatus();

        if (toEmail == null || toEmail.isEmpty()) {
            System.err.println("Cannot send email: recipient address is null or empty.");
            return;
        }

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.ssl.protocols", "TLSv1.2");

        System.out.println("EmailService: Preparing email message for " + toEmail);

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(InternetAddress.parse(username)[0]);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            
            String subject = "";
            StringBuilder content = new StringBuilder();
            content.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;'>");
            content.append("<h2 style='color: #2563eb;'>Talent Bridge - Mise à jour d'inscription</h2>");
            content.append("<p>Bonjour <strong>").append(userName).append("</strong>,</p>");

            switch (status) {
                case CONFIRMED:
                    subject = "✅ Confirmation d'inscription : " + eventTitle;
                    content.append("<p>Nous avons le plaisir de vous informer que votre inscription à l'événement <strong>\"").append(eventTitle).append("\"</strong> a été <strong>confirmée</strong>.</p>");
                    
                    boolean isWebinar = registration.getEvent() != null && "WEBINAIRE".equalsIgnoreCase(registration.getEvent().getEventType());
                    String meetLink = registration.getEvent() != null ? registration.getEvent().getMeetLink() : null;

                    if (isWebinar) {
                        content.append("<div style='text-align: center; margin: 30px 0; padding: 20px; background-color: #f0fdf4; border: 2px dashed #22c55e; border-radius: 8px;'>");
                        content.append("<p style='margin-bottom: 10px; color: #166534; font-weight: bold;'>C'est un Webinaire En Ligne !</p>");
                        if (meetLink != null && !meetLink.isEmpty()) {
                            content.append("<p style='margin-bottom: 15px; color: #15803d;'>Rejoignez-nous via le lien ci-dessous :</p>");
                            content.append("<a href='").append(meetLink).append("' style='display: inline-block; padding: 12px 25px; background-color: #22c55e; color: white; text-decoration: none; border-radius: 6px; font-weight: bold;'>Accéder à la Réunion</a>");
                            content.append("<p style='font-size: 12px; color: #15803d; margin-top: 15px;'>Lien direct : ").append(meetLink).append("</p>");
                        } else {
                            content.append("<p style='margin-bottom: 15px; color: #ef4444;'>Le lien de la réunion vous sera envoyé très prochainement par un autre email.</p>");
                        }
                        content.append("</div>");
                    } else {
                        content.append("<div style='text-align: center; margin: 30px 0; padding: 15px; background-color: #f8fafc; border-radius: 8px;'>");
                        content.append("<p style='margin-bottom: 10px; color: #64748b;'>Votre pass d'accès personnel :</p>");
                        // Generate QR Code URL
                        String qrData = "REG-" + registration.getId() + "-" + registration.getCandidateId();
                        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + qrData;
                        content.append("<img src='").append(qrUrl).append("' alt='QR Code Pass' style='border: 4px solid white; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);' />");
                        content.append("<p style='font-size: 12px; color: #94a3b8; margin-top: 10px;'>Veuillez présenter ce code à l'entrée.</p>");
                        content.append("</div>");
                    }
                    break;
                case CANCELLED:
                    subject = "❌ Annulation d'inscription : " + eventTitle;
                    content.append("<p>Nous vous informons malheureusement que votre inscription à l'événement <strong>\"").append(eventTitle).append("\"</strong> a été <strong>annulée</strong>.</p>");
                    content.append("<p>N'hésitez pas à consulter d'autres événements sur notre plateforme.</p>");
                    break;
                case ATTENDED:
                    subject = "⭐ Merci de votre participation : " + eventTitle;
                    content.append("<p>Merci d'avoir participé à l'événement <strong>\"").append(eventTitle).append("\"</strong> ! Nous espérons que cela vous a été utile.</p>");
                    break;
                case NO_SHOW:
                    subject = "⏳ Absence constatée : " + eventTitle;
                    content.append("<p>Nous avons constaté votre absence à l'événement <strong>\"").append(eventTitle).append("\"</strong>.</p>");
                    break;
                default:
                    subject = "Mise à jour d'inscription : " + eventTitle;
                    content.append("<p>Le statut de votre inscription à l'événement <strong>\"").append(eventTitle).append("\"</strong> est maintenant : <strong>").append(status).append("</strong>.</p>");
            }

            content.append("<br/><p>Cordialement,<br/><strong>L'équipe Talent Bridge</strong></p>");
            content.append("</div>");

            message.setSubject(subject);
            message.setContent(content.toString(), "text/html; charset=UTF-8");

            Thread mailThread = new Thread(() -> {
                try {
                    System.out.println("EmailService: Starting background Send task...");
                    Transport.send(message);
                    System.out.println("EmailService: Email sent successfully to " + toEmail + " for status " + status);
                } catch (MessagingException e) {
                    System.err.println("EmailService Error: Background email task failed: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            mailThread.start();

        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Failed to prepare email: " + e.getMessage());
        }
    }
}
