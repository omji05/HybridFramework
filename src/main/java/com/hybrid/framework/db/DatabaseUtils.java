package com.hybrid.framework.db;

import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.security.PasswordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * JDBC database utility for CRUD validation in tests.
 * <p>
 * Supports parameterized queries to prevent SQL injection.
 * Connection details are read from config.properties; the password
 * is decrypted at runtime using {@link PasswordUtils}.
 * </p>
 *
 * <b>Usage:</b>
 * <pre>
 *   try (DatabaseUtils db = DatabaseUtils.connect()) {
 *       List&lt;Map&lt;String, Object&gt;&gt; users = db.executeQuery(
 *           "SELECT * FROM users WHERE status = ?", "ACTIVE");
 *   }
 * </pre>
 */
public class DatabaseUtils implements AutoCloseable {

    private static final Logger LOG = LogManager.getLogger(DatabaseUtils.class);
    private final Connection connection;

    // ──────────────────────────────────────────────────────────────
    // Construction / Connection
    // ──────────────────────────────────────────────────────────────

    private DatabaseUtils(Connection connection) {
        this.connection = connection;
    }

    /**
     * Creates a new database connection using config.properties values.
     */
    public static DatabaseUtils connect() {
        ConfigReader config = ConfigReader.getInstance();
        String url = config.getProperty("db.url");
        String username = config.getProperty("db.username");
        String password = getPassword(config);

        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            LOG.info("Database connection established: {}", url);
            return new DatabaseUtils(conn);
        } catch (SQLException e) {
            LOG.error("Failed to connect to database: {}", e.getMessage());
            throw new RuntimeException("DB connection failure", e);
        }
    }

    /**
     * Creates a database connection with explicit parameters.
     */
    public static DatabaseUtils connect(String url, String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(url, username, password);
            LOG.info("Database connection established: {}", url);
            return new DatabaseUtils(conn);
        } catch (SQLException e) {
            LOG.error("Failed to connect to database: {}", e.getMessage());
            throw new RuntimeException("DB connection failure", e);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Query (SELECT)
    // ──────────────────────────────────────────────────────────────

    /**
     * Executes a parameterized SELECT query and returns results as a list of maps.
     *
     * @param sql    the SQL query with ? placeholders
     * @param params the parameters to bind
     * @return list of row maps (column name → value)
     */
    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (PreparedStatement ps = prepareStatement(sql, params);
             ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                results.add(row);
            }

            LOG.info("Query returned {} rows: {}", results.size(), sql);

        } catch (SQLException e) {
            LOG.error("Query execution failed: {} — {}", sql, e.getMessage());
            throw new RuntimeException("Query failure", e);
        }

        return results;
    }

    /**
     * Executes a query and returns a single value from the first row/column.
     */
    public <T> T executeScalar(String sql, Object... params) {
        List<Map<String, Object>> results = executeQuery(sql, params);
        if (results.isEmpty()) return null;
        @SuppressWarnings("unchecked")
        T value = (T) results.get(0).values().iterator().next();
        return value;
    }

    // ──────────────────────────────────────────────────────────────
    // Update (INSERT / UPDATE / DELETE)
    // ──────────────────────────────────────────────────────────────

    /**
     * Executes a parameterized INSERT, UPDATE, or DELETE statement.
     *
     * @return the number of affected rows
     */
    public int executeUpdate(String sql, Object... params) {
        try (PreparedStatement ps = prepareStatement(sql, params)) {
            int affected = ps.executeUpdate();
            LOG.info("Update affected {} rows: {}", affected, sql);
            return affected;
        } catch (SQLException e) {
            LOG.error("Update execution failed: {} — {}", sql, e.getMessage());
            throw new RuntimeException("Update failure", e);
        }
    }

    /**
     * Executes an INSERT and returns the generated key.
     */
    public long executeInsertAndGetKey(String sql, Object... params) {
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParameters(ps, params);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long key = keys.getLong(1);
                    LOG.info("Inserted row with generated key: {}", key);
                    return key;
                }
            }
            throw new RuntimeException("No generated key returned for: " + sql);
        } catch (SQLException e) {
            LOG.error("Insert execution failed: {} — {}", sql, e.getMessage());
            throw new RuntimeException("Insert failure", e);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // AutoCloseable
    // ──────────────────────────────────────────────────────────────

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                LOG.info("Database connection closed");
            } catch (SQLException e) {
                LOG.warn("Error closing database connection: {}", e.getMessage());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────

    private PreparedStatement prepareStatement(String sql, Object... params) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        bindParameters(ps, params);
        return ps;
    }

    private void bindParameters(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static String getPassword(ConfigReader config) {
        String encryptedPwd = config.getProperty("db.password.encrypted");
        if (encryptedPwd != null && !encryptedPwd.isEmpty()) {
            try {
                return PasswordUtils.decrypt(encryptedPwd);
            } catch (Exception e) {
                LOG.warn("Encrypted password decryption failed, trying base64 decode");
                return PasswordUtils.base64Decode(encryptedPwd);
            }
        }
        return config.getProperty("db.password", "");
    }
}
