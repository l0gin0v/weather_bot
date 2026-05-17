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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // Делаем Mockito менее строгим
class DialogLogicTest {

    @Mock
    private WeatherAPI mockWeatherAPI;

    @Mock
    private WeatherFormatter mockWeatherFormatter;

    @Mock
    private Geocoding mockGeocoding;

    private DialogLogic dialogLogic;

    @BeforeEach
    void setUp() {
        dialogLogic = new DialogLogic(mockWeatherAPI);

        // Используем рефлексию для подмены weatherFormatter на мок
        try {
            var baseClass = dialogLogic.getClass().getSuperclass();
            var field = baseClass.getDeclaredField("weatherFormatter");
            field.setAccessible(true);
            field.set(dialogLogic, mockWeatherFormatter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(mockWeatherAPI.getGeocoding()).thenReturn(mockGeocoding);
    }

    @Test
    void getQuestion_shouldAskForCity_whenCurrentCityIsNull() {
        // Act
        String result = dialogLogic.getQuestion();

        // Assert
        assertEquals("Введите название города для получения погоды:", result);
    }

    @Test
    void getQuestion_shouldShowMenu_whenCurrentCityIsSet() {
        // Arrange
        dialogLogic = new DialogLogic(mockWeatherAPI);
        setCurrentCityViaReflection("Moscow");

        // Act
        String result = dialogLogic.getQuestion();

        // Assert
        assertTrue(result.contains("Ваш текущий город: Moscow"));
        assertTrue(result.contains("1 - Погода сегодня"));
        assertTrue(result.contains("2 - Погода завтра"));
        assertTrue(result.contains("3 - Погода на 3 дня"));
        assertTrue(result.contains("4 - Погода на неделю"));
        assertTrue(result.contains("5 - Сменить город"));
    }

    @Test
    void welcomeWords_shouldReturnExtendedMessage() {
        // Act
        String result = dialogLogic.welcomeWords();

        // Assert
        assertTrue(result.contains("Добро пожаловать в погодный бот!"));
        assertTrue(result.contains("Вы можете ввести название города"));
        assertTrue(result.contains("=========================="));
    }

    @Test
    void processAnswer_shouldReturnHelp_whenHelpCommand() {
        // Act
        UserAnswerStatus result = dialogLogic.processAnswer("/help");

        // Assert
        assertFalse(result.isCorrectAnswer);
        assertTrue(result.message.contains("Погодный бот - справка"));
        assertFalse(result.isQuit);
    }

    @Test
    void processAnswer_shouldReturnFarewell_whenQuitCommand() {
        // Act
        UserAnswerStatus result = dialogLogic.processAnswer("/quit");

        // Assert
        assertFalse(result.isCorrectAnswer);
        assertTrue(result.message.contains("До свидания"));
        assertTrue(result.isQuit);
    }

    @Test
    void processAnswer_shouldSetCityAndReturnWeather_whenValidCityEntered() throws Exception {
        // Arrange
        String city = "Moscow";
        String weatherResponse = "☀️ Погода в Москве: 20°C";
        when(mockWeatherFormatter.getQuickWeather(city)).thenReturn(weatherResponse);

        // Act
        UserAnswerStatus result = dialogLogic.processAnswer(city);

        // Assert
        assertTrue(result.isCorrectAnswer);
        assertTrue(result.message.contains("Город установлен: Moscow"));
        assertTrue(result.message.contains(weatherResponse));
        assertFalse(result.isQuit);
        verify(mockWeatherFormatter).getQuickWeather(city);
    }

    @Test
    void processAnswer_shouldReturnError_whenInvalidCityEntered() throws Exception {
        // Arrange
        String invalidCity = "InvalidCity123";
        when(mockWeatherFormatter.getQuickWeather(invalidCity))
                .thenThrow(new RuntimeException("City not found"));

        // Act
        UserAnswerStatus result = dialogLogic.processAnswer(invalidCity);

        // Assert
        assertFalse(result.isCorrectAnswer);
        assertTrue(result.message.contains("Не удалось получить погоду"));
        assertTrue(result.message.contains(invalidCity));
        assertFalse(result.isQuit);
    }

    @Test
    void processAnswer_shouldReturnTodayWeather_whenOption1Selected() throws Exception {
        // Arrange
        setCurrentCityViaReflection("Moscow");
        String todayWeather = "Today's weather in Moscow";
        when(mockWeatherFormatter.getQuickWeather("Moscow")).thenReturn(todayWeather);

        // Act
        UserAnswerStatus result = dialogLogic.processAnswer("1");

        // Assert
        assertTrue(result.isCorrectAnswer);
        assertEquals(todayWeather, result.message);
        verify(mockWeatherFormatter).getQuickWeather("Moscow");
    }

    @Test
    void processAnswer_shouldReturnTomorrowWeather_whenOption2Selected() throws Exception {
        // Arrange
        setCurrentCityViaReflection("Moscow");
        String tomorrowWeather = "Tomorrow's weather";
        when(mockWeatherFormatter.formatTomorrowWeather("Moscow")).thenReturn(tomorrowWeather);

        // Act
        UserAnswerStatus result = dialogLogic.processAnswer("2");

        // Assert
        assertTrue(result.isCorrectAnswer);
        assertEquals(tomorrowWeather, result.message);
        verify(mockWeatherFormatter).formatTomorrowWeather("Moscow");
    }

    @Test
    void processAnswer_shouldClearCity_whenOption5Selected() {
        // Arrange
        setCurrentCityViaReflection("Moscow");

        // Act
        UserAnswerStatus result = dialogLogic.processAnswer("5");

        // Assert
        assertFalse(result.isCorrectAnswer);
        assertEquals("Введите новый город:", result.message);
        assertNull(getCurrentCityViaReflection());
    }

    @Test
    void processAnswer_shouldReturnError_whenInvalidOptionSelected() {
        // Arrange
        setCurrentCityViaReflection("Moscow");

        // Act
        UserAnswerStatus result = dialogLogic.processAnswer("invalid");

        // Assert
        assertFalse(result.isCorrectAnswer);
        assertTrue(result.message.contains("Неверный выбор"));
    }

    @Test
    void getHelp_shouldReturnCorrectHelpText() {
        // Act
        String helpText = dialogLogic.getHelp();

        // Assert
        assertTrue(helpText.contains("Погодный бот - справка"));
        assertTrue(helpText.contains("Введите название города"));
        assertTrue(helpText.contains("/help"));
        assertTrue(helpText.contains("/quit"));
    }

    @Test
    void getCurrentCity_shouldReturnNull_whenNoCitySet() {
        // Act & Assert
        assertNull(dialogLogic.getCurrentCity());
    }

    @Test
    void getCurrentCity_shouldReturnCity_whenCityIsSet() {
        // Arrange
        setCurrentCityViaReflection("Moscow");

        // Act & Assert
        assertEquals("Moscow", dialogLogic.getCurrentCity());
    }

    @Test
    void processAnswer_shouldWorkWithMixedCaseCommands() throws Exception {
        when(mockWeatherFormatter.getQuickWeather("/HELP"))
                .thenThrow(new RuntimeException("City not found"));
        when(mockWeatherFormatter.getQuickWeather("/Quit"))
                .thenThrow(new RuntimeException("City not found"));

        // Act
        UserAnswerStatus result1 = dialogLogic.processAnswer("/HELP");
        UserAnswerStatus result2 = dialogLogic.processAnswer("/Quit");

        assertFalse(result1.isCorrectAnswer);
        assertFalse(result2.isCorrectAnswer);

        assertTrue(result1.message.contains("Не удалось получить погоду"));
        assertTrue(result2.message.contains("Не удалось получить погоду"));
    }

    @Test
    void processAnswer_shouldHandleOption3For3DaysWeather() throws Exception {
        // Arrange
        setCurrentCityViaReflection("Moscow");
        Coordinates mockCoords = mock(Coordinates.class);
        OpenMeteoResponse mockResponse = mock(OpenMeteoResponse.class);

        when(mockGeocoding.getCoordinates("Moscow")).thenReturn(mockCoords);
        when(mockCoords.getDisplayName()).thenReturn("Moscow, Russia");
        when(mockWeatherAPI.getWeatherByCity("Moscow", 3)).thenReturn(mockResponse);
        when(mockWeatherFormatter.formatWeatherResponse(mockResponse, "Moscow, Russia", 3))
                .thenReturn("3-day forecast");

        // Act
        UserAnswerStatus result = dialogLogic.processAnswer("3");

        // Assert
        assertTrue(result.isCorrectAnswer);
        assertEquals("3-day forecast", result.message);
        verify(mockWeatherAPI).getWeatherByCity("Moscow", 3);
    }

    @Test
    void processAnswer_shouldHandleOption4For7DaysWeather() throws Exception {
        // Arrange
        setCurrentCityViaReflection("Moscow");
        Coordinates mockCoords = mock(Coordinates.class);
        OpenMeteoResponse mockResponse = mock(OpenMeteoResponse.class);

        when(mockGeocoding.getCoordinates("Moscow")).thenReturn(mockCoords);
        when(mockCoords.getDisplayName()).thenReturn("Moscow, Russia");
        when(mockWeatherAPI.getWeatherByCity("Moscow", 7)).thenReturn(mockResponse);
        when(mockWeatherFormatter.formatWeatherResponse(mockResponse, "Moscow, Russia", 7))
                .thenReturn("7-day forecast");

        // Act
        UserAnswerStatus result = dialogLogic.processAnswer("4");

        // Assert
        assertTrue(result.isCorrectAnswer);
        assertEquals("7-day forecast", result.message);
        verify(mockWeatherAPI).getWeatherByCity("Moscow", 7);
    }

    @Test
    void processAnswer_shouldHandleSpecialCharactersInCityName() throws Exception {
        // Arrange
        String city = "Санкт-Петербург";
        when(mockWeatherFormatter.getQuickWeather(city))
                .thenReturn("Погода в Санкт-Петербурге");

        // Act
        UserAnswerStatus result = dialogLogic.processAnswer(city);

        // Assert
        assertTrue(result.isCorrectAnswer);
        verify(mockWeatherFormatter).getQuickWeather(city);
    }

    @Test
    void fullUserFlow_shouldWorkCorrectly() throws Exception {
        // Arrange имитируем последовательность действий пользователя
        String city = "Москва";
        String todayWeather = "Сегодня в Москве +20°C";
        String tomorrowWeather = "Завтра в Москве +22°C";

        when(mockWeatherFormatter.getQuickWeather(city)).thenReturn(todayWeather);
        when(mockWeatherFormatter.formatTomorrowWeather(city)).thenReturn(tomorrowWeather);

        // Act & Assertшаг 1: пользователь вводит город
        UserAnswerStatus step1 = dialogLogic.processAnswer(city);
        assertTrue(step1.isCorrectAnswer);
        assertTrue(step1.message.contains("Город установлен"));
        assertEquals("Москва", dialogLogic.getCurrentCity());

        // Шаг 2: проверяем, что вопрос теперь показывает меню
        String questionAfterCity = dialogLogic.getQuestion();
        assertTrue(questionAfterCity.contains("Ваш текущий город: Москва"));

        // Шаг 3: пользователь запрашивает погоду на сегодня
        UserAnswerStatus step3 = dialogLogic.processAnswer("1");
        assertEquals(todayWeather, step3.message);

        // Шаг 4: пользователь запрашивает погоду на завтра
        UserAnswerStatus step4 = dialogLogic.processAnswer("2");
        assertEquals(tomorrowWeather, step4.message);

        // Шаг 5: пользователь меняет город
        String newCity = "Санкт-Петербург";
        String newCityWeather = "В Санкт-Петербурге +18°C";
        when(mockWeatherFormatter.getQuickWeather(newCity)).thenReturn(newCityWeather);

        UserAnswerStatus step5 = dialogLogic.processAnswer("5");
        assertFalse(step5.isCorrectAnswer);
        assertEquals("Введите новый город:", step5.message);
        assertNull(dialogLogic.getCurrentCity());

        // Шаг 6: ввод нового города
        UserAnswerStatus step6 = dialogLogic.processAnswer(newCity);
        assertTrue(step6.isCorrectAnswer);
        assertTrue(step6.message.contains(newCity));
        assertEquals(newCity, dialogLogic.getCurrentCity());

        // Шаг 7: выход
        UserAnswerStatus step7 = dialogLogic.processAnswer("/quit");
        assertTrue(step7.isQuit);
        assertTrue(step7.message.contains("До свидания"));
    }

    @Test
    void processAnswer_shouldHandleApiErrorsGracefully() throws Exception {
        // Arrange
        String city = "Moscow";
        when(mockWeatherFormatter.getQuickWeather(city)).thenReturn("Weather in Moscow");
        dialogLogic.processAnswer(city);

        //Сшибку API при получении погоды на 3 дня
        when(mockWeatherAPI.getWeatherByCity(city, 3))
                .thenThrow(new RuntimeException("API временно недоступен"));

        // Act
        UserAnswerStatus result = dialogLogic.processAnswer("3");

        // Assert
        assertFalse(result.isCorrectAnswer);
        assertTrue(result.message.contains("Ошибка при получении погоды"));
        assertFalse(result.isQuit);
    }

    //для доступа к приватным полям через рефлексию
    private void setCurrentCityViaReflection(String city) {
        try {
            var field = DialogLogic.class.getDeclaredField("currentCity");
            field.setAccessible(true);
            field.set(dialogLogic, city);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getCurrentCityViaReflection() {
        try {
            var field = DialogLogic.class.getDeclaredField("currentCity");
            field.setAccessible(true);
            return (String) field.get(dialogLogic);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}