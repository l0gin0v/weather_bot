package com.utils.tests;

import com.utils.interfaces.INotificationClient;
import com.utils.services.*;
import com.utils.models.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private WeatherAPI mockWeatherAPI;

    @Mock
    private WeatherFormatter mockWeatherFormatter;

    @Mock
    private SessionManager mockSessionManager;

    @Mock
    private INotificationClient mockNotificationClient;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(mockWeatherAPI, mockWeatherFormatter, mockSessionManager);
    }

    @Test
    void setNotification_shouldSetNotificationSuccessfully() throws Exception {
        // Arrange
        long chatId = 12345L;
        String city = "Moscow";
        String timeString = "09:00";
        LocalTime time = LocalTime.parse(timeString);

        when(mockWeatherAPI.getWeatherByCity(city, 1)).thenReturn(null);

        // Act
        String result = notificationService.setNotification(chatId, city, timeString);

        // Assert
        assertTrue(result.contains("✅ Уведомление установлено и сохранено в сессии!"));
        verify(mockSessionManager).enableNotifications(chatId, city, time);
    }

    @Test
    void setNotification_shouldReturnErrorForInvalidTimeFormat() {
        // Arrange
        long chatId = 12345L;
        String city = "Moscow";
        String invalidTime = "25:00";

        // Act
        String result = notificationService.setNotification(chatId, city, invalidTime);

        // Assert
        assertTrue(result.contains("❌ Ошибка"));
        verify(mockSessionManager, never()).enableNotifications(anyLong(), anyString(), any());
    }

    @Test
    void setNotification_shouldReturnErrorForInvalidCity() throws Exception {
        // Arrange
        long chatId = 12345L;
        String city = "InvalidCity";
        String timeString = "09:00";

        when(mockWeatherAPI.getWeatherByCity(city, 1))
                .thenThrow(new RuntimeException("City not found"));

        // Act
        String result = notificationService.setNotification(chatId, city, timeString);

        // Assert
        assertTrue(result.contains("❌ Ошибка"));
        verify(mockSessionManager, never()).enableNotifications(anyLong(), anyString(), any());
    }

    @Test
    void hasNotification_shouldReturnTrueWhenNotificationExists() {
        // Arrange
        long chatId = 12345L;
        when(mockSessionManager.hasNotification(chatId)).thenReturn(true);

        // Act
        boolean result = notificationService.hasNotification(chatId);

        // Assert
        assertTrue(result);
        verify(mockSessionManager).hasNotification(chatId);
    }

    @Test
    void hasNotification_shouldReturnFalseWhenNoNotification() {
        // Arrange
        long chatId = 12345L;
        when(mockSessionManager.hasNotification(chatId)).thenReturn(false);

        // Act
        boolean result = notificationService.hasNotification(chatId);

        // Assert
        assertFalse(result);
    }

    @Test
    void getWeatherNotification_shouldReturnWeatherWhenCityExists() throws Exception {
        // Arrange
        long chatId = 12345L;
        String city = "Moscow";
        String weatherText = "Погода в Москве: +20°C";

        when(mockSessionManager.getNotificationCity(chatId)).thenReturn(Optional.of(city));
        when(mockWeatherFormatter.getQuickWeather(city)).thenReturn(weatherText);

        // Act
        String result = notificationService.getWeatherNotification(chatId);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("🔔 Ежедневная погода для"));
        assertTrue(result.contains(weatherText));
    }

    @Test
    void getWeatherNotification_shouldReturnNullWhenNoCity() {
        // Arrange
        long chatId = 12345L;
        when(mockSessionManager.getNotificationCity(chatId)).thenReturn(Optional.empty());

        // Act
        String result = notificationService.getWeatherNotification(chatId);

        // Assert
        assertNull(result);
    }

    @Test
    void getWeatherNotification_shouldHandleExceptionWhenWeatherApiFails() throws Exception {
        // Arrange
        long chatId = 12345L;
        String city = "Moscow";

        when(mockSessionManager.getNotificationCity(chatId)).thenReturn(Optional.of(city));
        when(mockWeatherFormatter.getQuickWeather(city))
                .thenThrow(new RuntimeException("API error"));

        // Act
        String result = notificationService.getWeatherNotification(chatId);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("❌ Ошибка при получении погоды"));
        assertTrue(result.contains(city));
    }

    @Test
    void cancelNotification_shouldCallDisableNotifications() {
        // Arrange
        long chatId = 12345L;

        // Act
        String result = notificationService.cancelNotification(chatId);

        // Assert
        assertEquals("❌ Уведомление отменено", result);
        verify(mockSessionManager).disableNotifications(chatId);
    }

    @Test
    void getNotificationInfo_shouldReturnInfoWhenNotificationExists() {
        // Arrange
        long chatId = 12345L;
        UserSession mockSession = mock(UserSession.class);
        when(mockSession.getCity()).thenReturn("Moscow");
        when(mockSession.getNotificationTime()).thenReturn(LocalTime.of(9, 0));
        when(mockSessionManager.getSessionWithNotification(chatId)).thenReturn(Optional.of(mockSession));

        // Act
        String result = notificationService.getNotificationInfo(chatId);

        // Assert
        assertTrue(result.contains("🔔 Активное уведомление (в сессии)"));
        assertTrue(result.contains("Город: Moscow"));
        assertTrue(result.contains("Время: 09:00"));
    }

    @Test
    void getNotificationInfo_shouldReturnNoNotificationMessage() {
        // Arrange
        long chatId = 12345L;
        when(mockSessionManager.getSessionWithNotification(chatId)).thenReturn(Optional.empty());

        // Act
        String result = notificationService.getNotificationInfo(chatId);

        // Assert
        assertEquals("❌ У вас нет активных уведомлений", result);
    }

    @Test
    void getActiveNotifications_shouldReturnSetOfUserIds() {
        // Arrange
        UserSession session1 = mock(UserSession.class);
        UserSession session2 = mock(UserSession.class);
        when(session1.getUserId()).thenReturn(1L);
        when(session2.getUserId()).thenReturn(2L);
        List<UserSession> sessions = Arrays.asList(session1, session2);
        when(mockSessionManager.getAllSessionsWithNotifications()).thenReturn(sessions);

        // Act
        Set<Long> result = notificationService.getActiveNotifications();

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));
    }

    @Test
    void markNotificationSent_shouldUpdateLastNotificationSent() {
        // Arrange
        long chatId = 12345L;
        LocalDate today = LocalDate.now();

        // Act
        notificationService.markNotificationSent(chatId);

        // Assert
        verify(mockSessionManager).updateLastNotificationSent(chatId, today);
    }

    @Test
    void getLastNotificationSent_shouldReturnDate() {
        // Arrange
        long chatId = 12345L;
        LocalDate expectedDate = LocalDate.of(2023, 10, 5);
        UserSession mockSession = mock(UserSession.class);
        when(mockSession.getLastNotificationSent()).thenReturn(expectedDate);
        when(mockSessionManager.getSession(chatId)).thenReturn(Optional.of(mockSession));

        // Act
        LocalDate result = notificationService.getLastNotificationSent(chatId);

        // Assert
        assertEquals(expectedDate, result);
    }

    @Test
    void getLastNotificationSent_shouldReturnNullWhenNoSession() {
        // Arrange
        long chatId = 12345L;
        when(mockSessionManager.getSession(chatId)).thenReturn(Optional.empty());

        // Act
        LocalDate result = notificationService.getLastNotificationSent(chatId);

        // Assert
        assertNull(result);
    }

    @Test
    void getSessionsForNotificationCheck_shouldReturnSessions() {
        // Arrange
        List<UserSession> expectedSessions = Arrays.asList(mock(UserSession.class), mock(UserSession.class));
        when(mockSessionManager.getAllSessionsWithNotifications()).thenReturn(expectedSessions);

        // Act
        List<UserSession> result = notificationService.getSessionsForNotificationCheck();

        // Assert
        assertEquals(expectedSessions, result);
    }

    @Test
    void getSessionsForNotificationCheck_shouldReturnEmptyListWhenNoNotifications() {
        // Arrange
        when(mockSessionManager.getAllSessionsWithNotifications()).thenReturn(Collections.emptyList());

        // Act
        List<UserSession> result = notificationService.getSessionsForNotificationCheck();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void setNotification_shouldHandleLocalTimeParseException() {
        // Arrange
        long chatId = 12345L;
        String city = "Moscow";
        String invalidTime = "invalid-time";

        // Act
        String result = notificationService.setNotification(chatId, city, invalidTime);

        // Assert
        assertTrue(result.contains("❌ Ошибка"));
        assertTrue(result.contains("Неверный формат времени"));
    }
}