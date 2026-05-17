package com.utils.interfaces;

public interface INotificationClient {

    boolean isUserSessionActive(long userId);

    void sendNotificationToUser(long userId, String notificationText);

    String getClientName();
}