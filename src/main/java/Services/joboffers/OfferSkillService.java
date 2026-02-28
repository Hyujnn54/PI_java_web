package Services.joboffers;

import Models.joboffers.OfferSkill;
import Models.joboffers.SkillLevel;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OfferSkillService {

    public OfferSkillService() {
        // No cached connection — always use conn() to get a live connection
    }

    /** Always returns a live connection, reconnecting if needed. */
    private Connection conn() {
        return MyDatabase.getInstance().getConnection();
    }

    // CREATE
    public OfferSkill createOfferSkill(OfferSkill offerSkill) throws SQLException {
        String query = "INSERT INTO offer_skill (offer_id, skill_name, level_required) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, offerSkill.getOfferId());
            ps.setString(2, offerSkill.getSkillName());
            ps.setString(3, offerSkill.getLevelRequired().name());
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) offerSkill.setId(generatedKeys.getLong(1));
                }
            }
            return offerSkill;
        }
    }

    // CREATE - Batch insert
    public void createOfferSkills(List<OfferSkill> offerSkills) throws SQLException {
        String query = "INSERT INTO offer_skill (offer_id, skill_name, level_required) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            for (OfferSkill skill : offerSkills) {
                ps.setLong(1, skill.getOfferId());
                ps.setString(2, skill.getSkillName());
                ps.setString(3, skill.getLevelRequired().name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // READ - Get by ID
    public OfferSkill getOfferSkillById(Long id) throws SQLException {
        String query = "SELECT * FROM offer_skill WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSetToOfferSkill(rs);
        }
        return null;
    }

    // READ - Get all skills for an offer
    public List<OfferSkill> getSkillsByOfferId(Long offerId) throws SQLException {
        List<OfferSkill> skills = new ArrayList<>();
        String query = "SELECT * FROM offer_skill WHERE offer_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, offerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) skills.add(mapResultSetToOfferSkill(rs));
        }
        return skills;
    }

    // READ - Get All
    public List<OfferSkill> getAllOfferSkills() throws SQLException {
        List<OfferSkill> skills = new ArrayList<>();
        String query = "SELECT * FROM offer_skill";
        try (Statement stmt = conn().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) skills.add(mapResultSetToOfferSkill(rs));
        }
        return skills;
    }

    // READ - Search by skill name
    public List<OfferSkill> searchBySkillName(String skillName) throws SQLException {
        List<OfferSkill> skills = new ArrayList<>();
        String query = "SELECT * FROM offer_skill WHERE skill_name LIKE ?";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setString(1, "%" + skillName + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) skills.add(mapResultSetToOfferSkill(rs));
        }
        return skills;
    }

    // UPDATE
    public boolean updateOfferSkill(OfferSkill offerSkill) throws SQLException {
        String query = "UPDATE offer_skill SET offer_id = ?, skill_name = ?, level_required = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, offerSkill.getOfferId());
            ps.setString(2, offerSkill.getSkillName());
            ps.setString(3, offerSkill.getLevelRequired().name());
            ps.setLong(4, offerSkill.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean deleteOfferSkill(Long id) throws SQLException {
        String query = "DELETE FROM offer_skill WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // DELETE - Delete all skills for an offer
    public boolean deleteSkillsByOfferId(Long offerId) throws SQLException {
        String query = "DELETE FROM offer_skill WHERE offer_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(query)) {
            ps.setLong(1, offerId);
            return ps.executeUpdate() > 0;
        }
    }

    // UPDATE - Replace all skills for an offer
    public void replaceOfferSkills(Long offerId, List<OfferSkill> newSkills) throws SQLException {
        Connection c = conn();
        c.setAutoCommit(false);
        try {
            deleteSkillsByOfferId(offerId);
            if (newSkills != null && !newSkills.isEmpty()) createOfferSkills(newSkills);
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    private OfferSkill mapResultSetToOfferSkill(ResultSet rs) throws SQLException {
        OfferSkill offerSkill = new OfferSkill();
        offerSkill.setId(rs.getLong("id"));
        offerSkill.setOfferId(rs.getLong("offer_id"));
        offerSkill.setSkillName(rs.getString("skill_name"));
        offerSkill.setLevelRequired(SkillLevel.valueOf(rs.getString("level_required")));
        return offerSkill;
    }
}
