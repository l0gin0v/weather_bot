package com.utils.tests;

import com.utils.models.OpenMeteoResponse;
import com.utils.models.Coordinates;
import com.utils.models.Daily;
import com.utils.services.Geocoding;
import com.utils.services.WeatherAPI;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherAPITest {

    @Mock
    private Geocoding mockGeocoding;

    @Mock
    private OkHttpClient mockClient;

    @Mock
    private Call mockCall;

    @Mock
    private Response mockResponse;

    @Mock
    private ResponseBody mockResponseBody;

    private WeatherAPI weatherAPI;

    @BeforeEach
    void setUp() {
        weatherAPI = new WeatherAPI(mockGeocoding);

        try {
            var clientField = WeatherAPI.class.getDeclaredField("client");
            clientField.setAccessible(true);
            clientField.set(weatherAPI, mockClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void constructor_WithGeocoding_ShouldSetDependency() {
        WeatherAPI apiWithGeocoding = new WeatherAPI(mockGeocoding);
        assertNotNull(apiWithGeocoding);
    }

    @Test
    void constructor_WithoutParameters_ShouldCreateDefaultGeocoding() {
        WeatherAPI api = new WeatherAPI();
        assertNotNull(api);
    }

    @Test
    void getGeocoding_ShouldReturnGeocodingInstance() {
        assertNotNull(weatherAPI.getGeocoding());
    }

    @Test
    void getWeather_WithValidCoordinates_ShouldReturnResponse() throws IOException {
        String jsonResponse = """
            {
                "daily": {
                    "time": ["2025-10-01", "2025-10-02"],
                    "temperature_2m_max": [20.5, 22.0],
                    "temperature_2m_min": [10.5, 12.0],
                    "weathercode": [0, 1],
                    "windspeed_10m_max": [15.0, 18.0],
                    "precipitation_probability_max": [30.0, 40.0]
                }
            }
            """;

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn(jsonResponse);

        OpenMeteoResponse result = weatherAPI.getWeather(55.7558, 37.6173, 2);

        assertNotNull(result);
        assertNotNull(result.getDaily());
        assertEquals(2, result.getDaily().getTime().size());
        assertEquals(20.5, result.getDaily().getTemperature2mMax().get(0));
        assertEquals(10.5, result.getDaily().getTemperature2mMin().get(0));
    }

    @Test
    void getWeather_WithHttpError_ShouldThrowIOException() throws IOException {
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(false);
        when(mockResponse.code()).thenReturn(500);

        IOException exception = assertThrows(IOException.class,
                () -> weatherAPI.getWeather(55.7558, 37.6173, 2));
        assertTrue(exception.getMessage().contains("500"));
    }

    @Test
    void getWeatherByCity_WithValidCity_ShouldReturnResponse() throws IOException {
        Coordinates coordinates = new Coordinates(55.7558, 37.6173, "Москва");
        String jsonResponse = """
            {
                "daily": {
                    "time": ["2025-10-01"],
                    "temperature_2m_max": [20.5],
                    "temperature_2m_min": [10.5],
                    "weathercode": [0],
                    "windspeed_10m_max": [15.0],
                    "precipitation_probability_max": [30.0]
                }
            }
            """;

        when(mockGeocoding.getCoordinates("Москва")).thenReturn(coordinates);
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn(jsonResponse);

        OpenMeteoResponse result = weatherAPI.getWeatherByCity("Москва", 1);

        assertNotNull(result);
        verify(mockGeocoding, times(1)).getCoordinates("Москва");
    }

    @Test
    void getWeatherByCity_WithInvalidCity_ShouldThrowIOException() throws IOException {
        when(mockGeocoding.getCoordinates("НесуществующийГород"))
                .thenThrow(new IOException("Город не найден"));

        assertThrows(IOException.class,
                () -> weatherAPI.getWeatherByCity("НесуществующийГород", 1));
    }

    @Test
    void getTomorrowWeather_WithValidCity_ShouldReturnResponse() throws IOException {
        Coordinates coordinates = new Coordinates(55.7558, 37.6173, "Москва");
        String jsonResponse = """
            {
                "daily": {
                    "time": ["2025-10-01", "2025-10-02"],
                    "temperature_2m_max": [20.5, 22.0],
                    "temperature_2m_min": [10.5, 12.0],
                    "weathercode": [0, 1],
                    "windspeed_10m_max": [15.0, 18.0],
                    "precipitation_probability_max": [30.0, 40.0]
                }
            }
            """;

        when(mockGeocoding.getCoordinates("Москва")).thenReturn(coordinates);
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn(jsonResponse);

        OpenMeteoResponse result = weatherAPI.getTomorrowWeather("Москва");

        assertNotNull(result);
        assertEquals(2, result.getDaily().getTime().size());
        verify(mockGeocoding, times(1)).getCoordinates("Москва");
    }

    @Test
    void getWeatherCondition_WithVariousCodes_ShouldReturnCorrectDescriptions() {
        assertEquals("☀️ Ясно", weatherAPI.getWeatherCondition(0));
        assertEquals("🌤 Преимущественно ясно", weatherAPI.getWeatherCondition(1));
        assertEquals("⛅️ Переменная облачность", weatherAPI.getWeatherCondition(2));
        assertEquals("☁️ Пасмурно", weatherAPI.getWeatherCondition(3));
        assertEquals("🌫 Туман", weatherAPI.getWeatherCondition(45));
        assertEquals("🌦 Морось", weatherAPI.getWeatherCondition(51));
        assertEquals("🌧 Дождь", weatherAPI.getWeatherCondition(61));
        assertEquals("❄️ Снег", weatherAPI.getWeatherCondition(71));
        assertEquals("⛈ Гроза", weatherAPI.getWeatherCondition(95));
        assertEquals("❓ Неизвестно", weatherAPI.getWeatherCondition(999));
        assertEquals("🌨 Ледяная морось", weatherAPI.getWeatherCondition(56));
        assertEquals("🌨 Ледяной дождь", weatherAPI.getWeatherCondition(66));
        assertEquals("🌦 Ливень", weatherAPI.getWeatherCondition(80));
        assertEquals("🌨 Снегопад", weatherAPI.getWeatherCondition(85));
    }

    @Test
    void getWeather_DaysMoreThan7_ShouldLimitTo7() throws IOException {
        String jsonResponse = """
            {
                "daily": {
                    "time": ["2025-10-01", "2025-10-02", "2025-10-03", "2025-10-04", 
                            "2025-10-05", "2025-10-06", "2025-10-07"],
                    "temperature_2m_max": [20.5, 21.0, 22.0, 23.0, 24.0, 25.0, 26.0],
                    "temperature_2m_min": [10.5, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0],
                    "weathercode": [0, 1, 2, 3, 0, 1, 2],
                    "windspeed_10m_max": [15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0],
                    "precipitation_probability_max": [30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0]
                }
            }
            """;

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn(jsonResponse);

        OpenMeteoResponse result = weatherAPI.getWeather(55.7558, 37.6173, 10);

        assertNotNull(result);
        assertEquals(7, result.getDaily().getTime().size());
    }

    private OpenMeteoResponse createMockWeatherResponse() {
        OpenMeteoResponse response = new OpenMeteoResponse();
        Daily daily = new Daily();

        daily.setTime(Arrays.asList(LocalDate.now().toString()));
        daily.setTemperature2mMax(Arrays.asList(20.0));
        daily.setTemperature2mMin(Arrays.asList(10.0));
        daily.setWeatherCode(Arrays.asList(0));
        daily.setWindspeed10mMax(Arrays.asList(15.0));
        daily.setPrecipitationProbabilityMax(Arrays.asList(30.0));

        response.setDaily(daily);
        return response;
    }

    private OpenMeteoResponse createMockWeatherResponseForMultipleDays() {
        OpenMeteoResponse response = new OpenMeteoResponse();
        Daily daily = new Daily();

        LocalDate today = LocalDate.now();
        daily.setTime(Arrays.asList(
                today.toString(),
                today.plusDays(1).toString(),
                today.plusDays(2).toString()
        ));
        daily.setTemperature2mMax(Arrays.asList(20.0, 22.0, 18.0));
        daily.setTemperature2mMin(Arrays.asList(10.0, 12.0, 8.0));
        daily.setWeatherCode(Arrays.asList(0, 1, 2));
        daily.setWindspeed10mMax(Arrays.asList(15.0, 18.0, 12.0));
        daily.setPrecipitationProbabilityMax(Arrays.asList(30.0, 40.0, 20.0));

        response.setDaily(daily);
        return response;
    }
}