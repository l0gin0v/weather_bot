package com.utils.tests;

import com.utils.models.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void constructor_ShouldInitializeWithCorrectValues() {
        long chatId = 123456789L;
        String city = "Москва";
        LocalTime time = LocalTime.of(9, 0);

        Notification notification = new Notification(chatId, city, time);

        assertEquals(chatId, notification.getChatId());
        assertEquals(city, notification.getCity());
        assertEquals(time, notification.getTime());
        assertTrue(notification.isActive());
    }

    @Test
    void constructor_WithDifferentTimeFormats_ShouldWorkCorrectly() {
        LocalTime[] times = {
                LocalTime.of(0, 0),
                LocalTime.of(12, 30),
                LocalTime.of(23, 59),
                LocalTime.of(9, 15),
                LocalTime.of(18, 45)
        };

        for (LocalTime time : times) {
            Notification notification = new Notification(111L, "City", time);
            assertEquals(time, notification.getTime());
        }
    }

    @Test
    void constructor_WithDifferentChatIds_ShouldWorkCorrectly() {
        long[] chatIds = {
                1L,
                123L,
                123456789012345L,
                -123456L,
                0L
        };

        for (long chatId : chatIds) {
            Notification notification = new Notification(chatId, "City", LocalTime.MIDNIGHT);
            assertEquals(chatId, notification.getChatId());
        }
    }

    @Test
    void setActive_ShouldChangeActivityStatus() {
        Notification notification = new Notification(123L, "City", LocalTime.NOON);

        assertTrue(notification.isActive());

        notification.setActive(false);
        assertFalse(notification.isActive());

        notification.setActive(true);
        assertTrue(notification.isActive());
    }

    @ParameterizedTest
    @CsvSource({
            "9, 0",
            "12, 30",
            "18, 45",
            "23, 59",
            "0, 1"
    })
    void setTime_ShouldUpdateTime(int hour, int minute) {
        Notification notification = new Notification(123L, "City", LocalTime.of(8, 0));

        LocalTime newTime = LocalTime.of(hour, minute);
        notification.setTime(newTime);

        assertEquals(newTime, notification.getTime());
    }

    @Test
    void setTime_WithSameTime_ShouldWork() {
        LocalTime time = LocalTime.of(10, 30);
        Notification notification = new Notification(123L, "City", time);

        notification.setTime(time);

        assertEquals(time, notification.getTime());
    }

    @Test
    void setCity_ShouldUpdateCity() {
        Notification notification = new Notification(123L, "Москва", LocalTime.NOON);

        assertEquals("Москва", notification.getCity());

        notification.setCity("Санкт-Петербург");
        assertEquals("Санкт-Петербург", notification.getCity());

        notification.setCity("New York");
        assertEquals("New York", notification.getCity());

        notification.setCity("東京");
        assertEquals("東京", notification.getCity());
    }

    @Test
    void setCity_WithNull_ShouldWork() {
        Notification notification = new Notification(123L, "Москва", LocalTime.NOON);

        notification.setCity(null);

        assertNull(notification.getCity());
    }

    @Test
    void setCity_WithEmptyString_ShouldWork() {
        Notification notification = new Notification(123L, "Москва", LocalTime.NOON);

        notification.setCity("");

        assertEquals("", notification.getCity());
    }

    @Test
    void setCity_WithSpecialCharacters_ShouldWork() {
        Notification notification = new Notification(123L, "Москва", LocalTime.NOON);

        String specialCity = "São Paulo - Cidade!@#$%^&*()";
        notification.setCity(specialCity);

        assertEquals(specialCity, notification.getCity());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Notification{chatId=123456789, city='Москва', time=09:00, active=true}",
            "Notification{chatId=987654321, city='Санкт-Петербург', time=18:30, active=false}",
            "Notification{chatId=111, city='New York', time=12:00, active=true}",
            "Notification{chatId=0, city='', time=00:00, active=false}"
    })
    void toString_ShouldReturnCorrectFormat(String expected) {
        String[] parts = expected
                .replace("Notification{", "")
                .replace("}", "")
                .split(", ");

        long chatId = Long.parseLong(parts[0].split("=")[1]);
        String city = parts[1].split("=")[1].replace("'", "");
        LocalTime time = LocalTime.parse(parts[2].split("=")[1]);
        boolean active = Boolean.parseBoolean(parts[3].split("=")[1]);

        Notification notification = new Notification(chatId, city, time);
        notification.setActive(active);

        assertEquals(expected, notification.toString());
    }

    @Test
    void toString_FormatShouldBeConsistent() {
        Notification notification1 = new Notification(123456789L, "Москва", LocalTime.of(9, 0));
        notification1.setActive(true);

        Notification notification2 = new Notification(987654321L, "Санкт-Петербург", LocalTime.of(18, 30));
        notification2.setActive(false);

        String expected1 = "Notification{chatId=123456789, city='Москва', time=09:00, active=true}";
        String expected2 = "Notification{chatId=987654321, city='Санкт-Петербург', time=18:30, active=false}";

        assertEquals(expected1, notification1.toString());
        assertEquals(expected2, notification2.toString());
    }

    @Test
    void notification_ShouldBeMutable() {
        Notification notification = new Notification(111L, "Москва", LocalTime.of(8, 0));

        notification.setCity("Санкт-Петербург");
        notification.setTime(LocalTime.of(18, 30));
        notification.setActive(false);

        assertEquals(111L, notification.getChatId());
        assertEquals("Санкт-Петербург", notification.getCity());
        assertEquals(LocalTime.of(18, 30), notification.getTime());
        assertFalse(notification.isActive());
    }

    @Test
    void equalsAndHashCode_ShouldConsiderAllFields() {
        Notification notification1 = new Notification(123L, "Москва", LocalTime.of(9, 0));
        Notification notification2 = new Notification(123L, "Москва", LocalTime.of(9, 0));
        Notification notification3 = new Notification(456L, "Москва", LocalTime.of(9, 0));
        Notification notification4 = new Notification(123L, "Санкт-Петербург", LocalTime.of(9, 0));
        Notification notification5 = new Notification(123L, "Москва", LocalTime.of(10, 0));

        assertEquals(notification1.toString(), notification2.toString());
        assertNotEquals(notification1.toString(), notification3.toString());
        assertNotEquals(notification1.toString(), notification4.toString());
        assertNotEquals(notification1.toString(), notification5.toString());
    }

    @Test
    void notification_WithMaxValues_ShouldWork() {
        long maxChatId = Long.MAX_VALUE;
        String longCityName = "A".repeat(1000);
        LocalTime maxTime = LocalTime.MAX;

        Notification notification = new Notification(maxChatId, longCityName, maxTime);

        assertEquals(maxChatId, notification.getChatId());
        assertEquals(longCityName, notification.getCity());
        assertEquals(maxTime, notification.getTime());
        assertTrue(notification.isActive());
    }
}