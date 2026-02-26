package Services;

import Models.Interview;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * SMS Service for sending interview reminders via Twilio
 * Configuration loaded from sms.properties file
 */
public class SMSService {

    private static final Properties smsConfig = loadSMSConfiguration();

    // Configuration loaded from properties file
    private static final String ACCOUNT_SID = smsConfig.getProperty("twilio.account.sid", "");
    private static final String AUTH_TOKEN = smsConfig.getProperty("twilio.auth.token", "");
    private static final String TWILIO_PHONE_NUMBER = smsConfig.getProperty("twilio.phone.number", "");
    private static final String TEST_PHONE_NUMBER = smsConfig.getProperty("twilio.test.recipient", "");
    private static final boolean SMS_ENABLED = Boolean.parseBoolean(smsConfig.getProperty("twilio.enabled", "false"));

    private static boolean initialized = false;

    /**
     * Load SMS configuration from properties file
     */
    private static Properties loadSMSConfiguration() {
        Properties props = new Properties();

        try (InputStream input = SMSService.class.getClassLoader()
                .getResourceAsStream("sms.properties")) {

            if (input == null) {
                System.out.println("⚠️  sms.properties not found. SMS service running in SIMULATION MODE.");
                System.out.println("💡 Tip: Copy sms.properties.template to sms.properties and configure your Twilio credentials.");
                return props; // Return empty properties, will use defaults
            }

            props.load(input);
            System.out.println("✅ SMS configuration loaded successfully from sms.properties");

            // Debug: Check if credentials are configured
            String accountSid = props.getProperty("twilio.account.sid", "");
            String authToken = props.getProperty("twilio.auth.token", "");
            boolean enabled = Boolean.parseBoolean(props.getProperty("twilio.enabled", "false"));

            if (accountSid.isEmpty() || authToken.isEmpty() || accountSid.contains("YOUR_")) {
                System.out.println("⚠️  Twilio credentials not configured - running in SIMULATION MODE");
            } else if (!enabled) {
                System.out.println("⚠️  SMS sending is DISABLED (twilio.enabled=false) - SIMULATION MODE");
            } else {
                System.out.println("✅ Twilio credentials configured - REAL SMS MODE");
            }

        } catch (IOException e) {
            System.err.println("❌ Error loading SMS configuration: " + e.getMessage());
            System.out.println("💡 Using default values (simulation mode).");
        }

        return props;
    }

    /**
     * Initialize Twilio client
     */
    private static void initializeTwilio() {
        if (!initialized && SMS_ENABLED && !ACCOUNT_SID.isEmpty() && !AUTH_TOKEN.isEmpty()) {
            try {
                Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
                initialized = true;
                System.out.println("✅ Twilio client initialized successfully");
            } catch (Exception e) {
                System.err.println("❌ Failed to initialize Twilio: " + e.getMessage());
            }
        }
    }

    /**
     * Send an SMS reminder for an upcoming interview
     *
     * @param interview The interview details
     * @param recipientPhone The recipient's phone number (format: +1234567890)
     */
    public static void sendInterviewReminder(Interview interview, String recipientPhone) {
        try {
            String messageBody = buildReminderSMSBody(interview);
            sendSMS(recipientPhone, messageBody);
            System.out.println("📱 SMS reminder sent successfully to: " + recipientPhone);
        } catch (Exception e) {
            System.err.println("❌ Failed to send SMS reminder: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Build the SMS body for interview reminder
     */
    private static String buildReminderSMSBody(Interview interview) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

        StringBuilder body = new StringBuilder();
        body.append("🎯 RAPPEL ENTRETIEN - Talent Bridge\n\n");
        body.append("📅 ").append(interview.getScheduledAt().format(formatter)).append("\n");
        body.append("⏱️ Durée: ").append(interview.getDurationMinutes()).append(" min\n");
        body.append("📍 Mode: ").append(interview.getMode()).append("\n");

        if ("ONLINE".equals(interview.getMode()) && interview.getMeetingLink() != null) {
            body.append("🔗 Lien: ").append(interview.getMeetingLink()).append("\n");
        } else if ("ON_SITE".equals(interview.getMode()) && interview.getLocation() != null) {
            body.append("📍 Lieu: ").append(interview.getLocation()).append("\n");
        }

        body.append("\nBonne chance! 🍀");

        return body.toString();
    }

    /**
     * Send an SMS using Twilio
     *
     * @param toPhoneNumber Recipient phone number (format: +1234567890)
     * @param messageBody SMS message content
     */
    private static void sendSMS(String toPhoneNumber, String messageBody) {
        // Simulation mode
        if (!SMS_ENABLED || ACCOUNT_SID.isEmpty() || AUTH_TOKEN.isEmpty() || ACCOUNT_SID.contains("YOUR_")) {
            System.out.println("\n=== SMS SIMULATION MODE ===");
            System.out.println("⚠️  No SMS sent - Twilio not configured or disabled!");
            System.out.println("To: " + toPhoneNumber);
            System.out.println("Message:\n" + messageBody);
            System.out.println("============================\n");
            System.out.println("💡 To send real SMS:");
            System.out.println("   1. Sign up for Twilio: https://www.twilio.com/try-twilio");
            System.out.println("   2. Copy sms.properties.template to sms.properties");
            System.out.println("   3. Configure your Twilio credentials");
            System.out.println("   4. Set twilio.enabled=true");
            return;
        }

        // Real SMS sending mode
        try {
            initializeTwilio();

            System.out.println("📱 Sending real SMS via Twilio to: " + toPhoneNumber);

            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(TWILIO_PHONE_NUMBER),
                    messageBody
            ).create();

            System.out.println("✅ SMS sent successfully! Message SID: " + message.getSid());
            System.out.println("📊 Status: " + message.getStatus());

        } catch (Exception e) {
            System.err.println("❌ Failed to send SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send interview status update SMS
     *
     * @param recipientPhone Recipient phone number
     * @param status Interview status (ACCEPTED/REJECTED/PENDING)
     */
    public static void sendInterviewStatusUpdate(String recipientPhone, String status) {
        try {
            String messageBody;
            switch (status.toUpperCase()) {
                case "ACCEPTED":
                    messageBody = "✅ Talent Bridge: Votre entretien a été ACCEPTÉ! Vous recevrez bientôt plus de détails.";
                    break;
                case "REJECTED":
                    messageBody = "❌ Talent Bridge: Malheureusement, votre candidature n'a pas été retenue cette fois.";
                    break;
                case "PENDING":
                    messageBody = "⏳ Talent Bridge: Votre entretien est en cours d'évaluation. Nous vous tiendrons informé.";
                    break;
                default:
                    messageBody = "🎯 Talent Bridge: Mise à jour du statut de votre entretien: " + status;
            }

            sendSMS(recipientPhone, messageBody);
            System.out.println("📱 Status update SMS sent successfully to: " + recipientPhone);
        } catch (Exception e) {
            System.err.println("❌ Failed to send status update SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send application status update SMS
     *
     * @param recipientPhone Recipient phone number
     * @param jobTitle Job title
     * @param status Application status
     */
    public static void sendApplicationStatusUpdate(String recipientPhone, String jobTitle, String status) {
        try {
            String messageBody = String.format(
                    "🎯 Talent Bridge\n\nMise à jour de votre candidature pour: %s\n\nStatut: %s\n\nConsultez votre compte pour plus de détails.",
                    jobTitle, status
            );

            sendSMS(recipientPhone, messageBody);
            System.out.println("📱 Application status SMS sent successfully to: " + recipientPhone);
        } catch (Exception e) {
            System.err.println("❌ Failed to send application status SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test method to send a test SMS immediately
     */
    public static void sendTestSMS() {
        try {
            String messageBody = "🎯 Test SMS - Talent Bridge\n\n" +
                                "Si vous recevez ce message, le service SMS fonctionne correctement!\n\n" +
                                "Cordialement,\nL'équipe Talent Bridge";

            sendSMS(TEST_PHONE_NUMBER, messageBody);
            System.out.println("✅ Test SMS sent successfully!");
        } catch (Exception e) {
            System.err.println("❌ Failed to send test SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Validate phone number format
     *
     * @param phoneNumber Phone number to validate
     * @return true if valid format (+1234567890)
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        // Basic validation: starts with + and has 10-15 digits
        return phoneNumber.matches("^\\+[1-9]\\d{9,14}$");
    }
}

