package com.utils.services;

import com.utils.models.UserAnswerStatus;

public class WeatherBotDialogLogic extends BaseDialogLogic {

    public WeatherBotDialogLogic(WeatherAPI weatherAPI) {
        super(weatherAPI);
    }

    @Override
    public String getQuestion() {
        return "Введите название города для получения погоды:";
    }

    @Override
    public String welcomeWords() {
        return super.welcomeWords() + "\n" +
                "==========================\n" +
                "Доступные команды:\n" +
                "  /help - получить справку\n" +
                "  /quit - выйти из бота\n" +
                "===========================\n" +
                "Вы можете ввести название города в любой момент для получения погоды.";
    }

    public String farewallWordsForInactive() {
        return "❌ Сессия завершена. Введите /start для начала новой сессии.";
    }

    @Override
    public UserAnswerStatus processAnswer(String answer) {
        if (answer.equals("/help")) {
            return new UserAnswerStatus(false, getHelp(), false);
        }
        else if (answer.equals("/quit")) {
            return new UserAnswerStatus(false, farewellWords(), true);
        }
        else {
            try {
                String weather = getQuickWeatherForCity(answer);
                return new UserAnswerStatus(true, weather, false);
            } catch (Exception e) {
                return new UserAnswerStatus(false,
                        "Не удалось получить погоду для города: " + answer +
                                "\nПопробуйте еще раз или введите /quit", false);
            }
        }
    }

    @Override
    public String getHelp() {
        return super.getCommonHelp() +
                "\n\n📅 Дополнительные возможности в Telegram:\n" +
                "  - Кнопки для быстрого выбора периода (Сегодня, Завтра, 3 дня, Неделя)\n" +
                "  - Меню для выбора популярных городов\n" +
                "  - Настройка ежедневных уведомлений\n" +
                "  - Управление через интерактивные кнопки";
    }

    public String getWeatherForPeriod(String city, int days) {
        return formatWeatherForPeriod(city, days);
    }
}