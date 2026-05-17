package com.utils.services;

import com.utils.models.OpenMeteoResponse;
import com.utils.models.Coordinates;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class WeatherFormatter {
    private final WeatherAPI weatherAPI;

    public WeatherFormatter(WeatherAPI weatherAPI) {
        this.weatherAPI = weatherAPI;
    }

    public String formatWeatherResponse(OpenMeteoResponse response, String location, int days) {
        StringBuilder weatherText = new StringBuilder();

        if (days == 1) {
            double tempMin = response.getDaily().getTemperature2mMin().get(0);
            double tempMax = response.getDaily().getTemperature2mMax().get(0);
            String condition = weatherAPI.getWeatherCondition(response.getDaily().getWeatherCode().get(0));

            weatherText.append(String.format("🌤 Погода в %s:\n\n", location))
                    .append(String.format("🌡 Температура: %.0f°C...%.0f°C\n", tempMin, tempMax))
                    .append(String.format("%s\n", condition))
                    .append(String.format("💨 Ветер: %.0f км/ч\n",
                            response.getDaily().getWindspeed10mMax().get(0)));

            if (response.getDaily().getPrecipitationProbabilityMax() != null) {
                double precipitation = response.getDaily().getPrecipitationProbabilityMax().get(0);
                weatherText.append(String.format("☔️ Вероятность дождя: %.0f%%", precipitation));
            }

        } else {
            weatherText.append(String.format("📅 Погода в %s на %d дней:\n\n", location, days));

            for (int i = 0; i < Math.min(days, response.getDaily().getTime().size()); i++) {
                String dayName = formatDay(response.getDaily().getTime().get(i));
                double tempMin = response.getDaily().getTemperature2mMin().get(i);
                double tempMax = response.getDaily().getTemperature2mMax().get(i);
                String condition = weatherAPI.getWeatherCondition(response.getDaily().getWeatherCode().get(i));

                weatherText.append(String.format("%s: %.0f°C...%.0f°C, %s\n",
                        dayName, tempMin, tempMax, condition));
            }
        }

        return weatherText.toString();
    }

    public String formatTomorrowWeather(String city) throws Exception {
        OpenMeteoResponse response = weatherAPI.getTomorrowWeather(city);

        StringBuilder weatherText = new StringBuilder();
        weatherText.append(String.format("📅 Погода в %s на завтра:\n\n", city));

        // Берем данные для второго дня (индекс 1)
        double tempMin = response.getDaily().getTemperature2mMin().get(1);
        double tempMax = response.getDaily().getTemperature2mMax().get(1);
        String condition = weatherAPI.getWeatherCondition(response.getDaily().getWeatherCode().get(1));
        double windSpeed = response.getDaily().getWindspeed10mMax().get(1);

        weatherText.append(String.format("🌡 Температура: %.0f°C...%.0f°C\n", tempMin, tempMax))
                .append(String.format("%s\n", condition))
                .append(String.format("💨 Ветер: %.0f км/ч\n", windSpeed));

        if (response.getDaily().getPrecipitationProbabilityMax() != null) {
            double precipitation = response.getDaily().getPrecipitationProbabilityMax().get(1);
            weatherText.append(String.format("☔️ Вероятность дождя: %.0f%%", precipitation));
        }

        return weatherText.toString();
    }

    public String getQuickWeather(String city) throws Exception {
        OpenMeteoResponse response = weatherAPI.getWeatherByCity(city, 1);
        Coordinates coords = weatherAPI.getGeocoding().getCoordinates(city);
        return formatWeatherResponse(response, coords.getDisplayName(), 1);
    }

    private String formatDay(String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        LocalDate today = LocalDate.now();

        if (date.equals(today)) return "Сегодня";
        if (date.equals(today.plusDays(1))) return "Завтра";
        if (date.equals(today.plusDays(2))) return "Послезавтра";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");
        return date.format(formatter);
    }
}
