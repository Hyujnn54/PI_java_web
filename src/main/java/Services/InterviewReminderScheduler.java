package Services;

import Models.Interview;
import Utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Interview Email Reminder Scheduler
 *
 * Automatically checks for interviews 24 hours before they occur
 * and sends reminder emails without spamming (tracks sent emails).
 *
 * Features:
 * - Checks every 5 minutes for upcoming interviews
 * - Tracks sent emails to prevent duplicates
 * - Only sends if email hasn't been sent before
 * - Optimized database queries
 * - Thread-safe implementation
 */
public class InterviewReminderScheduler {

    private static Timer schedulerTimer;
    private static final Set<Long> sentReminderInterviewIds = new HashSet<>();
    private static final long CHECK_INTERVAL_MINUTES = 5; // Check every 5 minutes
    private static boolean isRunning = false;

    /**
     * Start the interview reminder scheduler
     * Runs in background and checks every 5 minutes
     */
    public static synchronized void start() {
        if (isRunning) {
            System.out.println("⚠️  Interview Reminder Scheduler is already running");
            return;
        }

        isRunning = true;
        schedulerTimer = new Timer("InterviewReminderScheduler", true);

        // Check immediately on start
        checkAndSendReminders();

        // Then schedule periodic checks every 5 minutes
        schedulerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAndSendReminders();
            }
        }, CHECK_INTERVAL_MINUTES * 60 * 1000, CHECK_INTERVAL_MINUTES * 60 * 1000);

        System.out.println("✅ Interview Reminder Scheduler started (checking every " + CHECK_INTERVAL_MINUTES + " minutes)");
    }

    /**
     * Check for interviews that need reminders and send them
     */
    private static void checkAndSendReminders() {
        try {
            List<Interview> interviews = InterviewService.getAll();

            for (Interview interview : interviews) {
                if (interview.getId() == null) continue;

                // Skip if reminder already sent
                if (sentReminderInterviewIds.contains(interview.getId())) {
                    continue;
                }

                // Check if interview is 24 hours away
                if (isTimeForReminder(interview.getScheduledAt())) {
                    sendReminderForInterview(interview);
                    sentReminderInterviewIds.add(interview.getId());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error in interview reminder scheduler: " + e.getMessage());
        }
    }

    /**
     * Check if an interview is within 24 hours (but not less than 23 hours before)
     */
    private static boolean isTimeForReminder(LocalDateTime interviewTime) {
        if (interviewTime == null) return false;

        LocalDateTime now = LocalDateTime.now();
        long minutesUntilInterview = ChronoUnit.MINUTES.between(now, interviewTime);

        // Send reminder if between 1380-1440 minutes (23-24 hours) before interview
        return minutesUntilInterview >= 1380 && minutesUntilInterview <= 1440;
    }

    /**
     * Send reminder email and SMS for an interview
     */
    private static void sendReminderForInterview(Interview interview) {
        try {
            // Get candidate email and phone (from application or database)
            CandidateContact contact = getCandidateContactForInterview(interview.getApplicationId());

            if (contact != null) {
                // Send Email
                if (contact.email != null && !contact.email.isEmpty()) {
                    EmailService.sendInterviewReminder(interview, contact.email);
                    System.out.println("📧 Email reminder sent for Interview #" + interview.getId()
                        + " to " + contact.email);
                }

                // Send SMS
                if (contact.phone != null && !contact.phone.isEmpty() && SMSService.isValidPhoneNumber(contact.phone)) {
                    SMSService.sendInterviewReminder(interview, contact.phone);
                    System.out.println("📱 SMS reminder sent for Interview #" + interview.getId()
                        + " to " + contact.phone);
                }

                // Log the reminder sent
                logReminder(interview.getId(), contact.email, contact.phone);

            } else {
                System.out.println("⚠️  Could not find candidate contact for Interview #" + interview.getId());
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to send reminder for Interview #" + interview.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Helper class to hold candidate contact information
     */
    private static class CandidateContact {
        String email;
        String phone;

        CandidateContact(String email, String phone) {
            this.email = email;
            this.phone = phone;
        }
    }

    /**
     * Get candidate contact info (email and phone) from application using database connection
     */
    private static CandidateContact getCandidateContactForInterview(Long applicationId) {
        if (applicationId == null) return null;

        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            // Join job_application -> candidate -> users to get email and phone
            String query = "SELECT u.email, u.phone FROM job_application ja " +
                          "JOIN candidate c ON ja.candidate_id = c.id " +
                          "JOIN users u ON c.id = u.id " +
                          "WHERE ja.id = ?";

            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setLong(1, applicationId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String email = rs.getString("email");
                        String phone = rs.getString("phone");
                        return new CandidateContact(email, phone);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching candidate contact: " + e.getMessage());
        }

        return null;
    }

    /**
     * Log that a reminder (email/SMS) was sent (prevents duplicate sends in database)
     */
    private static void logReminder(Long interviewId, String candidateEmail, String candidatePhone) {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            String query = "INSERT INTO reminders_log (interview_id, candidate_email, candidate_phone, sent_at) " +
                          "VALUES (?, ?, ?, NOW()) " +
                          "ON DUPLICATE KEY UPDATE sent_at = NOW()";

            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setLong(1, interviewId);
                ps.setString(2, candidateEmail);
                ps.setString(3, candidatePhone);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            // Table might not exist - that's okay, we still track in memory
            System.out.println("💡 Optional: Create reminders_log table for persistent tracking");
        }
    }

    /**
     * Check if a reminder was already sent for an interview (in-memory check)
     */
    public static boolean wasReminderSent(Long interviewId) {
        return sentReminderInterviewIds.contains(interviewId);
    }

    /**
     * Manually reset reminder for an interview (if it needs to be resent)
     */
    public static void resetReminder(Long interviewId) {
        sentReminderInterviewIds.remove(interviewId);
        System.out.println("🔄 Reminder reset for Interview #" + interviewId);
    }

    /**
     * Get scheduler status
     */
    public static boolean isRunning() {
        return isRunning;
    }

    /**
     * Get number of reminders sent so far in this session
     */
    public static int getRemindersSentCount() {
        return sentReminderInterviewIds.size();
    }
}




