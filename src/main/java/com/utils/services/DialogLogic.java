package com.utils.services;

import com.utils.models.UserAnswerStatus;

public class DialogLogic extends BaseDialogLogic {
    private String currentCity;

    public DialogLogic(WeatherAPI weatherAPI) {
        super(weatherAPI);
    }

    @Override
    public String getQuestion() {
        if (currentCity == null) {
            return "Введите название города для получения погоды:";
        } else {
            return String.format(
                    "Ваш текущий город: %s\nВыберите действие:\n" +
                            "1 - Погода сегодня\n" +
                            "2 - Погода завтра\n" +
                            "3 - Погода на 3 дня\n" +
                            "4 - Погода на неделю\n" +
                            "5 - Сменить город\n" +
                            "Введите номер:",
                    currentCity
            );
        }
    }

    @Override
    public String welcomeWords() {
        return super.welcomeWords() + "\n" +
                "==========================\n" +
                "Вы можете ввести название города для получения погоды.\n" +
                "===========================\n";
    }

    @Override
    public UserAnswerStatus processAnswer(String answer) {
        if (answer.equals("/help")) {
            return new UserAnswerStatus(false, getHelp(), false);
        }
        else if (answer.equals("/quit")) {
            return new UserAnswerStatus(false, farewellWords(), true);
        }
        else if (currentCity == null) {
            try {
                String weather = weatherFormatter.getQuickWeather(answer);
                currentCity = answer;
                return new UserAnswerStatus(true,
                        "✅ Город установлен: " + answer + "\n\n" + weather +
                                "\n\nТеперь вы можете выбрать период прогноза:",
                        false);
            } catch (Exception e) {
                return new UserAnswerStatus(false,
                        "❌ Не удалось получить погоду для города: " + answer +
                                "\nПопробуйте еще раз", false);
            }
        }
        else {
            // Обработка выбора периода
            switch (answer) {
                case "1":
                    return getWeatherForPeriodAsStatus(currentCity, 1);
                case "2":
                    return getWeatherForPeriodAsStatus(currentCity, 2);
                case "3":
                    return getWeatherForPeriodAsStatus(currentCity, 3);
                case "4":
                    return getWeatherForPeriodAsStatus(currentCity, 7);
                case "5":
                    currentCity = null;
                    return new UserAnswerStatus(false,
                            "Введите новый город:", false);
                default:
                    return new UserAnswerStatus(false,
                            "❌ Неверный выбор. Введите число от 1 до 5", false);
            }
        }
    }

    @Override
    public String getHelp() {
        return "Погодный бот - справка:\n\n" +
                "Как использовать:\n" +
                "1. Введите название города\n" +
                "2. Выберите период прогноза (1-4)\n" +
                "3. Для смены города введите 5\n" +
                "4. Для выхода введите /quit\n\n" +
                "Команды:\n" +
                "/help - показать эту справку\n" +
                "/quit - выйти из бота\n\n" +
                "🔔 Уведомления:\n" +
                "  - Введите 'уведомления' для настройки\n" +
                "  - Установите время ежедневных уведомлений";
    }

    public String getCurrentCity() {
        return currentCity;
    }
}