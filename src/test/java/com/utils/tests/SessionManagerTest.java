package com.utils.tests;

import com.utils.dao.UserSessionDAO;
import com.utils.models.UserSession;
import com.utils.services.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionManagerTest {

    private SessionManager manager;
    private UserSessionDAO daoMock;

    @BeforeEach
    void setUp() throws Exception {
        ObjenesisStd objenesis = new ObjenesisStd();
        manager = (SessionManager) objenesis.newInstance(SessionManager.class);
        daoMock = mock(UserSessionDAO.class);
        setPrivateField(manager, "sessionDAO", daoMock);
        setPrivateField(manager, "cache", new ConcurrentHashMap<Long, UserSession>());
        setPrivateField(manager, "dbAvailable", false);
    }

    @Test
    void testCreateOrUpdateAndGetSession() {
        manager.createOrUpdateSession(1L, "Moscow", "DEFAULT", true);
        Optional<UserSession> s = manager.getSession(1L);
        assertTrue(s.isPresent());
        assertEquals("Moscow", manager.getCurrentCity(1L).orElse(null));
        assertEquals("DEFAULT", manager.getCurrentState(1L).orElse(null));
        assertTrue(manager.isSessionActive(1L));
    }

    @Test
    void testEnableNotifications_newSession() {
        manager.enableNotifications(3L, "Kazan", LocalTime.of(8, 30));
        Optional<UserSession> s = manager.getSessionWithNotification(3L);
        assertTrue(s.isPresent());
        assertEquals("Kazan", manager.getNotificationCity(3L).orElse(null));
        assertTrue(manager.hasNotification(3L));
    }

    @Test
    void testDisableNotifications() {
        manager.createOrUpdateSession(4L, "Omsk", "DEFAULT", true);
        manager.enableNotifications(4L, "Omsk", LocalTime.of(7, 0));
        assertTrue(manager.hasNotification(4L));
        manager.disableNotifications(4L);
        assertFalse(manager.hasNotification(4L));
    }

    @Test
    void testUpdateLastNotificationSent_dbUnavailable_noDaoCall() throws Exception {
        UserSession sessionMock = mock(UserSession.class);
        when(sessionMock.getUserId()).thenReturn(5L);
        Map<Long, UserSession> cache = getCache();
        cache.put(5L, sessionMock);
        manager.updateLastNotificationSent(5L, java.time.LocalDate.now());
        verify(sessionMock).setLastNotificationSent(any());
        verify(daoMock, never()).updateLastNotificationSent(anyLong(), any());
    }

    @Test
    void testUpdateLastNotificationSent_dbAvailable_callsDao() throws Exception {
        setPrivateField(manager, "dbAvailable", true);
        UserSession sessionMock = mock(UserSession.class);
        when(sessionMock.getUserId()).thenReturn(6L);
        Map<Long, UserSession> cache = getCache();
        cache.put(6L, sessionMock);
        manager.updateLastNotificationSent(6L, java.time.LocalDate.now());
        verify(sessionMock).setLastNotificationSent(any());
        verify(daoMock).updateLastNotificationSent(eq(6L), any());
    }

    @Test
    void testGetAllSessionsWithNotifications_fromCache() {
        manager.createOrUpdateSession(7L, "A", "S", true);
        manager.createOrUpdateSession(8L, "B", "S", true);
        manager.enableNotifications(7L, "A", LocalTime.of(10, 0));
        var list = manager.getAllSessionsWithNotifications();
        assertTrue(list.size() >= 1);
    }

    @Test
    void testDeactivateSession_dbAvailable_callsDaoAndRemovesFromCache() throws Exception {
        setPrivateField(manager, "dbAvailable", true);
        UserSession session = new UserSession(9L, "C", "DEFAULT", true);
        Map<Long, UserSession> cache = getCache();
        cache.put(9L, session);
        manager.deactivateSession(9L);
        verify(daoMock).deactivateSession(9L);
        assertFalse(cache.containsKey(9L));
    }

    @Test
    void testUpdateCityStateActivity() {
        manager.createOrUpdateSession(10L, "X", "OLD", true);
        manager.updateCity(10L, "Y");
        assertEquals("Y", manager.getCurrentCity(10L).orElse(null));
        manager.updateState(10L, "NEW");
        assertEquals("NEW", manager.getCurrentState(10L).orElse(null));
        manager.updateActivity(10L);
        assertTrue(manager.getLastActivity(10L).isPresent());
    }

    @Test
    void testGetAllActiveSessions_cacheMode() {
        manager.createOrUpdateSession(11L, "a", "s", true);
        manager.createOrUpdateSession(12L, "b", "s", false);
        var list = manager.getAllActiveSessions();
        assertTrue(list.stream().allMatch(UserSession::isActive));
    }

    @Test
    void testCleanupOldSessions_removesOldInactive() throws Exception {
        Map<Long, UserSession> cache = getCache();
        UserSession oldMock = mock(UserSession.class);
        when(oldMock.isActive()).thenReturn(false);
        when(oldMock.getLastActivity()).thenReturn(LocalDateTime.now().minusDays(10));
        when(oldMock.getUserId()).thenReturn(20L);
        cache.put(20L, oldMock);
        UserSession freshMock = mock(UserSession.class);
        when(freshMock.isActive()).thenReturn(true);
        when(freshMock.getLastActivity()).thenReturn(LocalDateTime.now());
        when(freshMock.getUserId()).thenReturn(21L);
        cache.put(21L, freshMock);
        manager.cleanupOldSessions(5);
        assertFalse(cache.containsKey(20L));
        assertTrue(cache.containsKey(21L));
    }

    @Test
    void testGetStats_reflectsCacheAndDbFlag() {
        manager.clearCache();
        manager.createOrUpdateSession(30L, "s1", "st", true);
        manager.createOrUpdateSession(31L, "s2", "st", false);
        String stats = manager.getStats();
        assertTrue(stats.contains("Сессий в кэше: 2"));
        assertTrue(stats.contains("База данных: ❌ недоступна"));
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = findFieldInHierarchy(target.getClass(), fieldName);
        f.setAccessible(true);
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        } catch (NoSuchFieldException ignored) { }
        f.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, UserSession> getCache() throws Exception {
        Field f = findFieldInHierarchy(SessionManager.class, "cache");
        f.setAccessible(true);
        return (Map<Long, UserSession>) f.get(manager);
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
