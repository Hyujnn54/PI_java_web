package Services.interview;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

/**
 * Meeting Service - Advanced Feature
 * Generates unique Jitsi Meet links. Jitsi is free, needs no account,
 * works in Tunisia, and opens directly in any browser.
 *
 * Generated link format:
 *   https://meet.jit.si/TalentBridge-Interview-<ID>-<token>
 *
 * The room name is long and random enough that only people who receive
 * the link (via email) can join — effectively private.
 */
public class MeetingService {

    private static final String JITSI_BASE = "https://meet.jit.si/";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate a real, clickable Jitsi Meet URL for an interview.
     * The URL works immediately in any browser — no installation needed.
     *
     * @param interviewId     interview DB id (for traceability)
     * @param scheduledAt     scheduled date/time (used to build unique hash)
     * @param durationMinutes not used in URL, kept for API compatibility
     * @return full https://meet.jit.si/... URL
     */
    public static String generateMeetingLink(Long interviewId,
                                             LocalDateTime scheduledAt,
                                             int durationMinutes) {
        // 12-char alphanumeric random token  →  practically un-guessable
        byte[] bytes = new byte[9];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(bytes)
            .replaceAll("[^a-zA-Z0-9]", "")
            .substring(0, Math.min(12, 12));

        // Short epoch suffix for extra uniqueness
        long epoch = scheduledAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String suffix = Long.toHexString(epoch).substring(4); // 8 hex chars

        // Room name: human-readable prefix + random token
        String room = "TalentBridge-Interview-" + interviewId + "-" + token + suffix;

        return JITSI_BASE + room;
    }

    /** Overload — duration defaults to 60 min (backward compat). */
    public static String generateMeetingLink(Long interviewId, LocalDateTime scheduledAt) {
        return generateMeetingLink(interviewId, scheduledAt, 60);
    }

    /** True if 'now' is within the allowed join window (10 min before start). */
    public static boolean canJoinMeeting(LocalDateTime scheduledAt, LocalDateTime now) {
        return !now.isBefore(scheduledAt.minusMinutes(10));
    }

    /** True if the meeting window has fully expired (duration + 30 min buffer). */
    public static boolean hasMeetingEnded(LocalDateTime scheduledAt,
                                          int durationMinutes,
                                          LocalDateTime now) {
        return now.isAfter(scheduledAt.plusMinutes(durationMinutes + 30));
    }
}
