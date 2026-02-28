package Services.joboffers;

import Models.joboffers.JobOfferWarning;
import Models.joboffers.JobOfferWarning.WarningStatus;
import Models.joboffers.Status;
import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer les avertissements sur les offres d'emploi
 */
public class JobOfferWarningService {

    private Connection connection;

    public JobOfferWarningService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    /**
     * Créer un nouvel avertissement et flaguer l'offre
     */
    public JobOfferWarning createWarning(JobOfferWarning warning) throws SQLException {
        // Vérifier et obtenir un admin_id valide
        Long adminId = getValidAdminId(warning.getAdminId());
        if (adminId == null) {
            throw new SQLException("Aucun administrateur trouvé dans la base de données");
        }
        warning.setAdminId(adminId);

        // Vérifier et obtenir un recruiter_id valide
        Long recruiterId = getValidRecruiterId(warning.getRecruiterId(), warning.getJobOfferId());
        if (recruiterId == null) {
            throw new SQLException("Aucun recruteur trouvé pour cette offre");
        }
        warning.setRecruiterId(recruiterId);

        String query = "INSERT INTO job_offer_warning (job_offer_id, recruiter_id, admin_id, reason, message, status, created_at) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, warning.getJobOfferId());
            ps.setLong(2, warning.getRecruiterId());
            ps.setLong(3, warning.getAdminId());
            ps.setString(4, warning.getReason());
            ps.setString(5, warning.getMessage());
            ps.setString(6, warning.getStatus().name());
            ps.setTimestamp(7, Timestamp.valueOf(warning.getCreatedAt()));

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        warning.setId(generatedKeys.getLong(1));
                    }
                }

                // Mettre à jour l'offre comme signalée
                flagJobOffer(warning.getJobOfferId());
            }

            return warning;
        }
    }

    /**
     * Obtient un recruiter_id valide depuis la base de données
     */
    private Long getValidRecruiterId(Long preferredId, Long jobOfferId) throws SQLException {
        // D'abord essayer avec l'ID préféré dans différentes tables possibles
        String[] possibleTables = {"recruiter", "recruiters", "user", "users"};

        if (preferredId != null) {
            for (String table : possibleTables) {
                try {
                    String checkQuery = "SELECT id FROM " + table + " WHERE id = ?";
                    try (PreparedStatement ps = connection.prepareStatement(checkQuery)) {
                        ps.setLong(1, preferredId);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            return rs.getLong("id");
                        }
                    }
                } catch (SQLException e) {
                    // Table n'existe pas, continuer avec la suivante
                }
            }
        }

        // Sinon, récupérer le recruiter_id depuis l'offre d'emploi et vérifier
        if (jobOfferId != null) {
            String query = "SELECT recruiter_id FROM job_offer WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setLong(1, jobOfferId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Long recId = rs.getLong("recruiter_id");
                    // Vérifier que ce recruiter existe
                    for (String table : possibleTables) {
                        try {
                            String verifyQuery = "SELECT id FROM " + table + " WHERE id = ?";
                            try (PreparedStatement ps2 = connection.prepareStatement(verifyQuery)) {
                                ps2.setLong(1, recId);
                                ResultSet rs2 = ps2.executeQuery();
                                if (rs2.next()) {
                                    return rs2.getLong("id");
                                }
                            }
                        } catch (SQLException e) {
                            // Table n'existe pas, continuer
                        }
                    }
                    // Si le recruiter_id existe dans job_offer, l'utiliser directement
                    return recId;
                }
            }
        }

        // En dernier recours, prendre le premier disponible
        for (String table : possibleTables) {
            try {
                String query = "SELECT id FROM " + table + " LIMIT 1";
                try (PreparedStatement ps = connection.prepareStatement(query)) {
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        return rs.getLong("id");
                    }
                }
            } catch (SQLException e) {
                // Table n'existe pas, continuer
            }
        }

        return 1L; // Valeur par défaut
    }

    /**
     * Obtient un admin_id valide depuis la base de données
     */
    private Long getValidAdminId(Long preferredId) throws SQLException {
        String[] possibleTables = {"admin", "admins", "administrator", "user", "users"};

        // D'abord essayer avec l'ID préféré
        if (preferredId != null) {
            for (String table : possibleTables) {
                try {
                    String checkQuery = "SELECT id FROM " + table + " WHERE id = ?";
                    try (PreparedStatement ps = connection.prepareStatement(checkQuery)) {
                        ps.setLong(1, preferredId);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            return rs.getLong("id");
                        }
                    }
                } catch (SQLException e) {
                    // Table n'existe pas, continuer
                }
            }
        }

        // Sinon, prendre le premier admin disponible
        for (String table : possibleTables) {
            try {
                String query = "SELECT id FROM " + table + " LIMIT 1";
                try (PreparedStatement ps = connection.prepareStatement(query)) {
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        return rs.getLong("id");
                    }
                }
            } catch (SQLException e) {
                // Table n'existe pas, continuer
            }
        }

        return 1L; // Valeur par défaut
    }

    /**
     * Flaguer une offre d'emploi
     */
    public void flagJobOffer(Long jobOfferId) throws SQLException {
        String query = "UPDATE job_offer SET is_flagged = 1, flagged_at = ?, status = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, Status.FLAGGED.name());
            ps.setLong(3, jobOfferId);
            ps.executeUpdate();
        }
    }

    /**
     * Marquer un avertissement comme vu
     */
    public boolean markAsSeen(Long warningId) throws SQLException {
        String query = "UPDATE job_offer_warning SET status = ?, seen_at = ? WHERE id = ? AND status = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, WarningStatus.SEEN.name());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, warningId);
            ps.setString(4, WarningStatus.SENT.name());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Résoudre un avertissement
     */
    public boolean resolveWarning(Long warningId) throws SQLException {
        String query = "UPDATE job_offer_warning SET status = ?, resolved_at = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, WarningStatus.RESOLVED.name());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, warningId);

            int updated = ps.executeUpdate();

            if (updated > 0) {
                // Vérifier s'il reste des avertissements non résolus pour cette offre
                JobOfferWarning warning = getWarningById(warningId);
                if (warning != null) {
                    List<JobOfferWarning> pendingWarnings = getPendingWarningsByJobOfferId(warning.getJobOfferId());
                    if (pendingWarnings.isEmpty()) {
                        // Plus d'avertissements en attente, retirer le flag
                        unflagJobOffer(warning.getJobOfferId());
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Annuler un avertissement (par l'admin)
     */
    public boolean dismissWarning(Long warningId) throws SQLException {
        String query = "UPDATE job_offer_warning SET status = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, WarningStatus.DISMISSED.name());
            ps.setLong(2, warningId);

            int updated = ps.executeUpdate();

            if (updated > 0) {
                JobOfferWarning warning = getWarningById(warningId);
                if (warning != null) {
                    List<JobOfferWarning> pendingWarnings = getPendingWarningsByJobOfferId(warning.getJobOfferId());
                    if (pendingWarnings.isEmpty()) {
                        unflagJobOffer(warning.getJobOfferId());
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Retirer le flag d'une offre
     */
    public void unflagJobOffer(Long jobOfferId) throws SQLException {
        String query = "UPDATE job_offer SET is_flagged = 0, flagged_at = NULL, status = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, Status.OPEN.name());
            ps.setLong(2, jobOfferId);
            ps.executeUpdate();
        }
    }

    /**
     * Récupérer un avertissement par ID
     */
    public JobOfferWarning getWarningById(Long id) throws SQLException {
        String query = "SELECT * FROM job_offer_warning WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToWarning(rs);
            }
        }
        return null;
    }

    /**
     * Récupérer tous les avertissements pour une offre
     */
    public List<JobOfferWarning> getWarningsByJobOfferId(Long jobOfferId) throws SQLException {
        List<JobOfferWarning> warnings = new ArrayList<>();
        String query = "SELECT * FROM job_offer_warning WHERE job_offer_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, jobOfferId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                warnings.add(mapResultSetToWarning(rs));
            }
        }
        return warnings;
    }

    /**
     * Récupérer les avertissements en attente (SENT ou SEEN) pour une offre
     */
    public List<JobOfferWarning> getPendingWarningsByJobOfferId(Long jobOfferId) throws SQLException {
        List<JobOfferWarning> warnings = new ArrayList<>();
        String query = "SELECT * FROM job_offer_warning WHERE job_offer_id = ? AND status IN (?, ?) ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, jobOfferId);
            ps.setString(2, WarningStatus.SENT.name());
            ps.setString(3, WarningStatus.SEEN.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                warnings.add(mapResultSetToWarning(rs));
            }
        }
        return warnings;
    }

    /**
     * Récupérer tous les avertissements en attente (pour admin)
     */
    public List<JobOfferWarning> getAllPendingWarnings() throws SQLException {
        List<JobOfferWarning> warnings = new ArrayList<>();
        String query = "SELECT * FROM job_offer_warning WHERE status IN (?, ?) ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, WarningStatus.SENT.name());
            ps.setString(2, WarningStatus.SEEN.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                warnings.add(mapResultSetToWarning(rs));
            }
        }
        return warnings;
    }

    /**
     * Récupérer les avertissements pour un recruteur
     */
    public List<JobOfferWarning> getWarningsForRecruiter(Long recruiterId) throws SQLException {
        List<JobOfferWarning> warnings = new ArrayList<>();
        String query = "SELECT * FROM job_offer_warning WHERE recruiter_id = ? AND status IN (?, ?) ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, recruiterId);
            ps.setString(2, WarningStatus.SENT.name());
            ps.setString(3, WarningStatus.SEEN.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                warnings.add(mapResultSetToWarning(rs));
            }
        }
        return warnings;
    }

    /**
     * Compter les avertissements en attente pour un recruteur
     */
    public int countPendingWarningsForRecruiter(Long recruiterId) throws SQLException {
        String query = "SELECT COUNT(*) FROM job_offer_warning WHERE recruiter_id = ? AND status IN (?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setLong(1, recruiterId);
            ps.setString(2, WarningStatus.SENT.name());
            ps.setString(3, WarningStatus.SEEN.name());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Mapper ResultSet vers JobOfferWarning
     */
    private JobOfferWarning mapResultSetToWarning(ResultSet rs) throws SQLException {
        JobOfferWarning warning = new JobOfferWarning();
        warning.setId(rs.getLong("id"));
        warning.setJobOfferId(rs.getLong("job_offer_id"));
        warning.setRecruiterId(rs.getLong("recruiter_id"));
        warning.setAdminId(rs.getLong("admin_id"));
        warning.setReason(rs.getString("reason"));
        warning.setMessage(rs.getString("message"));
        warning.setStatus(WarningStatus.valueOf(rs.getString("status")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            warning.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp seenAt = rs.getTimestamp("seen_at");
        if (seenAt != null) {
            warning.setSeenAt(seenAt.toLocalDateTime());
        }

        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        if (resolvedAt != null) {
            warning.setResolvedAt(resolvedAt.toLocalDateTime());
        }

        return warning;
    }
}




