package Services;

import Models.JobOffer;
import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JobOfferService {
    private Connection connection;

    public JobOfferService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // CREATE
    public JobOffer createJobOffer(JobOffer jobOffer) throws SQLException {
        String query = "INSERT INTO job_offer (recruiter_id, title, description, location, contract_type, created_at, deadline, status) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, jobOffer.getRecruiterId());
            ps.setString(2, jobOffer.getTitle());
            ps.setString(3, jobOffer.getDescription());
            ps.setString(4, jobOffer.getLocation());
            ps.setString(5, jobOffer.getContractType().name());
            ps.setTimestamp(6, Timestamp.valueOf(jobOffer.getCreatedAt()));
            ps.setTimestamp(7, jobOffer.getDeadline() != null ? Timestamp.valueOf(jobOffer.getDeadline()) : null);
            ps.setString(8, jobOffer.getStatus().name());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        jobOffer.setId(generatedKeys.getLong(1));
                    }
                }
            }
            return jobOffer;
        }
    }

    // READ - Get by ID
    public JobOffer getJobOfferById(Long id) throws SQLException {
        String query = "SELECT * FROM job_offer WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToJobOffer(rs);
            }
        }
        return null;
    }

    // READ - Get All
    public List<JobOffer> getAllJobOffers() throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String query = "SELECT * FROM job_offer ORDER BY created_at DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                jobOffers.add(mapResultSetToJobOffer(rs));
            }
        }
        return jobOffers;
    }

    // READ - Get by Status
    public List<JobOffer> getJobOffersByStatus(JobOffer.Status status) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String query = "SELECT * FROM job_offer WHERE status = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                jobOffers.add(mapResultSetToJobOffer(rs));
            }
        }
        return jobOffers;
    }

    // READ - Get by Recruiter ID
    public List<JobOffer> getJobOffersByRecruiterId(Long recruiterId) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String query = "SELECT * FROM job_offer WHERE recruiter_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, recruiterId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                jobOffers.add(mapResultSetToJobOffer(rs));
            }
        }
        return jobOffers;
    }

    // READ - Search
    public List<JobOffer> searchJobOffers(String searchTerm, String searchField) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        String query = "SELECT * FROM job_offer WHERE " + searchField + " LIKE ? ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, "%" + searchTerm + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                jobOffers.add(mapResultSetToJobOffer(rs));
            }
        }
        return jobOffers;
    }

    // UPDATE
    public boolean updateJobOffer(JobOffer jobOffer) throws SQLException {
        String query = "UPDATE job_offer SET recruiter_id = ?, title = ?, description = ?, location = ?, " +
                      "contract_type = ?, deadline = ?, status = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, jobOffer.getRecruiterId());
            ps.setString(2, jobOffer.getTitle());
            ps.setString(3, jobOffer.getDescription());
            ps.setString(4, jobOffer.getLocation());
            ps.setString(5, jobOffer.getContractType().name());
            ps.setTimestamp(6, jobOffer.getDeadline() != null ? Timestamp.valueOf(jobOffer.getDeadline()) : null);
            ps.setString(7, jobOffer.getStatus().name());
            ps.setLong(8, jobOffer.getId());

            return ps.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean deleteJobOffer(Long id) throws SQLException {
        String query = "DELETE FROM job_offer WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // UPDATE - Change Status
    public boolean updateJobOfferStatus(Long id, JobOffer.Status status) throws SQLException {
        String query = "UPDATE job_offer SET status = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Helper method to map ResultSet to JobOffer
    private JobOffer mapResultSetToJobOffer(ResultSet rs) throws SQLException {
        JobOffer jobOffer = new JobOffer();
        jobOffer.setId(rs.getLong("id"));
        jobOffer.setRecruiterId(rs.getLong("recruiter_id"));
        jobOffer.setTitle(rs.getString("title"));
        jobOffer.setDescription(rs.getString("description"));
        jobOffer.setLocation(rs.getString("location"));
        jobOffer.setContractType(JobOffer.ContractType.valueOf(rs.getString("contract_type")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            jobOffer.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp deadline = rs.getTimestamp("deadline");
        if (deadline != null) {
            jobOffer.setDeadline(deadline.toLocalDateTime());
        }

        jobOffer.setStatus(JobOffer.Status.valueOf(rs.getString("status")));

        return jobOffer;
    }

    // Inner class for displaying job offers with additional info (optional)
    public static class JobOfferRow {
        public Long id;
        public Long recruiterId;
        public String title;
        public String description;
        public String location;
        public String contractType;
        public LocalDateTime createdAt;
        public LocalDateTime deadline;
        public String status;

        public JobOfferRow(Long id, Long recruiterId, String title, String description, String location,
                          String contractType, LocalDateTime createdAt, LocalDateTime deadline, String status) {
            this.id = id;
            this.recruiterId = recruiterId;
            this.title = title;
            this.description = description;
            this.location = location;
            this.contractType = contractType;
            this.createdAt = createdAt;
            this.deadline = deadline;
            this.status = status;
        }
    }
}

