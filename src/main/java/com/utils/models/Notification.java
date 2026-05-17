package com.utils.models;

import java.time.LocalTime;

public class Notification {
    private final long chatId;
    private String city;
    private LocalTime time;
    private boolean isActive;

    public Notification(long chatId, String city, LocalTime time) {
        this.chatId = chatId;
        this.city = city;
        this.time = time;
        this.isActive = true;
    }

    public long getChatId() { return chatId; }
    public String getCity() { return city; }
    public LocalTime getTime() { return time; }
    public boolean isActive() { return isActive; }

    public void setActive(boolean active) { isActive = active; }
    public void setTime(LocalTime time) { this.time = time; }
    public void setCity(String city) { this.city = city; }

    @Override
    public String toString() {
        return String.format("Notification{chatId=%d, city='%s', time=%s, active=%s}",
                chatId, city, time, isActive);
    }
}