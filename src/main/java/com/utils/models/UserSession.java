package com.utils.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class UserSession {
    private Long userId;
    private String city;
    private String state;
    private boolean isActive;
    private LocalTime notificationTime;
    private boolean notificationsEnabled;
    private LocalDate lastNotificationSent;
    private LocalDateTime lastActivity;
    private LocalDateTime createdAt;

    public UserSession() {}

    public UserSession(Long userId, String city, String state, boolean isActive) {
        this.userId = userId;
        this.city = city;
        this.state = state;
        this.isActive = isActive;
        this.lastActivity = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.notificationsEnabled = false;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalTime getNotificationTime() { return notificationTime; }
    public void setNotificationTime(LocalTime notificationTime) {
        this.notificationTime = notificationTime;
    }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public LocalDate getLastNotificationSent() { return lastNotificationSent; }
    public void setLastNotificationSent(LocalDate lastNotificationSent) {
        this.lastNotificationSent = lastNotificationSent;
    }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean hasNotification() {
        return notificationsEnabled && notificationTime != null && city != null;
    }

    public void enableNotifications(String city, LocalTime time) {
        this.city = city;
        this.notificationTime = time;
        this.notificationsEnabled = true;
    }

    public void disableNotifications() {
        this.notificationsEnabled = false;
        this.notificationTime = null;
    }
}