package com.utils.tests;

import com.google.gson.Gson;
import com.utils.models.NominatimResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NominatimResponseTest {

    @Test
    void constructor_ShouldCreateEmptyObject() {
        NominatimResponse response = new NominatimResponse();

        assertNull(response.getLatitude());
        assertNull(response.getLongitude());
        assertNull(response.getDisplayName());
        assertNull(response.getType());
        assertEquals(0.0, response.getImportance());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        NominatimResponse response = new NominatimResponse();

        response.setLatitude("55.7558");
        response.setLongitude("37.6176");
        response.setDisplayName("Москва, Россия");
        response.setType("city");
        response.setImportance(0.9);

        assertEquals("55.7558", response.getLatitude());
        assertEquals("37.6176", response.getLongitude());
        assertEquals("Москва, Россия", response.getDisplayName());
        assertEquals("city", response.getType());
        assertEquals(0.9, response.getImportance(), 0.001);
    }

    @Test
    void fromJson_ShouldDeserializeCorrectly() {
        String json = """
            {
                "lat": "55.7558",
                "lon": "37.6176",
                "display_name": "Москва, Центральный федеральный округ, Россия",
                "type": "city",
                "importance": 0.9
            }
            """;

        Gson gson = new Gson();
        NominatimResponse response = gson.fromJson(json, NominatimResponse.class);

        assertEquals("55.7558", response.getLatitude());
        assertEquals("37.6176", response.getLongitude());
        assertEquals("Москва, Центральный федеральный округ, Россия", response.getDisplayName());
        assertEquals("city", response.getType());
        assertEquals(0.9, response.getImportance(), 0.001);
    }

    @Test
    void fromJson_WithMissingFields_ShouldHandleCorrectly() {
        String json = """
            {
                "lat": "59.9343",
                "lon": "30.3351"
            }
            """;

        Gson gson = new Gson();
        NominatimResponse response = gson.fromJson(json, NominatimResponse.class);

        assertEquals("59.9343", response.getLatitude());
        assertEquals("30.3351", response.getLongitude());
        assertNull(response.getDisplayName());
        assertNull(response.getType());
        assertEquals(0.0, response.getImportance(), 0.001);
    }

    @Test
    void fromJson_WithTownType_ShouldDeserializeCorrectly() {
        String json = """
            {
                "lat": "56.4977",
                "lon": "84.9744",
                "display_name": "Томск, городской округ Томск, Томская область, Сибирский федеральный округ, Россия",
                "type": "town",
                "importance": 0.7
            }
            """;

        Gson gson = new Gson();
        NominatimResponse response = gson.fromJson(json, NominatimResponse.class);

        assertEquals("56.4977", response.getLatitude());
        assertEquals("84.9744", response.getLongitude());
        assertEquals("Томск, городской округ Томск, Томская область, Сибирский федеральный округ, Россия", response.getDisplayName());
        assertEquals("town", response.getType());
        assertEquals(0.7, response.getImportance(), 0.001);
    }

    @Test
    void fromJson_WithVillageType_ShouldDeserializeCorrectly() {
        String json = """
            {
                "lat": "55.1234",
                "lon": "37.5678",
                "display_name": "Петровское, Московская область, Россия",
                "type": "village",
                "importance": 0.3
            }
            """;

        Gson gson = new Gson();
        NominatimResponse response = gson.fromJson(json, NominatimResponse.class);

        assertEquals("55.1234", response.getLatitude());
        assertEquals("37.5678", response.getLongitude());
        assertEquals("Петровское, Московская область, Россия", response.getDisplayName());
        assertEquals("village", response.getType());
        assertEquals(0.3, response.getImportance(), 0.001);
    }

    @Test
    void fromJson_WithHighImportance_ShouldDeserializeCorrectly() {
        String json = """
            {
                "lat": "40.7128",
                "lon": "-74.0060",
                "display_name": "New York, United States",
                "type": "city",
                "importance": 1.0
            }
            """;

        Gson gson = new Gson();
        NominatimResponse response = gson.fromJson(json, NominatimResponse.class);

        assertEquals("40.7128", response.getLatitude());
        assertEquals("-74.0060", response.getLongitude());
        assertEquals("New York, United States", response.getDisplayName());
        assertEquals("city", response.getType());
        assertEquals(1.0, response.getImportance(), 0.001);
    }

    @Test
    void fromJson_WithZeroImportance_ShouldDeserializeCorrectly() {
        String json = """
            {
                "lat": "0.0",
                "lon": "0.0",
                "display_name": "Some location",
                "type": "unknown",
                "importance": 0.0
            }
            """;

        Gson gson = new Gson();
        NominatimResponse response = gson.fromJson(json, NominatimResponse.class);

        assertEquals("0.0", response.getLatitude());
        assertEquals("0.0", response.getLongitude());
        assertEquals("Some location", response.getDisplayName());
        assertEquals("unknown", response.getType());
        assertEquals(0.0, response.getImportance(), 0.001);
    }

    @Test
    void toJson_ShouldSerializeCorrectly() {
        NominatimResponse response = new NominatimResponse();
        response.setLatitude("55.7558");
        response.setLongitude("37.6176");
        response.setDisplayName("Москва, Россия");
        response.setType("city");
        response.setImportance(0.9);

        Gson gson = new Gson();
        String json = gson.toJson(response);

        assertTrue(json.contains("\"lat\":\"55.7558\""));
        assertTrue(json.contains("\"lon\":\"37.6176\""));
        assertTrue(json.contains("\"display_name\":\"Москва, Россия\""));
        assertTrue(json.contains("\"type\":\"city\""));
        assertTrue(json.contains("\"importance\":0.9"));
    }

    @Test
    void toJson_WithNegativeCoordinates_ShouldSerializeCorrectly() {
        NominatimResponse response = new NominatimResponse();
        response.setLatitude("-33.8688");
        response.setLongitude("151.2093");
        response.setDisplayName("Sydney, Australia");
        response.setType("city");
        response.setImportance(0.8);

        Gson gson = new Gson();
        String json = gson.toJson(response);

        assertTrue(json.contains("\"lat\":\"-33.8688\""));
        assertTrue(json.contains("\"lon\":\"151.2093\""));
        assertTrue(json.contains("\"display_name\":\"Sydney, Australia\""));
        assertTrue(json.contains("\"type\":\"city\""));
        assertTrue(json.contains("\"importance\":0.8"));
    }
}