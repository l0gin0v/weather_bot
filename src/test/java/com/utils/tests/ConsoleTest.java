package com.utils.tests;

import com.utils.interfaces.IDialogLogic;
import com.utils.services.*;
import com.utils.models.UserAnswerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsoleTest {
    @Mock
    private IDialogLogic mockDialogLogic;

    @Mock
    private SessionManager mockSessionManager;

    @Mock
    private NotificationService mockNotificationService;

    @Mock
    private NotificationScheduler mockNotificationScheduler;

    private Console console;
    private static final long CONSOLE_USER_ID = 1L;

    @BeforeEach
    void setUp() throws Exception {
        when(mockDialogLogic.welcomeWords()).thenReturn("Добро пожаловать!");
        when(mockDialogLogic.needToStart()).thenReturn("Введите /start");
        when(mockDialogLogic.getQuestion()).thenReturn("Введите город:");
        when(mockDialogLogic.processAnswer(anyString()))
                .thenReturn(new UserAnswerStatus(true, "Город установлен: Москва\nПогода: +20°C", false));

        console = new Console(mockDialogLogic);

        setPrivateField(console, "sessionManager", mockSessionManager);
        setPrivateField(console, "notificationService", mockNotificationService);
        setPrivateField(console, "notificationScheduler", mockNotificationScheduler);
        setPrivateField(console, "scanner", new Scanner(""));

        doNothing().when(mockSessionManager).activateSession(eq(CONSOLE_USER_ID), isNull());
        doNothing().when(mockSessionManager).deactivateSession(CONSOLE_USER_ID);
        doNothing().when(mockSessionManager).updateActivity(CONSOLE_USER_ID);
        doNothing().when(mockSessionManager).updateCity(eq(CONSOLE_USER_ID), anyString());

        when(mockSessionManager.getCurrentCity(CONSOLE_USER_ID)).thenReturn(Optional.of("Москва"));
        when(mockSessionManager.hasNotification(CONSOLE_USER_ID)).thenReturn(true);
        when(mockSessionManager.getNotificationTime(CONSOLE_USER_ID)).thenReturn(Optional.of(java.time.LocalTime.of(9, 0)));
        when(mockSessionManager.isDatabaseAvailable()).thenReturn(true);
    }

    @Test
    void isRunning_shouldReturnFalseByDefault() {
        assertFalse(console.isRunning());
    }

    @Test
    void getNotificationService_shouldReturnService() {
        NotificationService service = console.getNotificationService();
        assertNotNull(service);
        assertEquals(mockNotificationService, service);
    }

    @Test
    void getSessionManager_shouldReturnManager() {
        SessionManager manager = console.getSessionManager();
        assertNotNull(manager);
        assertEquals(mockSessionManager, manager);
    }

    @Test
    void isUserSessionActive_shouldReturnTrueWhenRunning() throws Exception {
        setPrivateField(console, "isRunning", true);
        boolean result = console.isUserSessionActive(CONSOLE_USER_ID);
        assertTrue(result);
    }

    @Test
    void isUserSessionActive_shouldReturnFalseWhenNotRunning() throws Exception {
        setPrivateField(console, "isRunning", false);
        boolean result = console.isUserSessionActive(CONSOLE_USER_ID);
        assertFalse(result);
    }

    @Test
    void isUserSessionActive_shouldReturnFalseForWrongUserId() throws Exception {
        setPrivateField(console, "isRunning", true);
        boolean result = console.isUserSessionActive(999L);
        assertFalse(result);
    }

    @Test
    void getClientName_shouldReturnConsoleBot() {
        String clientName = console.getClientName();
        assertEquals("ConsoleBot", clientName);
    }

    @Test
    void sendNotificationToUser_shouldSendToConsoleForCorrectUserId() {
        String notificationText = "Test notification";
        assertDoesNotThrow(() -> console.sendNotificationToUser(CONSOLE_USER_ID, notificationText));
    }

    @Test
    void sendNotificationToUser_shouldNotSendForWrongUserId() {
        String notificationText = "Test notification";
        assertDoesNotThrow(() -> console.sendNotificationToUser(999L, notificationText));
    }

    @Test
    void extractAndSaveCityFromResponse_shouldNotExtractIfPatternNotMatched() throws Exception {
        String response = "Просто сообщение без города";
        var method = Console.class.getDeclaredMethod("extractAndSaveCityFromResponse", String.class);
        method.setAccessible(true);
        method.invoke(console, response);
        verify(mockSessionManager, never()).updateCity(anyLong(), anyString());
    }

    @Test
    void runBot_shouldStartAndRun() throws Exception {
        Scanner mockScanner = mock(Scanner.class);
        when(mockScanner.nextLine())
                .thenReturn("/start")
                .thenReturn("Москва")
                .thenReturn("/quit");
        setPrivateField(console, "scanner", mockScanner);
        when(mockDialogLogic.processAnswer("/start"))
                .thenReturn(new UserAnswerStatus(true, "Добро пожаловать!", false));
        when(mockDialogLogic.processAnswer("Москва"))
                .thenReturn(new UserAnswerStatus(true, "Город установлен: Москва", false));
        when(mockDialogLogic.processAnswer("/quit"))
                .thenReturn(new UserAnswerStatus(false, "До свидания!", true));
        assertDoesNotThrow(() -> console.runBot());
        verify(mockSessionManager).activateSession(CONSOLE_USER_ID, null);
        verify(mockSessionManager).deactivateSession(CONSOLE_USER_ID);
    }

    @Test
    void handleNotificationMenu_shouldShowMenuWhenCityExists() throws Exception {
        Scanner mockScanner = mock(Scanner.class);
        when(mockScanner.nextLine()).thenReturn("5");
        setPrivateField(console, "scanner", mockScanner);
        var method = Console.class.getDeclaredMethod("handleNotificationMenu");
        method.setAccessible(true);
        assertDoesNotThrow(() -> method.invoke(console));
    }

    @Test
    void setNotificationTime_shouldSetTimeForValidInput() throws Exception {
        String city = "Москва";
        String validTime = "09:00";
        Scanner mockScanner = mock(Scanner.class);
        when(mockScanner.nextLine()).thenReturn(validTime);
        setPrivateField(console, "scanner", mockScanner);
        when(mockNotificationService.setNotification(CONSOLE_USER_ID, city, validTime))
                .thenReturn("Уведомление установлено");
        var method = Console.class.getDeclaredMethod("setNotificationTime", String.class);
        method.setAccessible(true);
        method.invoke(console, city);
        verify(mockNotificationService).setNotification(CONSOLE_USER_ID, city, validTime);
    }

    @Test
    void showNotificationInfo_shouldShowInfo() throws Exception {
        when(mockNotificationService.getNotificationInfo(CONSOLE_USER_ID))
                .thenReturn("Уведомление: Москва в 09:00");
        var method = Console.class.getDeclaredMethod("showNotificationInfo");
        method.setAccessible(true);
        assertDoesNotThrow(() -> method.invoke(console));
        verify(mockNotificationService).getNotificationInfo(CONSOLE_USER_ID);
    }

    @Test
    void cancelNotification_shouldCancel() throws Exception {
        when(mockNotificationService.cancelNotification(CONSOLE_USER_ID))
                .thenReturn("Уведомление отменено");
        var method = Console.class.getDeclaredMethod("cancelNotification");
        method.setAccessible(true);
        method.invoke(console);
        verify(mockNotificationService).cancelNotification(CONSOLE_USER_ID);
    }

    @Test
    void testNotification_shouldTestWhenNotificationExists() throws Exception {
        when(mockSessionManager.hasNotification(CONSOLE_USER_ID)).thenReturn(true);
        when(mockNotificationService.getWeatherNotification(CONSOLE_USER_ID))
                .thenReturn("Погода: +20°C");
        var method = Console.class.getDeclaredMethod("testNotification");
        method.setAccessible(true);
        assertDoesNotThrow(() -> method.invoke(console));
    }

    @Test
    void testNotification_shouldNotTestWhenNoNotification() throws Exception {
        when(mockSessionManager.hasNotification(CONSOLE_USER_ID)).thenReturn(false);
        var method = Console.class.getDeclaredMethod("testNotification");
        method.setAccessible(true);
        assertDoesNotThrow(() -> method.invoke(console));
    }

    @Test
    void getNotificationStatus_shouldReturnStatus() throws Exception {
        when(mockNotificationService.hasNotification(CONSOLE_USER_ID)).thenReturn(true);
        when(mockSessionManager.getNotificationTime(CONSOLE_USER_ID))
                .thenReturn(Optional.of(java.time.LocalTime.of(9, 0)));
        var method = Console.class.getDeclaredMethod("getNotificationStatus");
        method.setAccessible(true);
        String result = (String) method.invoke(console);
        assertNotNull(result);
        assertTrue(result.contains("активны"));
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}