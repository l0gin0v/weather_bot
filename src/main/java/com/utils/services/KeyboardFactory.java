package com.utils.services;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {

    public static ReplyKeyboardMarkup createMainWeatherKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первый ряд
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🌤 Сегодня");
        row1.add("📅 Завтра");

        // Второй ряд
        KeyboardRow row2 = new KeyboardRow();
        row2.add("📆 3 дня");
        row2.add("🗓 Неделя");

        // Третий ряд
        KeyboardRow row3 = new KeyboardRow();
        row3.add("📍 Сменить город");
        row3.add("🏙 Популярные города");

        // Четвертый ряд - ДОБАВЛЯЕМ уведомления
        KeyboardRow row4 = new KeyboardRow();
        row4.add("🔔 Уведомления");
        row4.add("/help");
        row4.add("/quit");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    // Клавиатура с популярными городами
    public static ReplyKeyboardMarkup createCitiesKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первый ряд
        KeyboardRow row1 = new KeyboardRow();
        row1.add("Москва");
        row1.add("Санкт-Петербург");

        // Второй ряд
        KeyboardRow row2 = new KeyboardRow();
        row2.add("Новосибирск");
        row2.add("Екатеринбург");

        // Третий ряд
        KeyboardRow row3 = new KeyboardRow();
        row3.add("Казань");
        row3.add("Нижний Новгород");

        // Четвертый ряд
        KeyboardRow row4 = new KeyboardRow();
        row4.add("Сочи");
        row4.add("Владивосток");

        // Пятый ряд - навигация
        KeyboardRow row5 = new KeyboardRow();
        row5.add("↩️ Назад");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);
        keyboard.add(row5);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup createCancelKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("↩️ Отмена");

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public static InlineKeyboardMarkup createInlineWeatherKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Первый ряд
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton("🌤 Сегодня", "weather_today"));
        row1.add(createInlineButton("📅 Завтра", "weather_tomorrow"));

        // Второй ряд
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton("📆 3 дня", "weather_3days"));
        row2.add(createInlineButton("🗓 Неделя", "weather_week"));

        rows.add(row1);
        rows.add(row2);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    public static InlineKeyboardMarkup createInlineCitiesKeyboard() {
        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Первый ряд
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton("Москва", "city_moscow"));
        row1.add(createInlineButton("СПб", "city_spb"));

        // Второй ряд
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton("Новосибирск", "city_novosibirsk"));
        row2.add(createInlineButton("Екатеринбург", "city_ekb"));

        // Третий ряд
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton("Казань", "city_kazan"));
        row3.add(createInlineButton("Сочи", "city_sochi"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        inlineKeyboard.setKeyboard(rows);
        return inlineKeyboard;
    }

    private static InlineKeyboardButton createInlineButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    public static ReplyKeyboardMarkup createConfirmationKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("✅ Да");
        row.add("❌ Нет");

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup createStartKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("/start");
        row.add("🌤 Погода");

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup createNotificationKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("⏰ Установить время");
        row1.add("ℹ️ Информация");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("❌ Отменить");
        row2.add("↩️ Назад");

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}