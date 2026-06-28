package com.hfnew.util;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BatchNameLoader {

    private static final Set<String> ALLOWED_TABLES = Set.of("t_material", "t_elderly", "t_user", "t_staff");

    /**
     * Batch load names from a table by IDs.
     *
     * @param jdbcTemplate the JdbcTemplate
     * @param table        table name (must be from whitelist)
     * @param idColumn     ID column name
     * @param nameColumn   name column name
     * @param ids          set of IDs to look up
     * @return Map of ID -> Name
     */
    public static Map<Long, String> loadNames(JdbcTemplate jdbcTemplate, String table, String idColumn, String nameColumn, Set<Long> ids) {
        if (!ALLOWED_TABLES.contains(table)) {
            throw new IllegalArgumentException("Table not in whitelist: " + table);
        }
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> result = new HashMap<>();
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT " + idColumn + ", " + nameColumn + " FROM " + table + " WHERE " + idColumn + " IN (" + placeholders + ")";
        Object[] params = ids.toArray();
        jdbcTemplate.query(sql, rs -> {
            result.put(rs.getLong(1), rs.getString(2));
        }, params);
        return result;
    }
}
