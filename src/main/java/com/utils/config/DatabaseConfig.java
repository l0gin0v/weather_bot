package com.utils.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static HikariDataSource dataSource;
    private static boolean initialized = false;

    static {
        initDataSource();
    }

    private static void initDataSource() {
        try {
            HikariConfig config = new HikariConfig();

            // Настройки подключения
            config.setJdbcUrl("jdbc:mariadb://localhost:3306/weather_bot_db");
            config.setUsername("weather_bot_user");

            String weather_bot_password = System.getenv("weather_bot_password");

            config.setPassword(weather_bot_password);

            config.setDriverClassName("org.mariadb.jdbc.Driver");

            // Настройки пула
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(10000);
            config.setIdleTimeout(300000);
            config.setMaxLifetime(600000);

            // Дополнительные параметры
            config.addDataSourceProperty("useSSL", "false");
            config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
            config.addDataSourceProperty("serverTimezone", "UTC");
            config.addDataSourceProperty("characterEncoding", "UTF-8");

            dataSource = new HikariDataSource(config);

            // Проверяем подключение
            try (Connection testConn = dataSource.getConnection()) {
                System.out.println("✅ Успешное подключение к MariaDB!");
                initialized = true;
            }

        } catch (Exception e) {
            System.err.println("❌ Ошибка при инициализации пула соединений:");
            System.err.println("   " + e.getMessage());
            System.err.println("⚠️ Бот будет работать без базы данных (режим кэша)");
            dataSource = null;
            initialized = false;
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized || dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Источник данных не инициализирован");
        }
        return dataSource.getConnection();
    }

    public static boolean isAvailable() {
        return initialized && dataSource != null && !dataSource.isClosed();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("🔌 Пул соединений закрыт");
        }
    }

    public static boolean testConnection() {
        if (!isAvailable()) {
            return false;
        }

        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Ошибка проверки подключения: " + e.getMessage());
            return false;
        }
    }

    public static void reinitialize() {
        close();
        initDataSource();
    }

    public static boolean isInitialized() {
        return initialized;
    }
}