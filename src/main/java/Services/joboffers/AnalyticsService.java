package Services.joboffers;

import Models.joboffers.ContractType;
import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Service pour les statistiques et analytics du tableau de bord.
 * Always fetches a fresh connection to avoid stale-connection failures.
 */
public class AnalyticsService {

    // Never cache – always ask MyDatabase for the current live connection
    private Connection getConn() {
        return MyDatabase.getInstance().getConnection();
    }

    // ==================== STATISTIQUES GÉNÉRALES ====================

    /**
     * Statistiques globales pour l'admin
     */
    public DashboardStats getGlobalStats() throws SQLException {
        DashboardStats stats = new DashboardStats();

        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT COUNT(*) as total FROM job_offer");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) stats.setTotalOffers(rs.getInt("total"));
        }

        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT COUNT(*) as active FROM job_offer WHERE status = 'OPEN'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) stats.setActiveOffers(rs.getInt("active"));
        }

        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT COUNT(*) as closed FROM job_offer WHERE status = 'CLOSED'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) stats.setClosedOffers(rs.getInt("closed"));
        }

        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT COUNT(*) as flagged FROM job_offer WHERE is_flagged = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) stats.setFlaggedOffers(rs.getInt("flagged"));
        }

        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT COUNT(DISTINCT recruiter_id) as recruiters FROM job_offer");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) stats.setActiveRecruiters(rs.getInt("recruiters"));
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
                "SUM(CASE WHEN status = 'OPEN'   THEN 1 ELSE 0 END) as active, " +
                "SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END) as closed, " +
                "SUM(CASE WHEN is_flagged = 1    THEN 1 ELSE 0 END) as flagged " +
                "FROM job_offer WHERE recruiter_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
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
        for (ContractType t : ContractType.values()) result.put(t, 0);

        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT contract_type, COUNT(*) as count FROM job_offer GROUP BY contract_type");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    result.put(ContractType.valueOf(rs.getString("contract_type")), rs.getInt("count"));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return result;
    }

    /**
     * Répartition des offres par type de contrat pour un recruteur
     */
    public Map<ContractType, Integer> getOffersByContractType(Long recruiterId) throws SQLException {
        Map<ContractType, Integer> result = new LinkedHashMap<>();
        for (ContractType t : ContractType.values()) result.put(t, 0);

        String sql = "SELECT contract_type, COUNT(*) as count FROM job_offer " +
                     "WHERE recruiter_id = ? GROUP BY contract_type";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        result.put(ContractType.valueOf(rs.getString("contract_type")), rs.getInt("count"));
                    } catch (IllegalArgumentException ignored) {}
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
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.put(rs.getString("location"), rs.getInt("count"));
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
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate m = now.minusMonths(i);
            result.put(String.format("%d-%02d", m.getYear(), m.getMonthValue()), 0);
        }
        String sql = "SELECT DATE_FORMAT(created_at,'%Y-%m') as month, COUNT(*) as count " +
                     "FROM job_offer WHERE created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH) " +
                     "GROUP BY month ORDER BY month";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String k = rs.getString("month");
                if (result.containsKey(k)) result.put(k, rs.getInt("count"));
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
            LocalDate m = now.minusMonths(i);
            result.put(String.format("%d-%02d", m.getYear(), m.getMonthValue()), 0);
        }
        String sql = "SELECT DATE_FORMAT(created_at,'%Y-%m') as month, COUNT(*) as count " +
                     "FROM job_offer WHERE recruiter_id = ? " +
                     "AND created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH) " +
                     "GROUP BY month ORDER BY month";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String k = rs.getString("month");
                    if (result.containsKey(k)) result.put(k, rs.getInt("count"));
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
        for (String j : new String[]{"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"}) result.put(j, 0);

        String sql = "SELECT DAYOFWEEK(created_at) as day, COUNT(*) as count " +
                     "FROM job_offer WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY day";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int d = rs.getInt("day");
                String jour = switch (d) {
                    case 2 -> "Lun"; case 3 -> "Mar"; case 4 -> "Mer";
                    case 5 -> "Jeu"; case 6 -> "Ven"; case 7 -> "Sam"; case 1 -> "Dim";
                    default -> null;
                };
                if (jour != null) result.put(jour, rs.getInt("count"));
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
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.put(rs.getString("skill_name"), rs.getInt("count"));
            }
        }
        return result;
    }

    // ==================== CLASSE INTERNE POUR LES STATS ====================

    /**
     * Classe pour stocker les statistiques du tableau de bord
     */
    public static class DashboardStats {
        private int totalOffers, activeOffers, closedOffers, flaggedOffers;
        private int activeRecruiters, totalCandidates, applicationsThisMonth;
        private double averageTimeToHire;

        public int getTotalOffers()    { return totalOffers; }
        public void setTotalOffers(int v)  { this.totalOffers = v; }

        public int getActiveOffers()   { return activeOffers; }
        public void setActiveOffers(int v) { this.activeOffers = v; }

        public int getClosedOffers()   { return closedOffers; }
        public void setClosedOffers(int v) { this.closedOffers = v; }

        public int getFlaggedOffers()  { return flaggedOffers; }
        public void setFlaggedOffers(int v){ this.flaggedOffers = v; }

        public int getActiveRecruiters()     { return activeRecruiters; }
        public void setActiveRecruiters(int v){ this.activeRecruiters = v; }

        public int getTotalCandidates()      { return totalCandidates; }
        public void setTotalCandidates(int v){ this.totalCandidates = v; }

        public int getApplicationsThisMonth()      { return applicationsThisMonth; }
        public void setApplicationsThisMonth(int v){ this.applicationsThisMonth = v; }

        public double getAverageTimeToHire()       { return averageTimeToHire; }
        public void setAverageTimeToHire(double v) { this.averageTimeToHire = v; }
    }
}

