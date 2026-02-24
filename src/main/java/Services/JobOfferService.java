package Services;

import Models.JobOffer;
import Models.ContractType;
import Models.Status;
import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public List<JobOffer> getJobOffersByStatus(Status status) throws SQLException {
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
    public boolean updateJobOfferStatus(Long id, Status status) throws SQLException {
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
        jobOffer.setContractType(ContractType.valueOf(rs.getString("contract_type")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            jobOffer.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp deadline = rs.getTimestamp("deadline");
        if (deadline != null) {
            jobOffer.setDeadline(deadline.toLocalDateTime());
        }

        jobOffer.setStatus(Status.valueOf(rs.getString("status")));

        // Champs pour le système d'avertissement
        try {
            jobOffer.setFlagged(rs.getBoolean("is_flagged"));
            Timestamp flaggedAt = rs.getTimestamp("flagged_at");
            if (flaggedAt != null) {
                jobOffer.setFlaggedAt(flaggedAt.toLocalDateTime());
            }
        } catch (SQLException e) {
            // Les colonnes n'existent peut-être pas encore
            jobOffer.setFlagged(false);
        }

        return jobOffer;
    }

    // ==================== STATISTIQUES ====================

    /**
     * Statistiques globales : Nombre d'offres par type de contrat (POUR ADMIN)
     * @return Map avec ContractType en clé et le nombre d'offres en valeur
     */
    public Map<ContractType, Integer> statsGlobal() throws SQLException {
        Map<ContractType, Integer> stats = new HashMap<>();

        // Initialiser toutes les valeurs à 0
        for (ContractType type : ContractType.values()) {
            stats.put(type, 0);
        }

        String query = "SELECT contract_type, COUNT(*) as total FROM job_offer GROUP BY contract_type";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String contractTypeStr = rs.getString("contract_type");
                int total = rs.getInt("total");

                try {
                    ContractType type = ContractType.valueOf(contractTypeStr);
                    stats.put(type, total);
                } catch (IllegalArgumentException e) {
                    System.err.println("Type de contrat inconnu : " + contractTypeStr);
                }
            }
        }

        return stats;
    }

    /**
     * Statistiques par recruteur : Nombre d'offres par type de contrat (POUR RECRUITER)
     * @param recruiterId L'ID du recruteur
     * @return Map avec ContractType en clé et le nombre d'offres en valeur
     */
    public Map<ContractType, Integer> statsByRecruiter(Long recruiterId) throws SQLException {
        Map<ContractType, Integer> stats = new HashMap<>();

        // Initialiser toutes les valeurs à 0
        for (ContractType type : ContractType.values()) {
            stats.put(type, 0);
        }

        String query = "SELECT contract_type, COUNT(*) as total FROM job_offer WHERE recruiter_id = ? GROUP BY contract_type";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, recruiterId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String contractTypeStr = rs.getString("contract_type");
                int total = rs.getInt("total");

                try {
                    ContractType type = ContractType.valueOf(contractTypeStr);
                    stats.put(type, total);
                } catch (IllegalArgumentException e) {
                    System.err.println("Type de contrat inconnu : " + contractTypeStr);
                }
            }
        }

        return stats;
    }

    /**
     * Statistiques : Nombre d'offres par type de contrat (DEPRECATED - utiliser statsGlobal ou statsByRecruiter)
     * @return Map avec ContractType en clé et le nombre d'offres en valeur
     */
    @Deprecated
    public Map<ContractType, Integer> statsOffresParContractType() throws SQLException {
        return statsGlobal();
    }

    /**
     * Statistiques globales : Nombre total d'offres (POUR ADMIN)
     */
    public int getTotalOffresGlobal() throws SQLException {
        String query = "SELECT COUNT(*) as total FROM job_offer";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt("total");
            }
        }

        return 0;
    }

    /**
     * Statistiques par recruteur : Nombre total d'offres (POUR RECRUITER)
     * @param recruiterId L'ID du recruteur
     */
    public int getTotalOffresByRecruiter(Long recruiterId) throws SQLException {
        String query = "SELECT COUNT(*) as total FROM job_offer WHERE recruiter_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, recruiterId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }
        }

        return 0;
    }

    /**
     * Statistiques : Nombre total d'offres (DEPRECATED - utiliser getTotalOffresGlobal ou getTotalOffresByRecruiter)
     */
    @Deprecated
    public int getTotalOffres() throws SQLException {
        return getTotalOffresGlobal();
    }

    /**
     * Statistiques globales : Nombre d'offres expirées (deadline < CURRENT_DATE) (POUR ADMIN)
     */
    public int getExpiredOffresGlobal() throws SQLException {
        String query = "SELECT COUNT(*) as total FROM job_offer WHERE deadline < NOW()";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt("total");
            }
        }

        return 0;
    }

    /**
     * Statistiques par recruteur : Nombre d'offres expirées (deadline < CURRENT_DATE) (POUR RECRUITER)
     * @param recruiterId L'ID du recruteur
     */
    public int getExpiredOffresByRecruiter(Long recruiterId) throws SQLException {
        String query = "SELECT COUNT(*) as total FROM job_offer WHERE recruiter_id = ? AND deadline < NOW()";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, recruiterId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }
        }

        return 0;
    }

    // ==================== FILTRAGE AVANCÉ ====================

    /**
     * Filtrage avancé des offres par ville, type de contrat et statut
     * @param location La ville (null ou vide pour ignorer)
     * @param contractType Le type de contrat (null pour ignorer)
     * @param status Le statut (null pour ignorer)
     * @return Liste des offres correspondant aux critères
     */
    public List<JobOffer> filterJobOffers(String location, ContractType contractType, Status status) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM job_offer WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // Filtre par ville (location)
        if (location != null && !location.trim().isEmpty()) {
            queryBuilder.append(" AND location LIKE ?");
            params.add("%" + location.trim() + "%");
        }

        // Filtre par type de contrat
        if (contractType != null) {
            queryBuilder.append(" AND contract_type = ?");
            params.add(contractType.name());
        }

        // Filtre par statut
        if (status != null) {
            queryBuilder.append(" AND status = ?");
            params.add(status.name());
        }

        queryBuilder.append(" ORDER BY created_at DESC");

        try (PreparedStatement ps = connection.prepareStatement(queryBuilder.toString())) {
            // Définir les paramètres
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                jobOffers.add(mapResultSetToJobOffer(rs));
            }
        }

        return jobOffers;
    }

    /**
     * Filtrage avancé des offres pour un recruteur spécifique
     * @param recruiterId L'ID du recruteur
     * @param location La ville (null ou vide pour ignorer)
     * @param contractType Le type de contrat (null pour ignorer)
     * @param status Le statut (null pour ignorer)
     * @return Liste des offres correspondant aux critères
     */
    public List<JobOffer> filterJobOffersByRecruiter(Long recruiterId, String location, ContractType contractType, Status status) throws SQLException {
        List<JobOffer> jobOffers = new ArrayList<>();
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM job_offer WHERE recruiter_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(recruiterId);

        // Filtre par ville (location)
        if (location != null && !location.trim().isEmpty()) {
            queryBuilder.append(" AND location LIKE ?");
            params.add("%" + location.trim() + "%");
        }

        // Filtre par type de contrat
        if (contractType != null) {
            queryBuilder.append(" AND contract_type = ?");
            params.add(contractType.name());
        }

        // Filtre par statut
        if (status != null) {
            queryBuilder.append(" AND status = ?");
            params.add(status.name());
        }

        queryBuilder.append(" ORDER BY created_at DESC");

        try (PreparedStatement ps = connection.prepareStatement(queryBuilder.toString())) {
            // Définir les paramètres
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                jobOffers.add(mapResultSetToJobOffer(rs));
            }
        }

        return jobOffers;
    }

    /**
     * Récupère toutes les villes distinctes présentes dans les offres
     * @return Liste des villes uniques
     */
    public List<String> getAllLocations() throws SQLException {
        List<String> locations = new ArrayList<>();
        String query = "SELECT DISTINCT location FROM job_offer WHERE location IS NOT NULL AND location != '' ORDER BY location";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                locations.add(rs.getString("location"));
            }
        }

        return locations;
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

