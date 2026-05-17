package com.utils.services;

import com.utils.interfaces.IDialogLogic;
import com.utils.models.UserAnswerStatus;

public abstract class BaseDialogLogic implements IDialogLogic {
    protected final WeatherAPI weatherAPI;
    protected final WeatherFormatter weatherFormatter;

    public BaseDialogLogic(WeatherAPI weatherAPI) {
        this.weatherAPI = weatherAPI;
        this.weatherFormatter = new WeatherFormatter(weatherAPI);
    }

    public String needToStart() {
        return "Для запуска бота введите /start";
    }

    public String welcomeWords() {
        return "Добро пожаловать в погодный бот!";
    }

    protected String farewellWords() {
        return "До свидания! Возвращайтесь еще!";
    }

    protected String getCommonHelp() {
        return "📖 Помощь по боту:\n\n" +
                "🌤 Получить погоду:\n" +
                "  - Введите название города\n" +
                "  - Бот покажет текущую погоду\n\n" +
                "🔄 Управление:\n" +
                "  - /help - получить справку\n" +
                "  - /quit - выйти из бота\n\n" +
                "❓ Если что-то не работает:\n" +
                "  - Проверьте правильность написания города\n" +
                "  - Используйте форматы: \"Москва\" или \"Moscow, Russia\"";
    }

    public abstract String getQuestion();
    public abstract UserAnswerStatus processAnswer(String answer);
    public abstract String getHelp();

    public String getQuickWeatherForCity(String city) {
        try {
            return weatherFormatter.getQuickWeather(city);
        } catch (Exception e) {
            return "❌ Не удалось получить погоду для города: " + city;
        }
    }

    protected UserAnswerStatus getWeatherForPeriodAsStatus(String city, int days) {
        try {
            String weather;
            switch (days) {
                case 1:
                    weather = weatherFormatter.getQuickWeather(city);
                    break;
                case 2:
                    weather = weatherFormatter.formatTomorrowWeather(city);
                    break;
                case 3:
                    var responseFor3Days = weatherAPI.getWeatherByCity(city, 3);
                    var coordsFor3Days = weatherAPI.getGeocoding().getCoordinates(city);
                    weather = weatherFormatter.formatWeatherResponse(
                            responseFor3Days, coordsFor3Days.getDisplayName(), 3
                    );
                    break;
                case 7:
                    var responseFor7Days = weatherAPI.getWeatherByCity(city, 7);
                    var coordsFor7Days = weatherAPI.getGeocoding().getCoordinates(city);
                    weather = weatherFormatter.formatWeatherResponse(
                            responseFor7Days, coordsFor7Days.getDisplayName(), 7
                    );
                    break;
                default:
                    weather = weatherFormatter.getQuickWeather(city);
            }
            return new UserAnswerStatus(true, weather, false);
        } catch (Exception e) {
            return new UserAnswerStatus(false,
                    "❌ Ошибка при получении погоды: " + e.getMessage(), false);
        }
    }

    public String formatWeatherForPeriod(String city, int days) {
        try {
            switch (days) {
                case 1:
                    return weatherFormatter.getQuickWeather(city);
                case 2:
                    return weatherFormatter.formatTomorrowWeather(city);
                case 3:
                    var responseFor3Days = weatherAPI.getWeatherByCity(city, 3);
                    var coordsFor3Days = weatherAPI.getGeocoding().getCoordinates(city);
                    return weatherFormatter.formatWeatherResponse(
                            responseFor3Days, coordsFor3Days.getDisplayName(), 3
                    );
                case 7:
                    var responseFor7Days = weatherAPI.getWeatherByCity(city, 7);
                    var coordsFor7Days = weatherAPI.getGeocoding().getCoordinates(city);
                    return weatherFormatter.formatWeatherResponse(
                            responseFor7Days, coordsFor7Days.getDisplayName(), 7
                    );
                default:
                    return weatherFormatter.getQuickWeather(city);
            }
        } catch (Exception e) {
            return "❌ Ошибка при получении погоды: " + e.getMessage();
        }
    }
}