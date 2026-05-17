package com.utils.tests;

import com.utils.services.Geocoding;
import com.utils.models.Coordinates;
import com.utils.models.NominatimResponse;
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
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeocodingTest {

    @Mock
    private OkHttpClient mockClient;

    @Mock
    private Call mockCall;

    @Mock
    private Response mockResponse;

    @Mock
    private ResponseBody mockResponseBody;

    private Geocoding geocoding;

    @BeforeEach
    void setUp() throws Exception {
        geocoding = new Geocoding();

        Field clientField = Geocoding.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(geocoding, mockClient);
    }

    @Test
    void getCoordinates_WithValidCity_ShouldReturnCoordinates() throws Exception {
        String jsonResponse = """
            [
                {
                    "lat": "55.7558",
                    "lon": "37.6176",
                    "display_name": "Москва, Центральный федеральный округ, Россия",
                    "type": "city",
                    "importance": 0.9
                }
            ]
            """;

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn(jsonResponse);

        Coordinates result = geocoding.getCoordinates("Москва");

        assertNotNull(result);
        assertEquals(55.7558, result.getLat(), 0.0001);
        assertEquals(37.6176, result.getLon(), 0.0001);
        assertEquals("Москва, Центральный федеральный округ, Россия", result.getDisplayName());
    }

    @Test
    void getCoordinates_WithMultipleResults_ShouldReturnFirst() throws Exception {
        String jsonResponse = """
            [
                {
                    "lat": "55.7558",
                    "lon": "37.6176",
                    "display_name": "Москва, Россия",
                    "type": "city",
                    "importance": 0.9
                },
                {
                    "lat": "55.7557",
                    "lon": "37.6175",
                    "display_name": "Московская область, Россия",
                    "type": "state",
                    "importance": 0.5
                }
            ]
            """;

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn(jsonResponse);

        Coordinates result = geocoding.getCoordinates("Москва");

        assertNotNull(result);
        assertEquals(55.7558, result.getLat(), 0.0001);
        assertEquals(37.6176, result.getLon(), 0.0001);
        assertEquals("Москва, Россия", result.getDisplayName());
    }

    @Test
    void getCoordinates_WithEmptyResponse_ShouldThrowIOException() throws Exception {
        String jsonResponse = "[]";

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn(jsonResponse);

        IOException exception = assertThrows(IOException.class,
                () -> geocoding.getCoordinates("НесуществующийГород"));

        assertEquals("Локация не найдена: НесуществующийГород", exception.getMessage());
    }

    @Test
    void getCoordinates_WithHttpError_ShouldThrowIOException() throws Exception {
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(false);
        when(mockResponse.code()).thenReturn(500);

        IOException exception = assertThrows(IOException.class,
                () -> geocoding.getCoordinates("Москва"));

        assertTrue(exception.getMessage().contains("500"));
    }

    @Test
    void getCoordinates_WithInvalidJson_ShouldThrowException() throws Exception {
        String invalidJson = "not a json";

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn(invalidJson);

        assertThrows(Exception.class,
                () -> geocoding.getCoordinates("Москва"));
    }

    @Test
    void getCoordinates_WithDifferentCityTypes_ShouldReturnCorrectCoordinates() throws Exception {
        String jsonResponse = """
            [
                {
                    "lat": "59.9343",
                    "lon": "30.3351",
                    "display_name": "Санкт-Петербург, Россия",
                    "type": "city",
                    "importance": 0.8
                }
            ]
            """;

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn(jsonResponse);

        Coordinates result = geocoding.getCoordinates("Санкт-Петербург");

        assertNotNull(result);
        assertEquals(59.9343, result.getLat(), 0.0001);
        assertEquals(30.3351, result.getLon(), 0.0001);
        assertEquals("Санкт-Петербург, Россия", result.getDisplayName());
    }

    @Test
    void getCoordinates_WithInternationalCity_ShouldReturnCoordinates() throws Exception {
        String jsonResponse = """
            [
                {
                    "lat": "40.7128",
                    "lon": "-74.0060",
                    "display_name": "New York, United States",
                    "type": "city",
                    "importance": 1.0
                }
            ]
            """;

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn(jsonResponse);

        Coordinates result = geocoding.getCoordinates("New York");

        assertNotNull(result);
        assertEquals(40.7128, result.getLat(), 0.0001);
        assertEquals(-74.0060, result.getLon(), 0.0001);
        assertEquals("New York, United States", result.getDisplayName());
    }
}