package com.utils.services;

import com.utils.models.UserSession;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class NotificationService {
    private final WeatherAPI weatherAPI;
    private final WeatherFormatter weatherFormatter;
    private final SessionManager sessionManager;

    public NotificationService(WeatherAPI weatherAPI, WeatherFormatter weatherFormatter,
                               SessionManager sessionManager) {
        this.weatherAPI = weatherAPI;
        this.weatherFormatter = weatherFormatter;
        this.sessionManager = sessionManager;

        System.out.println("🔔 Сервис уведомлений инициализирован");
        System.out.println("   Использует SessionManager для хранения уведомлений");
    }

    public String setNotification(long chatId, String city, String timeString) {
        try {
            if (!timeString.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                throw new IllegalArgumentException("Неверный формат времени");
            }

            LocalTime time = LocalTime.parse(timeString);

            // Проверяем, что город существует
            weatherAPI.getWeatherByCity(city, 1);

            // Сохраняем в сессии через SessionManager
            sessionManager.enableNotifications(chatId, city, time);

            return String.format(
                    "✅ Уведомление установлено и сохранено в сессии!\n" +
                            "🏙 Город: %s\n" +
                            "⏰ Время: %s\n\n" +
                            "Каждый день в это время вы будете получать прогноз погоды.\n" +
                            "Уведомление сохранится после перезапуска бота.",
                    city, time
            );

        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage() +
                    "\nИспользуйте формат HH:MM и существующий город";
        }
    }

    public boolean hasNotification(long chatId) {
        return sessionManager.hasNotification(chatId);
    }

    public String getWeatherNotification(long chatId) {
        return sessionManager.getNotificationCity(chatId)
                .map(city -> {
                    try {
                        String weather = weatherFormatter.getQuickWeather(city);
                        return String.format(
                                "🔔 Ежедневная погода для %s:\n\n%s",
                                city, weather
                        );
                    } catch (Exception e) {
                        return String.format(
                                "❌ Ошибка при получении погоды для %s: %s",
                                city, e.getMessage()
                        );
                    }
                })
                .orElse(null);
    }

    public String cancelNotification(long chatId) {
        sessionManager.disableNotifications(chatId);
        return "❌ Уведомление отменено";
    }

    public String getNotificationInfo(long chatId) {
        return sessionManager.getSessionWithNotification(chatId)
                .map(session -> String.format(
                        "🔔 Активное уведомление (в сессии):\nГород: %s\nВремя: %s",
                        session.getCity(),
                        session.getNotificationTime()
                ))
                .orElse("❌ У вас нет активных уведомлений");
    }

    public Set<Long> getActiveNotifications() {
        // Получаем всех пользователей с уведомлениями
        return sessionManager.getAllSessionsWithNotifications().stream()
                .map(UserSession::getUserId)
                .collect(Collectors.toSet());
    }

    public void markNotificationSent(long chatId) {
        sessionManager.updateLastNotificationSent(chatId, java.time.LocalDate.now());
    }

    public LocalDate getLastNotificationSent(long chatId) {
        return sessionManager.getSession(chatId)
                .map(UserSession::getLastNotificationSent)
                .orElse(null);
    }

    // Метод для NotificationScheduler
    public List<UserSession> getSessionsForNotificationCheck() {
        return sessionManager.getAllSessionsWithNotifications();
    }
}