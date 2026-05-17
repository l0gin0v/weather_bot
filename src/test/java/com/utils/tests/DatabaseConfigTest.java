package com.utils.tests;

import com.utils.config.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseConfigTest {

    private HikariDataSource originalDataSource;
    private boolean originalInitialized;

    @BeforeEach
    void setUp() throws Exception {
        originalDataSource = (HikariDataSource) getStaticField("dataSource");
        originalInitialized = (boolean) getStaticField("initialized");
    }

    @AfterEach
    void tearDown() throws Exception {
        setStaticField("dataSource", originalDataSource);
        setStaticField("initialized", originalInitialized);
    }

    @Test
    void testGetConnection_success() throws Exception {
        HikariDataSource ds = mock(HikariDataSource.class);
        Connection conn = mock(Connection.class);
        when(ds.isClosed()).thenReturn(false);
        when(ds.getConnection()).thenReturn(conn);

        setStaticField("dataSource", ds);
        setStaticField("initialized", true);

        Connection got = DatabaseConfig.getConnection();
        assertSame(conn, got);
        verify(ds).getConnection();
    }

    @Test
    void testGetConnection_notInitialized_throws() throws Exception {
        setStaticField("dataSource", null);
        setStaticField("initialized", false);
        assertThrows(SQLException.class, DatabaseConfig::getConnection);
    }

    @Test
    void testIsAvailable_trueAndFalse() throws Exception {
        HikariDataSource ds = mock(HikariDataSource.class);
        when(ds.isClosed()).thenReturn(false);
        setStaticField("dataSource", ds);
        setStaticField("initialized", true);
        assertTrue(DatabaseConfig.isAvailable());

        when(ds.isClosed()).thenReturn(true);
        assertFalse(DatabaseConfig.isAvailable());
    }

    @Test
    void testClose_callsCloseWhenOpen() throws Exception {
        HikariDataSource ds = mock(HikariDataSource.class);
        when(ds.isClosed()).thenReturn(false);
        setStaticField("dataSource", ds);
        setStaticField("initialized", true);

        DatabaseConfig.close();

        verify(ds).close();
    }

    @Test
    void testTestConnection_success() throws Exception {
        HikariDataSource ds = mock(HikariDataSource.class);
        Connection conn = mock(Connection.class);
        when(ds.isClosed()).thenReturn(false);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.isClosed()).thenReturn(false);

        setStaticField("dataSource", ds);
        setStaticField("initialized", true);

        assertTrue(DatabaseConfig.testConnection());
        verify(ds).getConnection();
        verify(conn).isClosed();
        verify(conn).close();
    }

    @Test
    void testTestConnection_failureOnGetConnection() throws Exception {
        HikariDataSource ds = mock(HikariDataSource.class);
        when(ds.isClosed()).thenReturn(false);
        when(ds.getConnection()).thenThrow(new SQLException("no conn"));

        setStaticField("dataSource", ds);
        setStaticField("initialized", true);

        assertFalse(DatabaseConfig.testConnection());
        verify(ds).getConnection();
    }

    private static Object getStaticField(String fieldName) throws Exception {
        Field f = findField(DatabaseConfig.class, fieldName);
        f.setAccessible(true);
        return f.get(null);
    }

    private static void setStaticField(String fieldName, Object value) throws Exception {
        Field f = findField(DatabaseConfig.class, fieldName);
        f.setAccessible(true);
        try {
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        } catch (NoSuchFieldException ignored) { }
        f.set(null, value);
    }


    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> current = cls;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ex) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
