package com.utils.tests;

import com.utils.interfaces.INotificationClient;
import com.utils.models.UserSession;
import com.utils.services.NotificationScheduler;
import com.utils.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationSchedulerTest {

    private NotificationService notificationServiceMock;
    private INotificationClient notificationClientMock;
    private NotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        notificationServiceMock = mock(NotificationService.class);
        notificationClientMock = mock(INotificationClient.class);
        scheduler = new NotificationScheduler(notificationServiceMock, notificationClientMock);
    }

    @Test
    void testCheckAndSendNotifications_sendsNotification_whenConditionsMet() throws Exception {
        UserSession session = mock(UserSession.class);
        when(session.getUserId()).thenReturn(1L);
        when(session.getLastNotificationSent()).thenReturn(null);
        LocalTime now = LocalTime.now();
        when(session.getNotificationTime()).thenReturn(now);
        when(notificationClientMock.isUserSessionActive(1L)).thenReturn(true);
        when(notificationServiceMock.getSessionsForNotificationCheck()).thenReturn(List.of(session));
        when(notificationServiceMock.getWeatherNotification(1L)).thenReturn("Погода норм");

        Method m = NotificationScheduler.class.getDeclaredMethod("checkAndSendNotifications");
        m.setAccessible(true);
        m.invoke(scheduler);

        verify(notificationClientMock).sendNotificationToUser(1L, "Погода норм");
        verify(notificationServiceMock).markNotificationSent(1L);
    }

    @Test
    void testCheckAndSendNotifications_doesNotSend_whenSessionInactive() throws Exception {
        UserSession session = mock(UserSession.class);
        when(session.getUserId()).thenReturn(2L);
        when(session.getLastNotificationSent()).thenReturn(null);
        when(session.getNotificationTime()).thenReturn(LocalTime.now());
        when(notificationClientMock.isUserSessionActive(2L)).thenReturn(false);
        when(notificationServiceMock.getSessionsForNotificationCheck()).thenReturn(List.of(session));

        Method m = NotificationScheduler.class.getDeclaredMethod("checkAndSendNotifications");
        m.setAccessible(true);
        m.invoke(scheduler);

        verify(notificationClientMock, never()).sendNotificationToUser(anyLong(), anyString());
        verify(notificationServiceMock, never()).markNotificationSent(anyLong());
    }

    @Test
    void testCheckAndSendNotifications_doesNotSend_whenAlreadySentToday() throws Exception {
        UserSession session = mock(UserSession.class);
        when(session.getUserId()).thenReturn(3L);
        when(session.getLastNotificationSent()).thenReturn(LocalDate.now());
        when(session.getNotificationTime()).thenReturn(LocalTime.now());
        when(notificationClientMock.isUserSessionActive(3L)).thenReturn(true);
        when(notificationServiceMock.getSessionsForNotificationCheck()).thenReturn(List.of(session));

        Method m = NotificationScheduler.class.getDeclaredMethod("checkAndSendNotifications");
        m.setAccessible(true);
        m.invoke(scheduler);

        verify(notificationClientMock, never()).sendNotificationToUser(anyLong(), anyString());
        verify(notificationServiceMock, never()).markNotificationSent(anyLong());
    }


    @Test
    void testCheckAndSendNotifications_doesNotSend_whenWeatherError() throws Exception {
        UserSession session = mock(UserSession.class);
        when(session.getUserId()).thenReturn(4L);
        when(session.getLastNotificationSent()).thenReturn(null);
        when(session.getNotificationTime()).thenReturn(LocalTime.now());
        when(notificationClientMock.isUserSessionActive(4L)).thenReturn(true);
        when(notificationServiceMock.getSessionsForNotificationCheck()).thenReturn(List.of(session));
        when(notificationServiceMock.getWeatherNotification(4L)).thenReturn("❌ Ошибка");

        Method m = NotificationScheduler.class.getDeclaredMethod("checkAndSendNotifications");
        m.setAccessible(true);
        m.invoke(scheduler);

        verify(notificationClientMock, never()).sendNotificationToUser(anyLong(), anyString());
        verify(notificationServiceMock, never()).markNotificationSent(anyLong());
    }

    @Test
    void testIsTimeToSend_trueAndFalse() throws Exception {
        Method m = NotificationScheduler.class.getDeclaredMethod("isTimeToSend", LocalTime.class, LocalTime.class);
        m.setAccessible(true);
        LocalTime now = LocalTime.now();
        boolean close = (boolean) m.invoke(scheduler, now, now);
        assertTrue(close);
        LocalTime far = now.plusMinutes(5);
        boolean farResult = (boolean) m.invoke(scheduler, now, far);
        assertFalse(farResult);
    }

    @Test
    void testClearNotificationHistory_removesEntry() throws Exception {
        Field f = NotificationScheduler.class.getDeclaredField("lastNotificationSent");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Long, LocalDate> map = (ConcurrentHashMap<Long, LocalDate>) f.get(scheduler);
        map.put(999L, LocalDate.now());
        assertTrue(map.containsKey(999L));
        scheduler.clearNotificationHistory(999L);
        assertFalse(map.containsKey(999L));
    }

    @Test
    void testCheckAndSendNotifications_multipleSessions_mixedBehavior() throws Exception {
        UserSession s1 = mock(UserSession.class);
        when(s1.getUserId()).thenReturn(10L);
        when(s1.getLastNotificationSent()).thenReturn(null);
        when(s1.getNotificationTime()).thenReturn(LocalTime.now());
        UserSession s2 = mock(UserSession.class);
        when(s2.getUserId()).thenReturn(11L);
        when(s2.getLastNotificationSent()).thenReturn(LocalDate.now());
        when(s2.getNotificationTime()).thenReturn(LocalTime.now());
        when(notificationServiceMock.getSessionsForNotificationCheck()).thenReturn(List.of(s1, s2));
        when(notificationClientMock.isUserSessionActive(10L)).thenReturn(true);
        when(notificationClientMock.isUserSessionActive(11L)).thenReturn(true);
        when(notificationServiceMock.getWeatherNotification(10L)).thenReturn("ОК");

        Method m = NotificationScheduler.class.getDeclaredMethod("checkAndSendNotifications");
        m.setAccessible(true);
        m.invoke(scheduler);

        verify(notificationClientMock).sendNotificationToUser(10L, "ОК");
        verify(notificationServiceMock).markNotificationSent(10L);
        verify(notificationClientMock, never()).sendNotificationToUser(eq(11L), anyString());
    }
}
