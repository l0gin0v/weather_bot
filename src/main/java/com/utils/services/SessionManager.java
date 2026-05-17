package com.utils.services;

import com.utils.dao.UserSessionDAO;
import com.utils.models.UserSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final UserSessionDAO sessionDAO;
    private final Map<Long, UserSession> cache;
    private boolean dbAvailable = false;

    public SessionManager() {
        this.sessionDAO = new UserSessionDAO();
        this.cache = new ConcurrentHashMap<>();

        System.out.println("🔧 Инициализация менеджера сессий с уведомлениями...");

        try {
            sessionDAO.createTableIfNotExists();
            dbAvailable = true;
            System.out.println("✅ База данных подключена");

            // Загружаем активные сессии с уведомлениями в кэш
            loadSessionsWithNotifications();

        } catch (Exception e) {
            System.err.println("❌ Ошибка при инициализации БД: " + e.getMessage());
            System.err.println("   Работаем в режиме кэша");
            dbAvailable = false;
        }
    }

    private void loadSessionsWithNotifications() {
        if (!dbAvailable) return;

        List<UserSession> sessions = sessionDAO.findSessionsWithNotifications();
        for (UserSession session : sessions) {
            cache.put(session.getUserId(), session);
        }
        System.out.println("✅ Загружено " + sessions.size() + " сессий с уведомлениями");
    }

    // === МЕТОДЫ ДЛЯ УВЕДОМЛЕНИЙ ===

    public void enableNotifications(Long userId, String city, LocalTime time) {
        getSession(userId).ifPresentOrElse(
                session -> {
                    session.enableNotifications(city, time);
                    saveSession(session);
                },
                () -> {
                    // Создаем новую сессию с уведомлениями
                    UserSession session = new UserSession(userId, city, "DEFAULT", true);
                    session.enableNotifications(city, time);
                    saveSession(session);
                }
        );
    }

    public void disableNotifications(Long userId) {
        getSession(userId).ifPresent(session -> {
            session.disableNotifications();
            saveSession(session);
        });
    }

    public Optional<UserSession> getSessionWithNotification(Long userId) {
        return getSession(userId)
                .filter(UserSession::hasNotification);
    }

    public List<UserSession> getAllSessionsWithNotifications() {
        if (dbAvailable) {
            return sessionDAO.findSessionsWithNotifications();
        } else {
            // Фильтруем из кэша
            return cache.values().stream()
                    .filter(UserSession::hasNotification)
                    .toList();
        }
    }

    public void updateLastNotificationSent(Long userId, LocalDate date) {
        getSession(userId).ifPresent(session -> {
            session.setLastNotificationSent(date);
            saveSession(session);

            if (dbAvailable) {
                sessionDAO.updateLastNotificationSent(userId, date);
            }
        });
    }

    public Optional<LocalTime> getNotificationTime(Long userId) {
        return getSession(userId)
                .map(UserSession::getNotificationTime);
    }

    public Optional<String> getNotificationCity(Long userId) {
        return getSession(userId)
                .map(UserSession::getCity);
    }

    public boolean hasNotification(Long userId) {
        return getSession(userId)
                .map(UserSession::hasNotification)
                .orElse(false);
    }

    // === ОСНОВНЫЕ МЕТОДЫ СЕССИЙ ===

    // Создание/обновление сессии
    public void createOrUpdateSession(Long userId, String city, String state, boolean isActive) {
        UserSession session = new UserSession(userId, city, state, isActive);
        saveSession(session);
    }

    // Получение сессии
    public Optional<UserSession> getSession(Long userId) {
        // Проверяем кэш
        if (cache.containsKey(userId)) {
            return Optional.of(cache.get(userId));
        }

        // Если нет в кэше, ищем в БД
        if (dbAvailable) {
            Optional<UserSession> session = sessionDAO.findById(userId);
            session.ifPresent(s -> cache.put(userId, s));
            return session;
        }

        return Optional.empty();
    }

    // Проверка активности сессии
    public boolean isSessionActive(Long userId) {
        return getSession(userId)
                .map(UserSession::isActive)
                .orElse(false);
    }

    // Обновление города
    public void updateCity(Long userId, String city) {
        getSession(userId).ifPresent(session -> {
            session.setCity(city);
            saveSession(session);
        });
    }

    // Обновление состояния
    public void updateState(Long userId, String state) {
        getSession(userId).ifPresent(session -> {
            session.setState(state);
            session.setLastActivity(LocalDateTime.now());
            saveSession(session);
        });
    }

    // Активация сессии
    public void activateSession(Long userId, String city) {
        createOrUpdateSession(userId, city, "DEFAULT", true);
    }

    // Деактивация сессии
    public void deactivateSession(Long userId) {
        if (dbAvailable) {
            sessionDAO.deactivateSession(userId);
        }
        cache.remove(userId);
    }

    // Обновление времени активности
    public void updateActivity(Long userId) {
        getSession(userId).ifPresent(session -> {
            session.setLastActivity(LocalDateTime.now());
            saveSession(session);

            if (dbAvailable) {
                sessionDAO.updateActivity(userId);
            }
        });
    }

    // Получение всех активных сессий
    public List<UserSession> getAllActiveSessions() {
        if (dbAvailable) {
            return sessionDAO.getActiveSessions();
        } else {
            return cache.values().stream()
                    .filter(UserSession::isActive)
                    .toList();
        }
    }

    // Очистка старых сессий
    public void cleanupOldSessions(int daysOld) {
        if (dbAvailable) {
            sessionDAO.cleanupOldSessions(daysOld);
        }
        // Очищаем кэш от неактивных сессий
        cache.entrySet().removeIf(entry ->
                !entry.getValue().isActive() &&
                        entry.getValue().getLastActivity().isBefore(LocalDateTime.now().minusDays(daysOld))
        );
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private void saveSession(UserSession session) {
        cache.put(session.getUserId(), session);

        if (dbAvailable) {
            new Thread(() -> sessionDAO.saveOrUpdate(session)).start();
        }
    }

    // Получение текущего города пользователя
    public Optional<String> getCurrentCity(Long userId) {
        return getSession(userId)
                .map(UserSession::getCity);
    }

    // Получение текущего состояния пользователя
    public Optional<String> getCurrentState(Long userId) {
        return getSession(userId)
                .map(UserSession::getState);
    }

    // Получение времени последней активности
    public Optional<LocalDateTime> getLastActivity(Long userId) {
        return getSession(userId)
                .map(UserSession::getLastActivity);
    }

    // Получение времени создания сессии
    public Optional<LocalDateTime> getCreatedAt(Long userId) {
        return getSession(userId)
                .map(UserSession::getCreatedAt);
    }

    // Проверка, доступна ли БД
    public boolean isDatabaseAvailable() {
        return dbAvailable;
    }

    // Очистка кэша (для тестирования)
    public void clearCache() {
        cache.clear();
    }

    // Получение статистики
    public String getStats() {
        int cachedSessions = cache.size();
        int activeSessions = (int) cache.values().stream()
                .filter(UserSession::isActive)
                .count();
        int sessionsWithNotifications = (int) cache.values().stream()
                .filter(UserSession::hasNotification)
                .count();

        return String.format(
                "📊 Статистика SessionManager:\n" +
                        "Сессий в кэше: %d\n" +
                        "Активных сессий: %d\n" +
                        "С уведомлениями: %d\n" +
                        "База данных: %s",
                cachedSessions, activeSessions, sessionsWithNotifications,
                dbAvailable ? "✅ подключена" : "❌ недоступна"
        );
    }
}