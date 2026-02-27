package Services;

import Models.ContractType;
import Models.JobOffer;
import Models.Status;
import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service pour les statistiques et analytics du tableau de bord
 */
public class AnalyticsService {

    private Connection connection;

    public AnalyticsService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // ==================== STATISTIQUES GÉNÉRALES ====================

    /**
     * Statistiques globales pour l'admin
     */
    public DashboardStats getGlobalStats() throws SQLException {
        DashboardStats stats = new DashboardStats();

        // Total des offres
        String sqlTotal = "SELECT COUNT(*) as total FROM job_offer";
        try (PreparedStatement ps = connection.prepareStatement(sqlTotal);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.setTotalOffers(rs.getInt("total"));
            }
        }

        // Offres actives
        String sqlActive = "SELECT COUNT(*) as active FROM job_offer WHERE status = 'OPEN'";
        try (PreparedStatement ps = connection.prepareStatement(sqlActive);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.setActiveOffers(rs.getInt("active"));
            }
        }

        // Offres fermées
        String sqlClosed = "SELECT COUNT(*) as closed FROM job_offer WHERE status = 'CLOSED'";
        try (PreparedStatement ps = connection.prepareStatement(sqlClosed);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.setClosedOffers(rs.getInt("closed"));
            }
        }

        // Offres signalées
        String sqlFlagged = "SELECT COUNT(*) as flagged FROM job_offer WHERE is_flagged = 1";
        try (PreparedStatement ps = connection.prepareStatement(sqlFlagged);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.setFlaggedOffers(rs.getInt("flagged"));
            }
        }

        // Nombre de recruteurs actifs
        String sqlRecruiters = "SELECT COUNT(DISTINCT recruiter_id) as recruiters FROM job_offer";
        try (PreparedStatement ps = connection.prepareStatement(sqlRecruiters);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.setActiveRecruiters(rs.getInt("recruiters"));
            }
        }

        return stats;
    }

    /**
     * Statistiques pour un recruteur spécifique
     */
    public DashboardStats getRecruiterStats(Long recruiterId) throws SQLException {
        DashboardStats stats = new DashboardStats();

        String sql = "SELECT " +
                    "COUNT(*) as total, " +
                    "SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END) as active, " +
                    "SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END) as closed, " +
                    "SUM(CASE WHEN is_flagged = 1 THEN 1 ELSE 0 END) as flagged " +
                    "FROM job_offer WHERE recruiter_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalOffers(rs.getInt("total"));
                    stats.setActiveOffers(rs.getInt("active"));
                    stats.setClosedOffers(rs.getInt("closed"));
                    stats.setFlaggedOffers(rs.getInt("flagged"));
                }
            }
        }

        return stats;
    }

    // ==================== STATISTIQUES PAR TYPE DE CONTRAT ====================

    /**
     * Répartition des offres par type de contrat (global)
     */
    public Map<ContractType, Integer> getOffersByContractType() throws SQLException {
        Map<ContractType, Integer> result = new LinkedHashMap<>();

        // Initialiser avec 0
        for (ContractType type : ContractType.values()) {
            result.put(type, 0);
        }

        String sql = "SELECT contract_type, COUNT(*) as count FROM job_offer GROUP BY contract_type";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String typeStr = rs.getString("contract_type");
                int count = rs.getInt("count");
                try {
                    ContractType type = ContractType.valueOf(typeStr);
                    result.put(type, count);
                } catch (IllegalArgumentException e) {
                    // Type inconnu, ignorer
                }
            }
        }

        return result;
    }

    /**
     * Répartition des offres par type de contrat pour un recruteur
     */
    public Map<ContractType, Integer> getOffersByContractType(Long recruiterId) throws SQLException {
        Map<ContractType, Integer> result = new LinkedHashMap<>();

        for (ContractType type : ContractType.values()) {
            result.put(type, 0);
        }

        String sql = "SELECT contract_type, COUNT(*) as count FROM job_offer WHERE recruiter_id = ? GROUP BY contract_type";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String typeStr = rs.getString("contract_type");
                    int count = rs.getInt("count");
                    try {
                        ContractType type = ContractType.valueOf(typeStr);
                        result.put(type, count);
                    } catch (IllegalArgumentException e) {
                        // Type inconnu
                    }
                }
            }
        }

        return result;
    }

    // ==================== STATISTIQUES PAR LOCALISATION ====================

    /**
     * Top des localisations avec le plus d'offres
     */
    public Map<String, Integer> getTopLocations(int limit) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();

        String sql = "SELECT location, COUNT(*) as count FROM job_offer " +
                    "WHERE location IS NOT NULL AND location != '' " +
                    "GROUP BY location ORDER BY count DESC LIMIT ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("location"), rs.getInt("count"));
                }
            }
        }

        return result;
    }

    // ==================== STATISTIQUES TEMPORELLES ====================

    /**
     * Nombre d'offres créées par mois (12 derniers mois)
     */
    public Map<String, Integer> getOffersByMonth() throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();

        // Initialiser les 12 derniers mois avec 0
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String key = String.format("%d-%02d", month.getYear(), month.getMonthValue());
            result.put(key, 0);
        }

        String sql = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count " +
                    "FROM job_offer " +
                    "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH) " +
                    "GROUP BY month ORDER BY month";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String month = rs.getString("month");
                int count = rs.getInt("count");
                if (result.containsKey(month)) {
                    result.put(month, count);
                }
            }
        }

        return result;
    }

    /**
     * Nombre d'offres créées par mois pour un recruteur
     */
    public Map<String, Integer> getOffersByMonth(Long recruiterId) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();

        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String key = String.format("%d-%02d", month.getYear(), month.getMonthValue());
            result.put(key, 0);
        }

        String sql = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count " +
                    "FROM job_offer " +
                    "WHERE recruiter_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH) " +
                    "GROUP BY month ORDER BY month";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String month = rs.getString("month");
                    int count = rs.getInt("count");
                    if (result.containsKey(month)) {
                        result.put(month, count);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Offres créées cette semaine par jour
     */
    public Map<String, Integer> getOffersThisWeek() throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();

        String[] jours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (String jour : jours) {
            result.put(jour, 0);
        }

        String sql = "SELECT DAYOFWEEK(created_at) as day, COUNT(*) as count " +
                    "FROM job_offer " +
                    "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                    "GROUP BY day";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int dayNum = rs.getInt("day"); // 1=Dimanche, 2=Lundi...
                int count = rs.getInt("count");
                String jour = switch (dayNum) {
                    case 2 -> "Lun";
                    case 3 -> "Mar";
                    case 4 -> "Mer";
                    case 5 -> "Jeu";
                    case 6 -> "Ven";
                    case 7 -> "Sam";
                    case 1 -> "Dim";
                    default -> null;
                };
                if (jour != null) {
                    result.put(jour, count);
                }
            }
        }

        return result;
    }

    // ==================== STATISTIQUES DES COMPÉTENCES ====================

    /**
     * Top des compétences les plus demandées
     */
    public Map<String, Integer> getTopSkills(int limit) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();

        String sql = "SELECT skill_name, COUNT(*) as count FROM offer_skill " +
                    "GROUP BY skill_name ORDER BY count DESC LIMIT ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("skill_name"), rs.getInt("count"));
                }
            }
        }

        return result;
    }

    // ==================== CLASSE INTERNE POUR LES STATS ====================

    /**
     * Classe pour stocker les statistiques du tableau de bord
     */
    public static class DashboardStats {
        private int totalOffers;
        private int activeOffers;
        private int closedOffers;
        private int flaggedOffers;
        private int activeRecruiters;
        private int totalCandidates;
        private int applicationsThisMonth;
        private double averageTimeToHire;

        public int getTotalOffers() { return totalOffers; }
        public void setTotalOffers(int total) { this.totalOffers = total; }

        public int getActiveOffers() { return activeOffers; }
        public void setActiveOffers(int active) { this.activeOffers = active; }

        public int getClosedOffers() { return closedOffers; }
        public void setClosedOffers(int closed) { this.closedOffers = closed; }

        public int getFlaggedOffers() { return flaggedOffers; }
        public void setFlaggedOffers(int flagged) { this.flaggedOffers = flagged; }

        public int getActiveRecruiters() { return activeRecruiters; }
        public void setActiveRecruiters(int recruiters) { this.activeRecruiters = recruiters; }

        public int getTotalCandidates() { return totalCandidates; }
        public void setTotalCandidates(int candidates) { this.totalCandidates = candidates; }

        public int getApplicationsThisMonth() { return applicationsThisMonth; }
        public void setApplicationsThisMonth(int apps) { this.applicationsThisMonth = apps; }

        public double getAverageTimeToHire() { return averageTimeToHire; }
        public void setAverageTimeToHire(double time) { this.averageTimeToHire = time; }
    }
}

