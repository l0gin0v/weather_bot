package com.utils.dao;

import com.utils.config.DatabaseConfig;
import com.utils.models.UserSession;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserSessionDAO {

    // === СОЗДАНИЕ ТАБЛИЦЫ ===

    public void createTableIfNotExists() {
        if (!DatabaseConfig.isAvailable()) {
            System.err.println("⚠️ База данных недоступна, таблица не создана");
            return;
        }

        String sql = """
            CREATE TABLE IF NOT EXISTS user_sessions (
                user_id BIGINT PRIMARY KEY,
                city VARCHAR(100),
                state VARCHAR(50),
                is_active BOOLEAN DEFAULT TRUE,
                notification_time TIME DEFAULT NULL,
                notifications_enabled BOOLEAN DEFAULT FALSE,
                last_notification_sent DATE DEFAULT NULL,
                last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_is_active (is_active),
                INDEX idx_notifications_enabled (notifications_enabled),
                INDEX idx_notification_time (notification_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Таблица user_sessions создана/проверена");
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при создании таблицы: " + e.getMessage());
        }
    }

    // === СОХРАНЕНИЕ СЕССИИ ===

    public void saveOrUpdate(UserSession session) {
        String sql = """
            INSERT INTO user_sessions 
            (user_id, city, state, is_active, notification_time, 
             notifications_enabled, last_notification_sent, last_activity) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE 
                city = VALUES(city),
                state = VALUES(state),
                is_active = VALUES(is_active),
                notification_time = VALUES(notification_time),
                notifications_enabled = VALUES(notifications_enabled),
                last_notification_sent = VALUES(last_notification_sent),
                last_activity = VALUES(last_activity)
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, session.getUserId());
            pstmt.setString(2, session.getCity());
            pstmt.setString(3, session.getState());
            pstmt.setBoolean(4, session.isActive());

            // Обработка nullable полей
            if (session.getNotificationTime() != null) {
                pstmt.setTime(5, Time.valueOf(session.getNotificationTime()));
            } else {
                pstmt.setNull(5, Types.TIME);
            }

            pstmt.setBoolean(6, session.isNotificationsEnabled());

            if (session.getLastNotificationSent() != null) {
                pstmt.setDate(7, Date.valueOf(session.getLastNotificationSent()));
            } else {
                pstmt.setNull(7, Types.DATE);
            }

            pstmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Ошибка при сохранении сессии: " + e.getMessage());
        }
    }

    // === ПОЛУЧЕНИЕ СЕССИИ ПО ID ===

    public Optional<UserSession> findById(Long userId) {
        String sql = "SELECT * FROM user_sessions WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                UserSession session = mapResultSetToSession(rs);
                return Optional.of(session);
            }

        } catch (SQLException e) {
            System.err.println("❌ Ошибка при получении сессии: " + e.getMessage());
        }

        return Optional.empty();
    }

    // === ПОЛУЧЕНИЕ ВСЕХ СЕССИЙ С УВЕДОМЛЕНИЯМИ ===

    public List<UserSession> findSessionsWithNotifications() {
        List<UserSession> sessions = new ArrayList<>();
        String sql = """
            SELECT * FROM user_sessions 
            WHERE notifications_enabled = TRUE 
            AND notification_time IS NOT NULL 
            AND city IS NOT NULL 
            AND is_active = TRUE
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UserSession session = mapResultSetToSession(rs);
                sessions.add(session);
            }

        } catch (SQLException e) {
            System.err.println("❌ Ошибка при получении сессий с уведомлениями: " + e.getMessage());
        }

        return sessions;
    }

    // === ОБНОВЛЕНИЕ ВРЕМЕНИ ПОСЛЕДНЕЙ ОТПРАВКИ УВЕДОМЛЕНИЯ ===

    public void updateLastNotificationSent(Long userId, LocalDate date) {
        String sql = "UPDATE user_sessions SET last_notification_sent = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(date));
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Ошибка при обновлении времени уведомления: " + e.getMessage());
        }
    }

    // === ОБНОВЛЕНИЕ АКТИВНОСТИ ===

    public void updateActivity(Long userId) {
        String sql = "UPDATE user_sessions SET last_activity = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Ошибка при обновлении активности: " + e.getMessage());
        }
    }

    // === ДЕАКТИВАЦИЯ СЕССИИ ===

    public void deactivateSession(Long userId) {
        String sql = "UPDATE user_sessions SET is_active = FALSE WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Ошибка при завершении сессии: " + e.getMessage());
        }
    }

    // === ОЧИСТКА СТАРЫХ СЕССИЙ ===

    public void cleanupOldSessions(int daysOld) {
        String sql = "DELETE FROM user_sessions WHERE is_active = FALSE AND last_activity < DATE_SUB(NOW(), INTERVAL ? DAY)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, daysOld);
            int deleted = pstmt.executeUpdate();
            System.out.println("🧹 Удалено старых сессий: " + deleted);

        } catch (SQLException e) {
            System.err.println("❌ Ошибка при очистке сессий: " + e.getMessage());
        }
    }

    // === ПОЛУЧЕНИЕ ВСЕХ АКТИВНЫХ СЕССИЙ ===

    public List<UserSession> getActiveSessions() {
        List<UserSession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM user_sessions WHERE is_active = TRUE ORDER BY last_activity DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UserSession session = mapResultSetToSession(rs);
                sessions.add(session);
            }

        } catch (SQLException e) {
            System.err.println("❌ Ошибка при получении активных сессий: " + e.getMessage());
        }

        return sessions;
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private UserSession mapResultSetToSession(ResultSet rs) throws SQLException {
        UserSession session = new UserSession();
        session.setUserId(rs.getLong("user_id"));
        session.setCity(rs.getString("city"));
        session.setState(rs.getString("state"));
        session.setActive(rs.getBoolean("is_active"));

        // Обработка nullable полей
        Time notificationTime = rs.getTime("notification_time");
        if (!rs.wasNull()) {
            session.setNotificationTime(notificationTime.toLocalTime());
        }

        session.setNotificationsEnabled(rs.getBoolean("notifications_enabled"));

        Date lastNotificationSent = rs.getDate("last_notification_sent");
        if (!rs.wasNull()) {
            session.setLastNotificationSent(lastNotificationSent.toLocalDate());
        }

        session.setLastActivity(rs.getTimestamp("last_activity").toLocalDateTime());
        session.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        return session;
    }

    // === ПОЛУЧЕНИЕ СТАТИСТИКИ ===

    public int getTotalSessionsCount() {
        String sql = "SELECT COUNT(*) FROM user_sessions";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Ошибка при получении количества сессий: " + e.getMessage());
        }

        return 0;
    }

    public int getActiveSessionsCount() {
        String sql = "SELECT COUNT(*) FROM user_sessions WHERE is_active = TRUE";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Ошибка при получении количества активных сессий: " + e.getMessage());
        }

        return 0;
    }

    public int getSessionsWithNotificationsCount() {
        String sql = "SELECT COUNT(*) FROM user_sessions WHERE notifications_enabled = TRUE";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Ошибка при получении количества сессий с уведомлениями: " + e.getMessage());
        }

        return 0;
    }
}