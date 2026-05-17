package com.utils.services;

import com.utils.interfaces.INotificationClient;
import com.utils.models.Notification;
import com.utils.models.UserSession;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationScheduler implements Runnable {
    private final NotificationService notificationService;
    private final INotificationClient notificationClient;
    private final ConcurrentHashMap<Long, LocalDate> lastNotificationSent = new ConcurrentHashMap<>();

    public NotificationScheduler(NotificationService notificationService, INotificationClient notificationClient) {
        this.notificationService = notificationService;
        this.notificationClient = notificationClient;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                checkAndSendNotifications();
                Thread.sleep(30000); // Проверка каждые 30 секунд
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Ошибка в NotificationScheduler: " + e.getMessage());
            }
        }
    }

    private void checkAndSendNotifications() {
        try {
            // Получаем все сессии с уведомлениями через NotificationService
            List<UserSession> sessions = notificationService.getSessionsForNotificationCheck();

            for (UserSession session : sessions) {
                Long userId = session.getUserId();

                // Проверяем активна ли сессия
                if (notificationClient.isUserSessionActive(userId)) {

                    LocalDate today = LocalDate.now();
                    LocalDate lastSent = session.getLastNotificationSent();

                    if (lastSent != null && lastSent.equals(today)) {
                        continue; // Уже отправляли сегодня
                    }

                    LocalTime now = LocalTime.now();
                    LocalTime notificationTime = session.getNotificationTime();

                    if (isTimeToSend(now, notificationTime)) {
                        String notificationText = notificationService.getWeatherNotification(userId);

                        if (notificationText != null && !notificationText.startsWith("❌")) {
                            notificationClient.sendNotificationToUser(userId, notificationText);

                            // Помечаем как отправленное
                            notificationService.markNotificationSent(userId);

                            System.out.println("Отправлено уведомление для userId: " + userId + " в " + now);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при проверке уведомлений: " + e.getMessage());
        }
    }

    private boolean isTimeToSend(LocalTime now, LocalTime notificationTime) {
        long nowSeconds = now.toSecondOfDay();
        long notificationSeconds = notificationTime.toSecondOfDay();
        long diff = Math.abs(nowSeconds - notificationSeconds);

        return diff <= 60; // Отправляем если разница меньше 60 секунд
    }

    public void clearNotificationHistory(Long chatId) {
        lastNotificationSent.remove(chatId);
    }
}