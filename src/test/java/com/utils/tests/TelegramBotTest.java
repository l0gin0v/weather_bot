package com.utils.tests;

import com.utils.models.Coordinates;
import com.utils.models.UserSession;
import com.utils.services.Geocoding;
import com.utils.services.NotificationService;
import com.utils.services.SessionManager;
import com.utils.services.TelegramBot;
import com.utils.services.WeatherBotDialogLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TelegramBotTest {

    private static final long CHAT_ID = 12345L;

    private TelegramBot bot;
    private SessionManager sessionManagerMock;
    private Geocoding geocodingMock;
    private NotificationService notificationServiceMock;
    private WeatherBotDialogLogic weatherBotDialogLogicMock;

    @BeforeEach
    void setUp() throws Exception {
        ObjenesisStd objenesis = new ObjenesisStd();
        TelegramBot plain = (TelegramBot) objenesis.newInstance(TelegramBot.class);
        bot = spy(plain);

        sessionManagerMock = mock(SessionManager.class);
        geocodingMock = mock(Geocoding.class);
        notificationServiceMock = mock(NotificationService.class);
        weatherBotDialogLogicMock = mock(WeatherBotDialogLogic.class);

        setPrivateField(bot, "sessionManager", sessionManagerMock);
        setPrivateField(bot, "geocodingService", geocodingMock);
        setPrivateField(bot, "notificationService", notificationServiceMock);
        setPrivateField(bot, "weatherBotDialogLogic", weatherBotDialogLogicMock);

        Message fakeMessage = mock(Message.class);
        doReturn(fakeMessage).when(bot).execute(any(SendMessage.class));

        when(sessionManagerMock.isSessionActive(CHAT_ID)).thenReturn(true);
    }

    @Test
    void testIsValidTimeFormat_validAndInvalid() throws Exception {
        Method m = TelegramBot.class.getDeclaredMethod("isValidTimeFormat", String.class);
        m.setAccessible(true);

        assertTrue((boolean) m.invoke(bot, "09:00"));
        assertTrue((boolean) m.invoke(bot, "23:59"));
        assertFalse((boolean) m.invoke(bot, "24:00"));
        assertFalse((boolean) m.invoke(bot, "9:0"));
        assertFalse((boolean) m.invoke(bot, "ab:cd"));
    }

    @Test
    void testHandleNotificationTimeInput_success_setsNotificationAndResetsState() throws Exception {
        UserSession sessionMock = mock(UserSession.class);
        when(sessionMock.getState()).thenReturn("WAITING_FOR_NOTIFICATION_TIME");
        when(sessionMock.getCity()).thenReturn("Москва");
        when(sessionManagerMock.getSession(CHAT_ID)).thenReturn(Optional.of(sessionMock));
        when(sessionManagerMock.isSessionActive(CHAT_ID)).thenReturn(true);

        when(notificationServiceMock.setNotification(CHAT_ID, "Москва", "09:00"))
                .thenReturn("✅ Уведомление установлено на 09:00");

        Method m = TelegramBot.class.getDeclaredMethod("handleNotificationTimeInput", long.class, String.class);
        m.setAccessible(true);
        m.invoke(bot, CHAT_ID, "09:00");

        verify(notificationServiceMock).setNotification(CHAT_ID, "Москва", "09:00");
        verify(sessionManagerMock).updateState(CHAT_ID, "DEFAULT");
        verify(bot, atLeastOnce()).execute(any(SendMessage.class));
    }

    @Test
    void testHandleNotificationTimeInput_invalidFormat_sendsError() throws Exception {
        UserSession sessionMock = mock(UserSession.class);
        when(sessionMock.getState()).thenReturn("WAITING_FOR_NOTIFICATION_TIME");
        when(sessionManagerMock.getSession(CHAT_ID)).thenReturn(Optional.of(sessionMock));
        when(sessionManagerMock.isSessionActive(CHAT_ID)).thenReturn(true);

        Method m = TelegramBot.class.getDeclaredMethod("handleNotificationTimeInput", long.class, String.class);
        m.setAccessible(true);
        m.invoke(bot, CHAT_ID, "25:99");

        verify(notificationServiceMock, never()).setNotification(anyLong(), anyString(), anyString());
        verify(bot, atLeastOnce()).execute(any(SendMessage.class));
    }

    @Test
    void testHandleCityInputState_success_updatesCityAndSendsConfirmation() throws Exception {
        Coordinates coords = mock(Coordinates.class);
        when(coords.getDisplayName()).thenReturn("Москва, Россия");
        when(geocodingMock.getCoordinates("Москва")).thenReturn(coords);
        when(sessionManagerMock.isSessionActive(CHAT_ID)).thenReturn(true);

        Method m = TelegramBot.class.getDeclaredMethod("handleCityInputState", long.class, String.class);
        m.setAccessible(true);
        m.invoke(bot, CHAT_ID, "Москва");

        verify(sessionManagerMock).updateCity(CHAT_ID, "Москва");
        verify(sessionManagerMock).updateState(CHAT_ID, "DEFAULT");
        verify(bot, atLeastOnce()).execute(any(SendMessage.class));
    }

    @Test
    void testHandleCityInputState_failedToFindCity_sendsError() throws Exception {
        when(geocodingMock.getCoordinates("Narnia")).thenThrow(new RuntimeException("not found"));
        when(sessionManagerMock.isSessionActive(CHAT_ID)).thenReturn(true);

        Method m = TelegramBot.class.getDeclaredMethod("handleCityInputState", long.class, String.class);
        m.setAccessible(true);
        m.invoke(bot, CHAT_ID, "Narnia");

        verify(sessionManagerMock, never()).updateCity(anyLong(), anyString());
        verify(bot, atLeastOnce()).execute(any(SendMessage.class));
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = findFieldInHierarchy(target.getClass(), fieldName);
        field.setAccessible(true);
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        } catch (NoSuchFieldException ignored) {
        }
        field.set(target, value);
    }

    private static Field findFieldInHierarchy(Class<?> cls, String fieldName) throws NoSuchFieldException {
        Class<?> current = cls;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
