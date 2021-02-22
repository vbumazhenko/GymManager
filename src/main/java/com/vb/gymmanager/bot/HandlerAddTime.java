package com.vb.gymmanager.bot;

import com.vb.gymmanager.model.Schedule;
import com.vb.gymmanager.model.WorkoutType;
import com.vb.gymmanager.repository.ScheduleRepo;
import com.vb.gymmanager.repository.WorkoutTypeRepo;
import com.vb.gymmanager.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class HandlerAddTime implements BotHandler {

    public static final Logger LOG = LoggerFactory.getLogger(HandlerAddTime.class);
    public static final Marker INFO_MARKER = MarkerFactory.getMarker("INFO");
    public static final Marker ERROR_MARKER = MarkerFactory.getMarker("ERROR");

    @Autowired
    private ChatBot bot;

    @Autowired
    private BotMenu botMenu;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private WorkoutTypeRepo workoutTypeRepo;

    @Autowired
    private ScheduleRepo scheduleRepo;

    private BotState next;

    @Override
    public void enter() {

        if (bot.getUpdate().hasCallbackQuery()) {
            AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
            answerCallbackQuery.setCallbackQueryId(bot.getUpdate().getCallbackQuery().getId());
            try {
                bot.execute(answerCallbackQuery);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                LOG.error(ERROR_MARKER, e.getMessage());
            }
        }

        bot.setMainMenuHeader("Введите время тренировки и код занятия, например: 8:00 CF");
        botMenu.showMainMenu(getMainMenu(), bot.getUser());

    }

    @Override
    public void handleInput() {

        if (!bot.getUpdate().hasMessage()) {
            next = BotState.ADD_TIME;
            return;
        }

        bot.setDate(bot.getUser().getInputDate());
        String inputText = bot.getUpdate().getMessage().getText().trim();

        // Нажали отмену
        if (inputText.equals("Отмена")) {
            bot.setMainMenuHeader("Добавление времени отменено");
            botMenu.showMainMenu(bot.getHandler(BotState.ADMIN).getMainMenu(), bot.getUser());
            editSchedule();
            next = BotState.ADMIN;
            return;
        }

        // Разбор того, что ввели
        String[] parsedTime = parseInputTime(inputText);
        if (parsedTime != null) {

            // Получение вида тренировки по коду
            Optional<WorkoutType> workoutTypeOptional = workoutTypeRepo.findByCode(parsedTime[1]);
            if (workoutTypeOptional.isEmpty()) {
                bot.sendMessage("Неверный формат вида тренировки");
                next = BotState.ADD_TIME;
                return;
            }

            // Копирование расписания
            scheduleService.copyScheduleOnDay();

            // Проверка, что такого времени нет в расписании
            Optional<Schedule> schedules = scheduleRepo.findByDateAndTimeAndWeekdayAndGym(
                    new java.sql.Date(bot.getDate().getTime()),
                    new java.sql.Time(Utils.stringToTime(parsedTime[0]).getTime()),
                    0,
                    bot.getUser().getDefaultGym()
            );

            String textMessage;
            if (schedules.isEmpty()) {
                // Добавление времени в расписание
                Schedule schedule = new Schedule();
                schedule.setDate(bot.getDate());
                schedule.setTime(Utils.stringToTime(parsedTime[0]));
                schedule.setWeekday(0);
                schedule.setGym(bot.getUser().getDefaultGym());
                workoutTypeOptional.ifPresent(schedule::setWorkoutType);
                scheduleRepo.save(schedule);
                textMessage = "Время добавлено в график";
            } else {
                textMessage ="Такое время уже есть в графике";
            }

            // Вызов редактора расписания на день и переход в главное меню
            bot.setMainMenuHeader(textMessage);
            botMenu.showMainMenu(getMainMenu(), bot.getUser());
            editSchedule();
            next = BotState.ADMIN;

        } else {
            bot.sendMessage("Неверный формат времени");
            next = BotState.ADD_TIME;
        }

    }

    @Override
    public BotState nextState() {
        return next;
    }

    @Override
    public ReplyKeyboard getMainMenu() {
        return null;
    }

    public void editSchedule() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(bot.getDate());

        String headerText = "График на *" +
                new SimpleDateFormat("EEEE d MMMM").format(bot.getDate()) + "*\n" +
                bot.getUser().getDefaultGym().getName();

        List<ScheduleDay> scheduleDayList = scheduleService.getScheduleDayList();

        // Кнопки выбора времени тренировки
        List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline;
        InlineKeyboardButton keyboardButton;

        int colCount = 2;
        rowInline = new ArrayList<>();
        for (ScheduleDay scheduleDay : scheduleDayList) {

            if (rowInline.size() >= colCount) {
                keyboardInline.add(rowInline);
                rowInline = new ArrayList<>();
            }
            keyboardButton = new InlineKeyboardButton();

            StringBuilder text = new StringBuilder();
            text.append("❌  ");
            text.append(Utils.timeToString(scheduleDay.getTime()))
                    .append(scheduleDay.getWorkoutCode().length() > 0 ? " " + scheduleDay.getWorkoutCode() : "")
                    .append(" \n");
            if (scheduleDay.getCount() > 0) {
                text.append(scheduleDay.getCount()).append(" чел.");
            } else {
                text.append("-");
            }

            keyboardButton.setText(text.toString());
            keyboardButton.setCallbackData("delTime|d=" + Utils.dateToString(bot.getDate())
                    + ";t=" + Utils.timeToString(scheduleDay.getTime()));
            rowInline.add(keyboardButton);

        }
        while (rowInline.size() > 0 && rowInline.size() < colCount) {
            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText(" ");
            keyboardButton.setCallbackData(" ");
            rowInline.add(keyboardButton);
        }
        keyboardInline.add(rowInline);

        rowInline = new ArrayList<>();
        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("   Добавить время   ");
        keyboardButton.setCallbackData("addTime|d=" + Utils.dateToString(bot.getDate()));
        rowInline.add(keyboardButton);

        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("« Назад");
        keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(bot.getDate()));
        rowInline.add(keyboardButton);

        keyboardInline.add(rowInline);

        // Вывод клавиатуры
        InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
        replyKeyboard.setKeyboard(keyboardInline);

        botMenu.showInlineKeyboardMarkup(headerText, replyKeyboard);

    }

    private String[] parseInputTime(String str) {

        String[] result = new String[2];
        String[] args = str.split("\\s+", 2);
        if (args.length == 0) {
            return null;
        }

        // Парсим время
        try {
            Date time = new SimpleDateFormat("H:mm").parse(args[0]);
            result[0] = Utils.timeToString(time);
        } catch (ParseException ignored) {
            LOG.error(ERROR_MARKER, "Ошибка парсинга времени: " + str);
            return null;
        }

        // Парсим тип тренировки
        List<WorkoutType> workoutTypeList = workoutTypeRepo.findAll();
        if (workoutTypeList.size() > 0) {
            result[1] = workoutTypeList.get(0).getCode();
        }

        if (args.length == 2) {
            for (WorkoutType wt : workoutTypeList) {
                if (Utils.getModifyCode(args[1]).equals(Utils.getModifyCode(wt.getCode()))) {
                    result[1] = wt.getCode();
                }
            }
        }

        return result;

    }

}
