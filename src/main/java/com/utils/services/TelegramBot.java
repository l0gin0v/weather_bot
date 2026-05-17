package com.utils.services;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import com.utils.interfaces.INotificationClient;
import com.utils.models.Coordinates;
import com.utils.models.UserSession;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

public class TelegramBot extends TelegramLongPollingBot implements INotificationClient {
    private final String botUsername;
    private final String botToken;
    private final WeatherAPI weatherAPI;
    private final WeatherBotDialogLogic weatherBotDialogLogic;
    private final Geocoding geocodingService;
    private final NotificationService notificationService;
    private final NotificationScheduler notificationScheduler;
    private final SessionManager sessionManager;

    private enum UserState {
        DEFAULT, WAITING_FOR_CITY, WAITING_FOR_NOTIFICATION_TIME, INACTIVE
    }

    public TelegramBot(String botUsername, String botToken) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.weatherAPI = new WeatherAPI();
        this.weatherBotDialogLogic = new WeatherBotDialogLogic(weatherAPI);
        this.geocodingService = new Geocoding();
        this.sessionManager = new SessionManager();

        WeatherFormatter weatherFormatter = new WeatherFormatter(weatherAPI);
        this.notificationService = new NotificationService(
                weatherAPI, weatherFormatter, sessionManager
        );
        this.notificationScheduler = new NotificationScheduler(notificationService, this);

        Thread notificationThread = new Thread(notificationScheduler);
        notificationThread.setDaemon(true);
        notificationThread.start();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            sessionManager.updateActivity(chatId);

            if (!sessionManager.isSessionActive(chatId) && !messageText.equals("/start")) {
                sendSessionInactiveMessage(chatId);
                return;
            }

            UserState currentState = getUserStateFromDB(chatId);

            if (messageText.equals("/start")) {
                startUserSession(chatId);
                sendWelcomeMessage(chatId);
                return;
            }
            else if (messageText.equals("/help")) {
                sendHelp(chatId);
                return;
            }
            else if (messageText.equals("/quit")) {
                endUserSession(chatId);
                return;
            }

            if (!sessionManager.isSessionActive(chatId)) {
                return;
            }

            switch (currentState) {
                case DEFAULT:
                    handleDefaultState(chatId, messageText);
                    break;
                case WAITING_FOR_CITY:
                    handleCityInputState(chatId, messageText);
                    break;
                case WAITING_FOR_NOTIFICATION_TIME:
                    handleNotificationTimeInput(chatId, messageText);
                    break;
                case INACTIVE:
                    sendSessionInactiveMessage(chatId);
                    break;
            }
        }
    }

    private UserState getUserStateFromDB(long chatId) {
        return sessionManager.getSession(chatId)
                .map(session -> {
                    try {
                        return UserState.valueOf(session.getState());
                    } catch (IllegalArgumentException e) {
                        return UserState.INACTIVE;
                    }
                })
                .orElse(UserState.INACTIVE);
    }

    private void startUserSession(long chatId) {
        sessionManager.activateSession(chatId, null);
        sessionManager.updateState(chatId, UserState.DEFAULT.name());
    }

    private void endUserSession(long chatId) {
        String farewellText = "👋 До свидания! Сессия завершена.\nДля возобновления работы введите /start";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(farewellText);

        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(true);
        message.setReplyMarkup(keyboardRemove);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        sessionManager.deactivateSession(chatId);
        notificationService.cancelNotification(chatId);
    }

    @Override
    public boolean isUserSessionActive(long chatId) {
        return sessionManager.isSessionActive(chatId);
    }

    private void sendSessionInactiveMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(weatherBotDialogLogic.farewallWordsForInactive());

        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(true);
        message.setReplyMarkup(keyboardRemove);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleDefaultState(long chatId, String messageText) {
        switch (messageText) {
            case "🌤 Сегодня":
                sendWeatherForPeriod(chatId, 1);
                break;
            case "📅 Завтра":
                sendWeatherForPeriod(chatId, 2);
                break;
            case "📆 3 дня":
                sendWeatherForPeriod(chatId, 3);
                break;
            case "🗓 Неделя":
                sendWeatherForPeriod(chatId, 7);
                break;
            case "📍 Сменить город":
                sessionManager.updateState(chatId, UserState.WAITING_FOR_CITY.name());
                askForCity(chatId);
                break;
            case "🏙 Популярные города":
                sessionManager.updateState(chatId, UserState.WAITING_FOR_CITY.name());
                showPopularCities(chatId);
                break;
            case "🔔 Уведомления":
                showNotificationMenu(chatId);
                break;
            case "⏰ Установить время":
                askForNotificationTime(chatId);
                break;
            case "ℹ️ Информация":
                String info = notificationService.getNotificationInfo(chatId);
                sendMessage(chatId, info, KeyboardFactory.createNotificationKeyboard());
                break;
            case "❌ Отменить":
                String result = notificationService.cancelNotification(chatId);
                sendMessage(chatId, result, KeyboardFactory.createMainWeatherKeyboard());
                break;
            case "↩️ Назад":
            case "↩️ Отмена":
                sessionManager.updateState(chatId, UserState.DEFAULT.name());
                sendWelcomeMessage(chatId);
                break;
            default:
                sendMessage(chatId,
                        "🤔 Используйте кнопки для навигации или введите /help для справки",
                        KeyboardFactory.createMainWeatherKeyboard()
                );
        }
    }

    private void showNotificationMenu(long chatId) {
        String city = sessionManager.getSession(chatId)
                .map(UserSession::getCity)
                .orElse(null);

        if (city == null) {
            sendMessage(chatId,
                    "❌ Сначала выберите город для уведомлений",
                    KeyboardFactory.createMainWeatherKeyboard()
            );
            return;
        }

        String menuText = String.format(
                "🔔 Управление уведомлениями для %s:\n\nнажмите кнопку:",
                city
        );

        sendMessage(chatId, menuText, KeyboardFactory.createNotificationKeyboard());
    }

    private void handleNotificationTimeInput(long chatId, String timeInput) {
        UserState currentState = getUserStateFromDB(chatId);

        if (!currentState.equals(UserState.WAITING_FOR_NOTIFICATION_TIME)) {
            sendMessage(chatId,
                    "Нажмите ⏰ Установить время сначала",
                    KeyboardFactory.createMainWeatherKeyboard()
            );
            return;
        }

        if (timeInput.equals("↩️ Назад") || timeInput.equals("↩️ Отмена")) {
            sessionManager.updateState(chatId, UserState.DEFAULT.name());
            showNotificationMenu(chatId);
            return;
        }

        if (!isValidTimeFormat(timeInput)) {
            sendMessage(chatId,
                    "❌ Неверный формат времени. Используйте HH:MM (например: 09:00)\n" +
                            "Попробуйте снова или нажмите ↩️ Отмена:",
                    KeyboardFactory.createCancelKeyboard()
            );
            return;
        }

        String city = sessionManager.getSession(chatId)
                .map(UserSession::getCity)
                .orElse(null);

        if (city == null) {
            sendMessage(chatId,
                    "❌ Сначала выберите город",
                    KeyboardFactory.createMainWeatherKeyboard()
            );
            sessionManager.updateState(chatId, UserState.DEFAULT.name());
            return;
        }

        try {
            String result = notificationService.setNotification(chatId, city, timeInput);
            sendMessage(chatId, result, KeyboardFactory.createMainWeatherKeyboard());
            sessionManager.updateState(chatId, UserState.DEFAULT.name());

        } catch (Exception e) {
            sendMessage(chatId,
                    "❌ Ошибка: " + e.getMessage() + "\nПопробуйте снова:",
                    KeyboardFactory.createCancelKeyboard()
            );
        }
    }

    @Override
    public void sendNotificationToUser(long chatId, String notificationText) {
        sendMessage(chatId, notificationText, KeyboardFactory.createMainWeatherKeyboard());
    }

    private boolean isValidTimeFormat(String time) {
        return time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");
    }

    private void handleCityInputState(long chatId, String messageText) {
        if (messageText.equals("↩️ Назад") || messageText.equals("↩️ Отмена")) {
            sessionManager.updateState(chatId, UserState.DEFAULT.name());
            sendWelcomeMessage(chatId);
            return;
        }

        try {
            Coordinates coords = geocodingService.getCoordinates(messageText);

            sessionManager.updateCity(chatId, messageText);
            sessionManager.updateState(chatId, UserState.DEFAULT.name());

            String confirmation = String.format(
                    "✅ Город установлен: %s\n\n" +
                            "Теперь вы можете:\n" +
                            "1. Посмотреть погоду (кнопки выше)\n" +
                            "2. Настроить уведомления (кнопка 🔔 Уведомления)",
                    coords.getDisplayName()
            );

            sendMessage(chatId, confirmation, KeyboardFactory.createMainWeatherKeyboard());

        } catch (Exception e) {
            sendMessage(chatId,
                    "❌ Не удалось найти город: " + messageText +
                            "\nПопробуйте уточнить название или нажмите ↩️ Отмена",
                    KeyboardFactory.createCancelKeyboard()
            );
        }
    }

    private void sendWelcomeMessage(long chatId) {
        sessionManager.updateState(chatId, UserState.DEFAULT.name());
        String userName = getUserName(chatId);

        String city = sessionManager.getSession(chatId)
                .map(UserSession::getCity)
                .orElse(null);

        String text;
        if (city != null) {
            String notificationInfo = notificationService.getNotificationInfo(chatId);
            text = String.format(
                    "🌤 Привет, %s!\nДобро пожаловать в погодный бот!\n\n" +
                            "Ваш текущий город: %s\n\n" +
                            "%s\n\n" +
                            "Выберите действие:",
                    userName, city, notificationInfo
            );
        } else {
            text = String.format(
                    "🌤 Привет, %s!\nДобро пожаловать в погодный бот!\n\n" +
                            "Сначала выберите город, затем период прогноза.",
                    userName
            );
        }

        sendMessage(chatId, text, KeyboardFactory.createMainWeatherKeyboard());
    }

    private void askForCity(long chatId) {
        sendMessage(chatId,
                "🏙 Введите название города:\n(например: Москва, Санкт-Петербург, London)\n\n" +
                        "Или нажмите ↩️ Отмена для возврата",
                KeyboardFactory.createCancelKeyboard()
        );
    }

    private void askForNotificationTime(long chatId) {
        sessionManager.updateState(chatId, UserState.WAITING_FOR_NOTIFICATION_TIME.name());
        sendMessage(chatId,
                "⏰ Введите время для уведомления (формат HH:MM):\n" +
                        "Например: 09:00, 18:30\n\n" +
                        "Бот будет присылать вам погоду каждый день в это время.\n\n" +
                        "Или нажмите ↩️ Отмена",
                KeyboardFactory.createCancelKeyboard()
        );
    }

    private void showPopularCities(long chatId) {
        sendMessage(chatId,
                "Выберите город из списка или введите свой:\n\n" +
                        "Или нажмите ↩️ Отмена для возврата",
                KeyboardFactory.createCitiesKeyboard()
        );
    }

    private void sendHelp(long chatId) {
        String helpText = weatherBotDialogLogic.getHelp();

        sendMessage(chatId, helpText, KeyboardFactory.createMainWeatherKeyboard());
        sessionManager.updateState(chatId, UserState.DEFAULT.name());
    }

    private void sendWeatherForPeriod(long chatId, int days) {
        String city = sessionManager.getSession(chatId)
                .map(UserSession::getCity)
                .orElse(null);

        if (city == null) {
            sendMessage(chatId,
                    "❌ Сначала выберите город с помощью кнопки \"📍 Сменить город\"",
                    KeyboardFactory.createMainWeatherKeyboard()
            );
            return;
        }

        try {
            String weatherText = weatherBotDialogLogic.getWeatherForPeriod(city, days);
            sendMessage(chatId, weatherText, KeyboardFactory.createMainWeatherKeyboard());

        } catch (Exception e) {
            sendMessage(chatId,
                    "❌ Ошибка при получении погоды для: " + city +
                            "\nПопробуйте выбрать другой город",
                    KeyboardFactory.createMainWeatherKeyboard()
            );
            e.printStackTrace();
        }
    }

    private String getUserName(long chatId) {
        return "друг";
    }

    public void sendMessage(long chatId, String text, ReplyKeyboardMarkup keyboard) {
        // Проверяем активна ли сессия через SessionManager
        if (!sessionManager.isSessionActive(chatId)) {
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getClientName() {
        return "TelegramBot";
    }
}