package test;

import Models.Interview;
import Services.SMSService;

import java.time.LocalDateTime;

/**
 * Test class for SMS Service
 * Run this to test SMS functionality before integrating into main app
 */
public class TestSMSService {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("📱 TESTING SMS SERVICE - TALENT BRIDGE");
        System.out.println("=".repeat(60));
        System.out.println();

        // Test 1: Simple test SMS
        System.out.println("🧪 Test 1: Sending simple test SMS...");
        SMSService.sendTestSMS();
        System.out.println();

        // Wait a bit
        sleep(2000);

        // Test 2: Interview reminder SMS
        System.out.println("🧪 Test 2: Sending interview reminder SMS...");
        Interview testInterview = createTestInterview();
        SMSService.sendInterviewReminder(testInterview, "+33612345678"); // Replace with your phone
        System.out.println();

        // Wait a bit
        sleep(2000);

        // Test 3: Status update SMS
        System.out.println("🧪 Test 3: Sending interview status update SMS...");
        SMSService.sendInterviewStatusUpdate("+33612345678", "ACCEPTED"); // Replace with your phone
        System.out.println();

        // Wait a bit
        sleep(2000);

        // Test 4: Application status SMS
        System.out.println("🧪 Test 4: Sending application status update SMS...");
        SMSService.sendApplicationStatusUpdate("+33612345678", "Développeur Java Senior", "En cours d'évaluation");
        System.out.println();

        // Test 5: Phone number validation
        System.out.println("🧪 Test 5: Testing phone number validation...");
        testPhoneValidation();
        System.out.println();

        System.out.println("=".repeat(60));
        System.out.println("✅ ALL TESTS COMPLETED!");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("💡 Check your phone for messages (if SMS was enabled)");
        System.out.println("💡 If you see 'SIMULATION MODE', configure sms.properties");
    }

    /**
     * Create a sample interview for testing
     */
    private static Interview createTestInterview() {
        Interview interview = new Interview();
        interview.setId(999L);
        interview.setScheduledAt(LocalDateTime.now().plusDays(1).withHour(14).withMinute(30));
        interview.setDurationMinutes(45);
        interview.setMode("ONLINE");
        interview.setMeetingLink("https://meet.google.com/abc-def-ghi");
        interview.setLocation(null);
        interview.setNotes("N'oubliez pas de préparer votre CV et vos projets");
        return interview;
    }

    /**
     * Test phone number validation
     */
    private static void testPhoneValidation() {
        String[] testNumbers = {
            "+33612345678",      // ✅ Valid (France)
            "+14155551234",      // ✅ Valid (US)
            "+212612345678",     // ✅ Valid (Morocco)
            "0612345678",        // ❌ Invalid (missing +)
            "33612345678",       // ❌ Invalid (missing +)
            "+33 6 12 34 56 78", // ❌ Invalid (has spaces)
            "+123",              // ❌ Invalid (too short)
            "invalid"            // ❌ Invalid
        };

        System.out.println("Testing phone number validation:");
        for (String number : testNumbers) {
            boolean isValid = SMSService.isValidPhoneNumber(number);
            String emoji = isValid ? "✅" : "❌";
            System.out.println(emoji + " " + number + " → " + (isValid ? "Valid" : "Invalid"));
        }
    }

    /**
     * Helper method to pause between tests
     */
    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

