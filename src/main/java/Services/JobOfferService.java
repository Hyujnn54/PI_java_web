package Services;

import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for job_offer table.
 * Read-only operations for browsing job offers.
 */
public class JobOfferService {

    public record JobOfferRow(
        Long id,
        Long recruiterId,
        String title,
        String description,
        String location,
        String contractType,
        LocalDateTime createdAt,
        LocalDateTime deadline,
        String status
    ) {}

    private static Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public static List<JobOfferRow> getAll() {
        List<JobOfferRow> list = new ArrayList<>();
        String sql = "SELECT id, recruiter_id, title, description, location, contract_type, created_at, deadline, status " +
                     "FROM job_offer WHERE status = 'OPEN' ORDER BY created_at DESC";

        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new JobOfferRow(
                        rs.getLong("id"),
                        rs.getLong("recruiter_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("location"),
                        rs.getString("contract_type"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("deadline") != null ? rs.getTimestamp("deadline").toLocalDateTime() : null,
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving job offers: " + e.getMessage());
        }

        return list;
    }

    public static JobOfferRow getById(Long id) {
        String sql = "SELECT id, recruiter_id, title, description, location, contract_type, created_at, deadline, status " +
                     "FROM job_offer WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new JobOfferRow(
                            rs.getLong("id"),
                            rs.getLong("recruiter_id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("location"),
                            rs.getString("contract_type"),
                            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                            rs.getTimestamp("deadline") != null ? rs.getTimestamp("deadline").toLocalDateTime() : null,
                            rs.getString("status")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving job offer by id: " + e.getMessage());
        }
        return null;
    }

    public static void addJobOffer(JobOfferRow jobOffer) {
        String sql = "INSERT INTO job_offer (recruiter_id, title, description, location, contract_type, deadline, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'OPEN')";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, jobOffer.recruiterId());
            ps.setString(2, jobOffer.title());
            ps.setString(3, jobOffer.description());
            ps.setString(4, jobOffer.location());
            ps.setString(5, jobOffer.contractType());
            ps.setTimestamp(6, jobOffer.deadline() != null ? Timestamp.valueOf(jobOffer.deadline()) : null);

            ps.executeUpdate();
            System.out.println("Job offer created successfully");
        } catch (SQLException e) {
            System.err.println("Error adding job offer: " + e.getMessage());
            throw new RuntimeException("Failed to create job offer: " + e.getMessage(), e);
        }
    }

    public static List<JobOfferRow> getByRecruiterId(Long recruiterId) {
        List<JobOfferRow> list = new ArrayList<>();
        String sql = "SELECT id, recruiter_id, title, description, location, contract_type, created_at, deadline, status " +
                     "FROM job_offer WHERE recruiter_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new JobOfferRow(
                            rs.getLong("id"),
                            rs.getLong("recruiter_id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("location"),
                            rs.getString("contract_type"),
                            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                            rs.getTimestamp("deadline") != null ? rs.getTimestamp("deadline").toLocalDateTime() : null,
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving recruiter offers: " + e.getMessage());
        }

        return list;
    }

    public static List<JobOfferRow> searchByTitle(String keyword) {
        List<JobOfferRow> list = new ArrayList<>();
        String sql = "SELECT id, recruiter_id, title, description, location, contract_type, created_at, deadline, status " +
                     "FROM job_offer WHERE status = 'OPEN' AND title LIKE ? ORDER BY created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new JobOfferRow(
                        rs.getLong("id"),
                        rs.getLong("recruiter_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("location"),
                        rs.getString("contract_type"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("deadline") != null ? rs.getTimestamp("deadline").toLocalDateTime() : null,
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error searching job offers: " + e.getMessage());
        }

        return list;
    }

    public static List<JobOfferRow> searchByLocation(String keyword) {
        List<JobOfferRow> list = new ArrayList<>();
        String sql = "SELECT id, recruiter_id, title, description, location, contract_type, created_at, deadline, status " +
                     "FROM job_offer WHERE status = 'OPEN' AND location LIKE ? ORDER BY created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new JobOfferRow(
                        rs.getLong("id"),
                        rs.getLong("recruiter_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("location"),
                        rs.getString("contract_type"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("deadline") != null ? rs.getTimestamp("deadline").toLocalDateTime() : null,
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error searching job offers: " + e.getMessage());
        }

        return list;
    }

    public static List<JobOfferRow> searchByContractType(String contractType) {
        List<JobOfferRow> list = new ArrayList<>();
        String sql = "SELECT id, recruiter_id, title, description, location, contract_type, created_at, deadline, status " +
                     "FROM job_offer WHERE status = 'OPEN' AND contract_type = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, contractType);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new JobOfferRow(
                        rs.getLong("id"),
                        rs.getLong("recruiter_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("location"),
                        rs.getString("contract_type"),
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("deadline") != null ? rs.getTimestamp("deadline").toLocalDateTime() : null,
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error searching job offers: " + e.getMessage());
        }

        return list;
    }

    public static List<String> getOfferSkills(Long offerId) {
        List<String> skills = new ArrayList<>();
        String sql = "SELECT skill_name FROM offer_skill WHERE offer_id = ? ORDER BY level_required DESC, skill_name ASC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    skills.add(rs.getString("skill_name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving offer skills: " + e.getMessage());
        }

        return skills;
    }
}
}

