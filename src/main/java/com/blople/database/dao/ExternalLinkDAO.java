package com.blople.database.dao;

import com.blople.database.DatabaseManager;
import com.blople.model.ExternalLink;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ExternalLinkDAO extends BaseDAO<ExternalLink, String> {

    public ExternalLinkDAO(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override protected String getTableName()    { return "external_link"; }
    @Override protected String getIdColumnName() { return "id"; }

    @Override
    protected ExternalLink mapRow(ResultSet rs) throws SQLException {
        return ExternalLink.reconstruct(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("url"),
                rs.getInt("sort_order"),
                rs.getString("created_at"));
    }

    public void insert(ExternalLink l) {
        String sql = """
            INSERT INTO external_link (id, name, url, sort_order, created_at)
            VALUES (?, ?, ?, ?, ?)
            """;
        executeUpdate(sql, l.getId(), l.getName(), l.getUrl(),
                l.getSortOrder(), l.getCreatedAt());
    }

    public void update(ExternalLink l) {
        String sql = "UPDATE external_link SET name = ?, url = ?, sort_order = ? WHERE id = ?";
        executeUpdate(sql, l.getName(), l.getUrl(), l.getSortOrder(), l.getId());
    }

    public boolean deleteById(String id) {
        return executeUpdate("DELETE FROM external_link WHERE id = ?", id) > 0;
    }

    @Override
    public List<ExternalLink> findAll() {
        return queryList("SELECT * FROM external_link ORDER BY sort_order ASC, created_at ASC");
    }
}
