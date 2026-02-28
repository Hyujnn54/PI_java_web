package Services.application;

import Utils.MyDatabase;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApplicationStatisticsService {

    private static Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Statistics for a single job offer
     */
    public record OfferStatistics(
        Long offerId,
        String offerTitle,
        int totalApplications,
        int submitted,
        int shortlisted,
        int rejected,
        int interview,
        int hired,
        double acceptanceRate
    ) {
        public int getAcceptancePercentage() {
            return (int) Math.round(acceptanceRate * 100);
        }
    }

    /**
     * Get statistics for all job offers
     */
    public static Map<Long, OfferStatistics> getAllOfferStatistics() {
        Map<Long, OfferStatistics> stats = new LinkedHashMap<>();

        String sql = "SELECT " +
                     "jo.id as offer_id, " +
                     "jo.title as offer_title, " +
                     "COUNT(ja.id) as total_apps, " +
                     "SUM(CASE WHEN ja.current_status = 'SUBMITTED' THEN 1 ELSE 0 END) as submitted_count, " +
                     "SUM(CASE WHEN ja.current_status = 'SHORTLISTED' THEN 1 ELSE 0 END) as shortlisted_count, " +
                     "SUM(CASE WHEN ja.current_status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_count, " +
                     "SUM(CASE WHEN ja.current_status = 'INTERVIEW' THEN 1 ELSE 0 END) as interview_count, " +
                     "SUM(CASE WHEN ja.current_status = 'HIRED' THEN 1 ELSE 0 END) as hired_count " +
                     "FROM job_offer jo " +
                     "LEFT JOIN job_application ja ON jo.id = ja.offer_id AND ja.is_archived = 0 " +
                     "GROUP BY jo.id, jo.title " +
                     "ORDER BY jo.created_at DESC";

        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Long offerId = rs.getLong("offer_id");
                String offerTitle = rs.getString("offer_title");
                int totalApps = rs.getInt("total_apps");
                int submitted = rs.getInt("submitted_count");
                int shortlisted = rs.getInt("shortlisted_count");
                int rejected = rs.getInt("rejected_count");
                int interview = rs.getInt("interview_count");
                int hired = rs.getInt("hired_count");

                // Calculate acceptance rate (shortlisted + hired / total)
                double acceptanceRate = totalApps > 0 ?
                    ((double)(shortlisted + hired) / totalApps) : 0.0;

                stats.put(offerId, new OfferStatistics(
                    offerId,
                    offerTitle,
                    totalApps,
                    submitted,
                    shortlisted,
                    rejected,
                    interview,
                    hired,
                    acceptanceRate
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving statistics: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Get statistics for a single offer
     */
    public static OfferStatistics getOfferStatistics(Long offerId) {
        String sql = "SELECT " +
                     "jo.id as offer_id, " +
                     "jo.title as offer_title, " +
                     "COUNT(ja.id) as total_apps, " +
                     "SUM(CASE WHEN ja.current_status = 'SUBMITTED' THEN 1 ELSE 0 END) as submitted_count, " +
                     "SUM(CASE WHEN ja.current_status = 'SHORTLISTED' THEN 1 ELSE 0 END) as shortlisted_count, " +
                     "SUM(CASE WHEN ja.current_status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_count, " +
                     "SUM(CASE WHEN ja.current_status = 'INTERVIEW' THEN 1 ELSE 0 END) as interview_count, " +
                     "SUM(CASE WHEN ja.current_status = 'HIRED' THEN 1 ELSE 0 END) as hired_count " +
                     "FROM job_offer jo " +
                     "LEFT JOIN job_application ja ON jo.id = ja.offer_id AND ja.is_archived = 0 " +
                     "WHERE jo.id = ? " +
                     "GROUP BY jo.id, jo.title";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int totalApps = rs.getInt("total_apps");
                    int submitted = rs.getInt("submitted_count");
                    int shortlisted = rs.getInt("shortlisted_count");
                    int rejected = rs.getInt("rejected_count");
                    int interview = rs.getInt("interview_count");
                    int hired = rs.getInt("hired_count");

                    double acceptanceRate = totalApps > 0 ?
                        ((double)(shortlisted + hired) / totalApps) : 0.0;

                    return new OfferStatistics(
                        rs.getLong("offer_id"),
                        rs.getString("offer_title"),
                        totalApps,
                        submitted,
                        shortlisted,
                        rejected,
                        interview,
                        hired,
                        acceptanceRate
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving offer statistics: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get global statistics across all offers
     */
    public static OfferStatistics getGlobalStatistics() {
        String sql = "SELECT " +
                     "COUNT(ja.id) as total_apps, " +
                     "SUM(CASE WHEN ja.current_status = 'SUBMITTED' THEN 1 ELSE 0 END) as submitted_count, " +
                     "SUM(CASE WHEN ja.current_status = 'SHORTLISTED' THEN 1 ELSE 0 END) as shortlisted_count, " +
                     "SUM(CASE WHEN ja.current_status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_count, " +
                     "SUM(CASE WHEN ja.current_status = 'INTERVIEW' THEN 1 ELSE 0 END) as interview_count, " +
                     "SUM(CASE WHEN ja.current_status = 'HIRED' THEN 1 ELSE 0 END) as hired_count " +
                     "FROM job_application ja " +
                     "WHERE ja.is_archived = 0";

        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                int totalApps = rs.getInt("total_apps");
                int submitted = rs.getInt("submitted_count");
                int shortlisted = rs.getInt("shortlisted_count");
                int rejected = rs.getInt("rejected_count");
                int interview = rs.getInt("interview_count");
                int hired = rs.getInt("hired_count");

                double acceptanceRate = totalApps > 0 ?
                    ((double)(shortlisted + hired) / totalApps) : 0.0;

                return new OfferStatistics(
                    -1L, // Global stats
                    "All Offers",
                    totalApps,
                    submitted,
                    shortlisted,
                    rejected,
                    interview,
                    hired,
                    acceptanceRate
                );
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving global statistics: " + e.getMessage());
        }

        return null;
    }
}

