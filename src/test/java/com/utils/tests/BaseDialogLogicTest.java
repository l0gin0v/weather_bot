package com.utils.tests;

import com.utils.services.*;
import com.utils.models.UserAnswerStatus;
import com.utils.models.Coordinates;
import com.utils.models.OpenMeteoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BaseDialogLogicTest {

    @Mock
    private WeatherAPI mockWeatherAPI;

    @Mock
    private WeatherFormatter mockWeatherFormatter;

    @Mock
    private Geocoding mockGeocoding;

    private BaseDialogLogicStub baseDialogLogic;

    // Stub класс для тестирования абстрактного класса
    private static class BaseDialogLogicStub extends BaseDialogLogic {
        public BaseDialogLogicStub(WeatherAPI weatherAPI) {
            super(weatherAPI);
        }

        @Override
        public String getQuestion() {
            return "Test question";
        }

        @Override
        public UserAnswerStatus processAnswer(String answer) {
            return new UserAnswerStatus(true, "Processed: " + answer, false);
        }

        @Override
        public String getHelp() {
            return "Test help";
        }

        // Публичнобертки для protected методов для тестирования
        public String testFarewellWords() {
            return farewellWords();
        }

        public String testGetCommonHelp() {
            return getCommonHelp();
        }

        public String testGetQuickWeatherForCity(String city) {
            return getQuickWeatherForCity(city);
        }

        public UserAnswerStatus testGetWeatherForPeriodAsStatus(String city, int days) {
            return getWeatherForPeriodAsStatus(city, days);
        }

        public String testFormatWeatherForPeriod(String city, int days) {
            return formatWeatherForPeriod(city, days);
        }
    }

    @BeforeEach
    void setUp() {
        baseDialogLogic = new BaseDialogLogicStub(mockWeatherAPI);

        try {
            Field field = BaseDialogLogic.class.getDeclaredField("weatherFormatter");
            field.setAccessible(true);
            field.set(baseDialogLogic, mockWeatherFormatter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(mockWeatherAPI.getGeocoding()).thenReturn(mockGeocoding);
    }

    @Test
    void constructor_shouldInitializeFields() {
        // Arrange & Act
        BaseDialogLogicStub logic = new BaseDialogLogicStub(mockWeatherAPI);

        // Assert
        try {
            Field weatherAPIField = BaseDialogLogic.class.getDeclaredField("weatherAPI");
            weatherAPIField.setAccessible(true);
            Field weatherFormatterField = BaseDialogLogic.class.getDeclaredField("weatherFormatter");
            weatherFormatterField.setAccessible(true);

            assertNotNull(weatherAPIField.get(logic));
            assertNotNull(weatherFormatterField.get(logic));
        } catch (Exception e) {
            fail("Failed to access fields via reflection", e);
        }
    }

    @Test
    void needToStart_shouldReturnCorrectMessage() {
        // Act
        String result = baseDialogLogic.needToStart();

        // Assert
        assertEquals("Для запуска бота введите /start", result);
    }

    @Test
    void welcomeWords_shouldReturnWelcomeMessage() {
        // Act
        String result = baseDialogLogic.welcomeWords();

        // Assert
        assertEquals("Добро пожаловать в погодный бот!", result);
    }

    @Test
    void testFarewellWords_shouldReturnFarewellMessage() {
        // Act
        String result = baseDialogLogic.testFarewellWords();

        // Assert
        assertEquals("До свидания! Возвращайтесь еще!", result);
    }

    @Test
    void testGetCommonHelp_shouldReturnHelpMessage() {
        // Act
        String result = baseDialogLogic.testGetCommonHelp();

        // Assert
        assertTrue(result.contains("📖 Помощь по боту:"));
        assertTrue(result.contains("🌤 Получить погоду:"));
        assertTrue(result.contains("🔄 Управление:"));
        assertTrue(result.contains("/help - получить справку"));
        assertTrue(result.contains("/quit - выйти из бота"));
    }

    @Test
    void testGetQuickWeatherForCity_shouldReturnWeather_whenSuccessful() throws Exception {
        // Arrange
        String city = "Moscow";
        String expectedWeather = "🌤 Погода в Москве: ...";
        when(mockWeatherFormatter.getQuickWeather(city)).thenReturn(expectedWeather);

        // Act
        String result = baseDialogLogic.testGetQuickWeatherForCity(city);

        // Assert
        assertEquals(expectedWeather, result);
        verify(mockWeatherFormatter).getQuickWeather(city);
    }

    @Test
    void testGetQuickWeatherForCity_shouldReturnErrorMessage_whenExceptionThrown() throws Exception {
        // Arrange
        String city = "InvalidCity";
        when(mockWeatherFormatter.getQuickWeather(city))
                .thenThrow(new RuntimeException("API error"));

        // Act
        String result = baseDialogLogic.testGetQuickWeatherForCity(city);

        // Assert
        assertTrue(result.contains("Не удалось получить погоду"));
        assertTrue(result.contains(city));
    }

    @Test
    void testGetWeatherForPeriodAsStatus_shouldReturnSuccessStatusFor1Day() throws Exception {
        // Arrange
        String city = "Moscow";
        String expectedWeather = "Today's weather";
        when(mockWeatherFormatter.getQuickWeather(city)).thenReturn(expectedWeather);

        // Act
        UserAnswerStatus result = baseDialogLogic.testGetWeatherForPeriodAsStatus(city, 1);

        // Assert
        assertTrue(result.isCorrectAnswer);
        assertEquals(expectedWeather, result.message);
        assertFalse(result.isQuit);
        verify(mockWeatherFormatter).getQuickWeather(city);
    }

    @Test
    void testGetWeatherForPeriodAsStatus_shouldReturnSuccessStatusFor2Days() throws Exception {
        // Arrange
        String city = "Moscow";
        String expectedWeather = "Tomorrow's weather";
        when(mockWeatherFormatter.formatTomorrowWeather(city)).thenReturn(expectedWeather);

        // Act
        UserAnswerStatus result = baseDialogLogic.testGetWeatherForPeriodAsStatus(city, 2);

        // Assert
        assertTrue(result.isCorrectAnswer);
        assertEquals(expectedWeather, result.message);
        assertFalse(result.isQuit);
        verify(mockWeatherFormatter).formatTomorrowWeather(city);
    }

    @Test
    void testGetWeatherForPeriodAsStatus_shouldReturnSuccessStatusFor3Days() throws Exception {
        // Arrange
        String city = "Moscow";
        int days = 3;

        Coordinates mockCoords = mock(Coordinates.class);
        when(mockCoords.getDisplayName()).thenReturn("Moscow, Russia");
        when(mockGeocoding.getCoordinates(city)).thenReturn(mockCoords);

        OpenMeteoResponse mockResponse = mock(OpenMeteoResponse.class);
        when(mockWeatherAPI.getWeatherByCity(city, days)).thenReturn(mockResponse);

        String formattedWeather = "3-day forecast";
        when(mockWeatherFormatter.formatWeatherResponse(mockResponse, "Moscow, Russia", days))
                .thenReturn(formattedWeather);

        // Act
        UserAnswerStatus result = baseDialogLogic.testGetWeatherForPeriodAsStatus(city, days);

        // Assert
        assertTrue(result.isCorrectAnswer);
        assertEquals(formattedWeather, result.message);
        verify(mockWeatherAPI).getWeatherByCity(city, days);
        verify(mockWeatherFormatter).formatWeatherResponse(mockResponse, "Moscow, Russia", days);
    }

    @Test
    void testGetWeatherForPeriodAsStatus_shouldReturnSuccessStatusFor7Days() throws Exception {
        // Arrange
        String city = "Moscow";
        int days = 7;

        Coordinates mockCoords = mock(Coordinates.class);
        when(mockCoords.getDisplayName()).thenReturn("Moscow, Russia");
        when(mockGeocoding.getCoordinates(city)).thenReturn(mockCoords);

        OpenMeteoResponse mockResponse = mock(OpenMeteoResponse.class);
        when(mockWeatherAPI.getWeatherByCity(city, days)).thenReturn(mockResponse);

        String formattedWeather = "7-day forecast";
        when(mockWeatherFormatter.formatWeatherResponse(mockResponse, "Moscow, Russia", days))
                .thenReturn(formattedWeather);

        // Act
        UserAnswerStatus result = baseDialogLogic.testGetWeatherForPeriodAsStatus(city, days);

        // Assert
        assertTrue(result.isCorrectAnswer);
        assertEquals(formattedWeather, result.message);
        verify(mockWeatherAPI).getWeatherByCity(city, days);
        verify(mockWeatherFormatter).formatWeatherResponse(mockResponse, "Moscow, Russia", days);
    }

    @Test
    void testGetWeatherForPeriodAsStatus_shouldHandleDefaultCase() throws Exception {
        // Arrange
        String city = "Moscow";
        String expectedWeather = "Default weather";
        when(mockWeatherFormatter.getQuickWeather(city)).thenReturn(expectedWeather);

        // Act
        UserAnswerStatus result = baseDialogLogic.testGetWeatherForPeriodAsStatus(city, 999);

        // Assert
        assertTrue(result.isCorrectAnswer);
        assertEquals(expectedWeather, result.message);
        verify(mockWeatherFormatter).getQuickWeather(city);
    }

    @Test
    void testGetWeatherForPeriodAsStatus_shouldReturnErrorStatus_whenExceptionThrown() throws Exception {
        // Arrange
        String city = "InvalidCity";
        when(mockWeatherFormatter.getQuickWeather(city))
                .thenThrow(new RuntimeException("City not found"));

        // Act
        UserAnswerStatus result = baseDialogLogic.testGetWeatherForPeriodAsStatus(city, 1);

        // Assert
        assertFalse(result.isCorrectAnswer);
        assertTrue(result.message.contains("Ошибка при получении погоды"));
        assertFalse(result.isQuit);
    }

    @Test
    void testGetWeatherForPeriodAsStatus_shouldHandleApiExceptionFor3Days() throws Exception {
        // Arrange
        String city = "Moscow";
        when(mockGeocoding.getCoordinates(city))
                .thenThrow(new RuntimeException("Geocoding API error"));

        // Act
        UserAnswerStatus result = baseDialogLogic.testGetWeatherForPeriodAsStatus(city, 3);

        // Assert
        assertFalse(result.isCorrectAnswer);
        assertTrue(result.message.contains("Ошибка при получении погоды"));
    }

    @Test
    void testFormatWeatherForPeriod_shouldFormatFor1Day() throws Exception {
        // Arrange
        String city = "Moscow";
        String expectedWeather = "1 day forecast";
        when(mockWeatherFormatter.getQuickWeather(city)).thenReturn(expectedWeather);

        // Act
        String result = baseDialogLogic.testFormatWeatherForPeriod(city, 1);

        // Assert
        assertEquals(expectedWeather, result);
        verify(mockWeatherFormatter).getQuickWeather(city);
    }

    @Test
    void testFormatWeatherForPeriod_shouldFormatFor2Days() throws Exception {
        // Arrange
        String city = "Moscow";
        String expectedWeather = "2 days forecast";
        when(mockWeatherFormatter.formatTomorrowWeather(city)).thenReturn(expectedWeather);

        // Act
        String result = baseDialogLogic.testFormatWeatherForPeriod(city, 2);

        // Assert
        assertEquals(expectedWeather, result);
        verify(mockWeatherFormatter).formatTomorrowWeather(city);
    }

    @Test
    void testFormatWeatherForPeriod_shouldFormatFor3Days() throws Exception {
        // Arrange
        String city = "Moscow";
        int days = 3;

        Coordinates mockCoords = mock(Coordinates.class);
        when(mockCoords.getDisplayName()).thenReturn("Moscow, Russia");
        when(mockGeocoding.getCoordinates(city)).thenReturn(mockCoords);

        OpenMeteoResponse mockResponse = mock(OpenMeteoResponse.class);
        when(mockWeatherAPI.getWeatherByCity(city, days)).thenReturn(mockResponse);

        String formattedWeather = "3-day formatted forecast";
        when(mockWeatherFormatter.formatWeatherResponse(mockResponse, "Moscow, Russia", days))
                .thenReturn(formattedWeather);

        // Act
        String result = baseDialogLogic.testFormatWeatherForPeriod(city, days);

        // Assert
        assertEquals(formattedWeather, result);
        verify(mockWeatherAPI).getWeatherByCity(city, days);
    }

    @Test
    void testFormatWeatherForPeriod_shouldFormatFor7Days() throws Exception {
        // Arrange
        String city = "Moscow";
        int days = 7;

        Coordinates mockCoords = mock(Coordinates.class);
        when(mockCoords.getDisplayName()).thenReturn("Moscow, Russia");
        when(mockGeocoding.getCoordinates(city)).thenReturn(mockCoords);

        OpenMeteoResponse mockResponse = mock(OpenMeteoResponse.class);
        when(mockWeatherAPI.getWeatherByCity(city, days)).thenReturn(mockResponse);

        String formattedWeather = "7-day formatted forecast";
        when(mockWeatherFormatter.formatWeatherResponse(mockResponse, "Moscow, Russia", days))
                .thenReturn(formattedWeather);

        // Act
        String result = baseDialogLogic.testFormatWeatherForPeriod(city, days);

        // Assert
        assertEquals(formattedWeather, result);
        verify(mockWeatherAPI).getWeatherByCity(city, days);
    }

    @Test
    void testFormatWeatherForPeriod_shouldReturnErrorMessage_whenExceptionThrown() throws Exception {
        // Arrange
        String city = "InvalidCity";
        when(mockWeatherFormatter.getQuickWeather(city))
                .thenThrow(new RuntimeException("API unavailable"));

        // Act
        String result = baseDialogLogic.testFormatWeatherForPeriod(city, 1);

        // Assert
        assertTrue(result.contains("Ошибка при получении погоды"));
    }

    @Test
    void testFormatWeatherForPeriod_shouldHandleDefaultCase() throws Exception {
        // Arrange
        String city = "Moscow";
        String expectedWeather = "Default forecast";
        when(mockWeatherFormatter.getQuickWeather(city)).thenReturn(expectedWeather);

        // Act
        String result = baseDialogLogic.testFormatWeatherForPeriod(city, 999);

        // Assert
        assertEquals(expectedWeather, result);
        verify(mockWeatherFormatter).getQuickWeather(city);
    }

    @Test
    void abstractMethods_shouldBeImplemented() {
        // Act
        String question = baseDialogLogic.getQuestion();
        UserAnswerStatus processResult = baseDialogLogic.processAnswer("test");
        String help = baseDialogLogic.getHelp();

        // Assert
        assertNotNull(question);
        assertNotNull(processResult);
        assertNotNull(help);
        assertEquals("Test question", question);
        assertEquals("Test help", help);
    }

    @Test
    void testGetWeatherForPeriodAsStatus_shouldHandleNullPointerInFormatter() throws Exception {
        // Arrange
        String city = "Moscow";
        when(mockWeatherFormatter.getQuickWeather(city))
                .thenThrow(new NullPointerException("Formatter is null"));

        // Act
        UserAnswerStatus result = baseDialogLogic.testGetWeatherForPeriodAsStatus(city, 1);

        // Assert
        assertFalse(result.isCorrectAnswer);
        assertTrue(result.message.contains("Ошибка при получении погоды"));
        assertTrue(result.message.contains("Formatter is null"));
    }

    @Test
    void testGetWeatherForPeriodAsStatus_shouldHandleExceptionInGeocoding() throws Exception {
        // Arrange
        String city = "NonexistentCity";
        int days = 3;

        when(mockGeocoding.getCoordinates(city))
                .thenThrow(new RuntimeException("City not found in geocoding service"));

        // Act
        UserAnswerStatus result = baseDialogLogic.testGetWeatherForPeriodAsStatus(city, days);

        // Assert
        assertFalse(result.isCorrectAnswer);
        assertTrue(result.message.contains("Ошибка при получении погоды"));
        assertTrue(result.message.contains("City not found in geocoding service"));
    }

    @Test
    void testFormatWeatherForPeriod_shouldHandleExceptionInWeatherAPI() throws Exception {
        // Arrange
        String city = "Moscow";
        int days = 3;

        when(mockWeatherAPI.getWeatherByCity(city, days))
                .thenThrow(new RuntimeException("Weather API is down"));

        // Act
        String result = baseDialogLogic.testFormatWeatherForPeriod(city, days);

        // Assert
        assertTrue(result.contains("Ошибка при получении погоды"));
        assertTrue(result.contains("Weather API is down"));
    }

    @Test
    void testFormatWeatherForPeriod_shouldHandleDifferentCityFormats() throws Exception {
        // Arrange
        String[] cities = {"London", "New York", "Paris", "Tokyo", "Berlin"};
        String expectedWeather = "Weather for city";

        for (String city : cities) {
            when(mockWeatherFormatter.getQuickWeather(city)).thenReturn(expectedWeather);

            // Act
            String result = baseDialogLogic.testFormatWeatherForPeriod(city, 1);

            // Assert
            assertEquals(expectedWeather, result);
            verify(mockWeatherFormatter).getQuickWeather(city);

            // Reset mocks для следющей итерации
            reset(mockWeatherFormatter);
        }
    }

    @Test
    void testGetWeatherForPeriodAsStatus_shouldHandleNegativeDays() throws Exception {
        // Arrange
        String city = "Moscow";
        String expectedWeather = "Default weather";
        when(mockWeatherFormatter.getQuickWeather(city)).thenReturn(expectedWeather);

        // Act
        UserAnswerStatus result = baseDialogLogic.testGetWeatherForPeriodAsStatus(city, -1);

        // Assert
        assertTrue(result.isCorrectAnswer);
        assertEquals(expectedWeather, result.message);
        verify(mockWeatherFormatter).getQuickWeather(city);
    }

    @Test
    void testGetWeatherForPeriodAsStatus_shouldHandleZeroDays() throws Exception {
        // Arrange
        String city = "Moscow";
        String expectedWeather = "Default weather";
        when(mockWeatherFormatter.getQuickWeather(city)).thenReturn(expectedWeather);

        // Act
        UserAnswerStatus result = baseDialogLogic.testGetWeatherForPeriodAsStatus(city, 0);

        // Assert
        assertTrue(result.isCorrectAnswer);
        assertEquals(expectedWeather, result.message);
        verify(mockWeatherFormatter).getQuickWeather(city);
    }

    @Test
    void testFormatWeatherForPeriod_shouldHandleNegativeDays() throws Exception {
        // Arrange
        String city = "Moscow";
        String expectedWeather = "Default forecast";
        when(mockWeatherFormatter.getQuickWeather(city)).thenReturn(expectedWeather);

        // Act
        String result = baseDialogLogic.testFormatWeatherForPeriod(city, -5);

        // Assert
        assertEquals(expectedWeather, result);
        verify(mockWeatherFormatter).getQuickWeather(city);
    }
}