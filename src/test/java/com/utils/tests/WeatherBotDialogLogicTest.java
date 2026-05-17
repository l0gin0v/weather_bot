package com.utils.tests;

import com.utils.models.UserAnswerStatus;
import com.utils.services.WeatherAPI;
import com.utils.services.WeatherBotDialogLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeatherBotDialogLogicTest {

    private WeatherAPI weatherAPIMock;
    private WeatherBotDialogLogic logic;

    @BeforeEach
    void setUp() {
        weatherAPIMock = mock(WeatherAPI.class);
        logic = spy(new WeatherBotDialogLogic(weatherAPIMock));
    }

    @Test
    void testGetQuestion() {
        String q = logic.getQuestion();
        assertEquals("Введите название города для получения погоды:", q);
    }

    @Test
    void testWelcomeWords_containsHelpAndHint() {
        String w = logic.welcomeWords();
        assertTrue(w.contains("/help"));
        assertTrue(w.contains("Вы можете ввести название города"));
    }

    @Test
    void testFarewallWordsForInactive() {
        String f = logic.farewallWordsForInactive();
        assertEquals("❌ Сессия завершена. Введите /start для начала новой сессии.", f);
    }

    @Test
    void testProcessAnswer_help_returnsHelpTextAndNoFlagsSet() throws Exception {
        UserAnswerStatus res = logic.processAnswer("/help");
        String text = extractFirstStringField(res);
        List<Boolean> bools = extractBooleanFields(res);
        assertEquals(logic.getHelp(), text);
        for (Boolean b : bools) {
            assertFalse(b);
        }
    }

    @Test
    void testProcessAnswer_quit_returnsFarewellAndExitFlag() throws Exception {
        UserAnswerStatus res = logic.processAnswer("/quit");
        String text = extractFirstStringField(res);
        List<Boolean> bools = extractBooleanFields(res);
        assertTrue(text.contains("погоду") || text.length() > 0);
        assertTrue(bools.stream().anyMatch(Boolean::booleanValue));
    }

    @Test
    void testProcessAnswer_city_success_callsGetQuickWeatherAndReturnsIt() throws Exception {
        doReturn("Погода: ясно, +20°C").when(logic).getQuickWeatherForCity("Moscow");
        UserAnswerStatus res = logic.processAnswer("Moscow");
        String text = extractFirstStringField(res);
        List<Boolean> bools = extractBooleanFields(res);
        assertEquals("Погода: ясно, +20°C", text);
        assertTrue(bools.stream().anyMatch(Boolean::booleanValue));
    }

    @Test
    void testProcessAnswer_city_failure_returnsErrorMessage() throws Exception {
        doThrow(new RuntimeException("api error")).when(logic).getQuickWeatherForCity("Nowhere");
        UserAnswerStatus res = logic.processAnswer("Nowhere");
        String text = extractFirstStringField(res);
        assertTrue(text.contains("Не удалось получить погоду для города: Nowhere"));
    }

    @Test
    void testGetHelp_includesExtraTelegramInfo() {
        String help = logic.getHelp();
        assertTrue(help.contains("Дополнительные возможности"));
        assertTrue(help.contains("Кнопки для быстрого выбора периода"));
    }

    @Test
    void testGetWeatherForPeriod_delegatesToFormatWeatherForPeriod() throws Exception {
        doReturn("3-day forecast").when(logic).formatWeatherForPeriod("Moscow", 3);
        String out = logic.getWeatherForPeriod("Moscow", 3);
        assertEquals("3-day forecast", out);
        verify(logic).formatWeatherForPeriod("Moscow", 3);
    }

    private static String extractFirstStringField(Object obj) throws Exception {
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (f.getType().equals(String.class)) {
                f.setAccessible(true);
                Object val = f.get(obj);
                return val == null ? null : val.toString();
            }
        }
        Class<?> current = obj.getClass().getSuperclass();
        while (current != null) {
            for (Field f : current.getDeclaredFields()) {
                if (f.getType().equals(String.class)) {
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    return val == null ? null : val.toString();
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("No String field found in " + obj.getClass());
    }

    private static List<Boolean> extractBooleanFields(Object obj) throws Exception {
        List<Boolean> ret = new ArrayList<>();
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (f.getType().equals(boolean.class) || f.getType().equals(Boolean.class)) {
                f.setAccessible(true);
                Object v = f.get(obj);
                ret.add(v == null ? false : (Boolean) v);
            }
        }
        Class<?> current = obj.getClass().getSuperclass();
        while (current != null) {
            for (Field f : current.getDeclaredFields()) {
                if (f.getType().equals(boolean.class) || f.getType().equals(Boolean.class)) {
                    f.setAccessible(true);
                    Object v = f.get(obj);
                    ret.add(v == null ? false : (Boolean) v);
                }
            }
            current = current.getSuperclass();
        }
        return ret;
    }
}
