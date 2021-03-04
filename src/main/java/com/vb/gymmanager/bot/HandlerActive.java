package com.vb.gymmanager.bot;

import com.vb.gymmanager.model.SubscribeReserve;
import com.vb.gymmanager.model.Subscription;
import com.vb.gymmanager.repository.SubscribeReserveRepo;
import com.vb.gymmanager.repository.SubscriptionRepo;
import com.vb.gymmanager.service.ScheduleService;
import com.vb.gymmanager.service.SubscribeReserveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class HandlerActive implements BotHandler {

    public static final Logger LOG = LoggerFactory.getLogger(HandlerActive.class);
    public static final Marker INFO_MARKER = MarkerFactory.getMarker("INFO");
    public static final Marker ERROR_MARKER = MarkerFactory.getMarker("ERROR");

    public static final Locale LOCALE = new Locale("ru", "RU");

    @Autowired
    private ChatBot bot;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private BotMenu botMenu;

    @Autowired
    private SubscriptionRepo subscriptionRepo;

    @Autowired
    private SubscribeReserveRepo subscribeReserveRepo;

    @Autowired
    private SubscribeReserveService subscribeReserveService;

    @Override
    public void enter() {

        // После любого запроса сразу отправляем answerCallbackQuery, обозначающий
        // принятие команды.
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

    }

    @Override
    public void handleInput() {

        if (bot.getUpdate().hasMessage()) {  // Обработка сообщения типа Message

            if (bot.getUpdate().getMessage().getText().equals("График тренировок")) {
                showScheduleOnDate();
            } else {
                botMenu.showMainMenu(getMainMenu(), bot.getUser());
            }

        } else if (bot.getUpdate().hasCallbackQuery()) {

            if (bot.getMessageId() == bot.getUser().getLastMessageId()) {

                switch (bot.getCallbackData()) {
                    case "showScheduleOnDate":
                        showScheduleOnDate();
                        break;
                    case "showScheduleOnTime":
                        showScheduleOnTime();
                        break;
                    case "subscribe":
                        if (subscribeUser()) {
                            showScheduleOnTime();
                        }
                        break;
                    case "unSubscribe":
                        if (unSubscribeUser()) {
                            showScheduleOnTime();
                        }
                        break;
                    case "addSubscribe":
                        if (addSubscribe()) {
                            showScheduleOnTime();
                        }
                }

            }

        }

    }

    @Override
    public BotState nextState() {
        return BotState.ACTIVE;
    }

    @Override
    public ReplyKeyboard getMainMenu() {

        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow keyboardRow;

        keyboardRow = new KeyboardRow();
        keyboardRow.add("График тренировок");
        keyboardRowList.add(keyboardRow);

        ReplyKeyboardMarkup replyKeyboard = new ReplyKeyboardMarkup();
        replyKeyboard.setKeyboard(keyboardRowList);
        replyKeyboard.setResizeKeyboard(true);

        return replyKeyboard;

    }

    private void showScheduleOnDate() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(bot.getDate());

        String headerText = "График на *" +
                new SimpleDateFormat("EEEE d MMMM", LOCALE).format(bot.getDate()) + "*\n" +
                bot.getUser().getDefaultGym().getName();

        List<ScheduleDay> scheduleDayList = scheduleService.getScheduleDayList();

        // Кнопки выбора времени занятия
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
            if (scheduleDay.isNotSure()) {
                text.append("❓ ");
            } else if (scheduleDay.isSubscribed()) {
                text.append("✅ ");
            }
            text.append(Utils.timeToString(scheduleDay.getTime()))
                    .append(scheduleDay.getWorkoutCode().length() > 0 ? " " + scheduleDay.getWorkoutCode() : "")
                    .append("\n");
            if (scheduleDay.getCount() > 0) {
                text.append(scheduleDay.getCount()).append(" чел.");
            } else {
                text.append("-");
            }

            keyboardButton.setText(text.toString());
            keyboardButton.setCallbackData("showScheduleOnTime|d=" + Utils.dateToString(bot.getDate())
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

        // Кнопки перехода по датам
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date leftDate = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, 2);
        Date rightDate = calendar.getTime();

        rowInline = new ArrayList<>();
        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("  ⬅  Назад  ");
        keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(leftDate));
        rowInline.add(keyboardButton);

        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("\uD83D\uDD01");
        keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(bot.getDate()));
        rowInline.add(keyboardButton);

        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("  Вперед  ➡  ");
        keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(rightDate));
        rowInline.add(keyboardButton);

        keyboardInline.add(rowInline);

        // Вывод клавиатуры
        InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
        replyKeyboard.setKeyboard(keyboardInline);

        botMenu.showInlineKeyboardMarkup(headerText, replyKeyboard);

    }

    private void showScheduleOnTime() {

        if (bot.getWorkoutType() == null) {
            // Случай, когда пользователь пытается открыть расписание на
            // время, которое администратор уже удалил.
            showScheduleOnDate();
            return;
        }

        List<Subscription> subscriptions = subscriptionRepo.findAllByDateAndTimeAndGym(
                new java.sql.Date(bot.getDate().getTime()),
                new java.sql.Time(bot.getTime().getTime()),
                bot.getGym());

        StringBuilder text = new StringBuilder();
        String weekDay = new SimpleDateFormat("EEEE ", LOCALE).format(bot.getDate());
        text.append("*").append(weekDay.substring(0, 1).toUpperCase()).append(weekDay.substring(1))
                .append(new SimpleDateFormat("d MMMM yyyy", LOCALE).format(bot.getDate()))
                .append(" г.*\n")
                .append(bot.getWorkoutType().getName()).append(" на ")
                .append(Utils.timeToString(bot.getTime()))
                .append("\n_Продолжительность ").append(bot.getWorkoutType().getDuration())
                .append(" ч._\n").append(bot.getGym().getName()).append("\n\n");

        if (subscriptions.size() > 0) {
            text.append("Состав группы:\n");
        } else {
            text.append("Пока никто не записан на это время\n");
        }

        int goingCount = 0;
        int notSureCount = 0;
        int addCount = 0;

        for (Subscription subscription : subscriptions) {
            if (subscription.isNotSure()) {
                text.append("❓ ");
                notSureCount++;
            } else if (subscription.getUser().getId() == bot.getUser().getId()) {
                text.append("✅ ");
                goingCount++;
            } else {
                text.append("☑ ");
                goingCount++;
            }
            text.append("[").append(subscription.getUser().getName()).append("](")
                    .append("tg://user?id=").append(subscription.getUser().getChatId()).append(")\n");
            if (subscription.getCount() > 1) {
                text.append("➕ ").append(subscription.getCount() - 1).append(", от [")
                        .append(subscription.getUser().getName()).append("](")
                        .append("tg://user?id=").append(subscription.getUser().getChatId()).append(")\n");
                addCount += subscription.getCount() - 1;
            }
        }

        // Итого
        if (subscriptions.size() > 0) {
            text.append("\n————————————————\n");
            text.append("Всего идут: ").append(goingCount + addCount).append("\n");
            if (goingCount > 0) {
                text.append("☑    ").append(goingCount).append("\n");
            }
            if (addCount > 0) {
                text.append("➕    ").append(addCount).append("\n");
            }
            if (notSureCount > 0) {
                text.append("❓    ").append(notSureCount).append("\n");
            }
        }

        List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();

        // Первый ряд кнопок
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("✅ Пойду");
        keyboardButton.setCallbackData("subscribe|d=" + Utils.dateToString(bot.getDate())
                + ";t=" + Utils.timeToString(bot.getTime())
                + ";notSure=0");
        rowInline.add(keyboardButton);

        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("❌ Не пойду");
        keyboardButton.setCallbackData("unSubscribe|d=" + Utils.dateToString(bot.getDate())
                + ";t=" + Utils.timeToString(bot.getTime()));
        rowInline.add(keyboardButton);

        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("❓ Возможно");
        keyboardButton.setCallbackData("subscribe|d=" + Utils.dateToString(bot.getDate())
                + ";t=" + Utils.timeToString(bot.getTime())
                + ";notSure=1");
        rowInline.add(keyboardButton);
        keyboardInline.add(rowInline);

        // Второй ряд кнопок
        rowInline = new ArrayList<>();
        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("➕ 1");
        keyboardButton.setCallbackData("addSubscribe|d=" + Utils.dateToString(bot.getDate())
                + ";t=" + Utils.timeToString(bot.getTime())
                + ";c=1");
        rowInline.add(keyboardButton);

        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("➖ 1");
        keyboardButton.setCallbackData("addSubscribe|d=" + Utils.dateToString(bot.getDate())
                + ";t=" + Utils.timeToString(bot.getTime())
                + ";c=-1");
        rowInline.add(keyboardButton);

        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("« Назад");
        keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(bot.getDate()));
        rowInline.add(keyboardButton);
        keyboardInline.add(rowInline);

        InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
        replyKeyboard.setKeyboard(keyboardInline);

        botMenu.showInlineKeyboardMarkup(text.toString(), replyKeyboard);

    }

    private boolean subscribeUser() {

        if (bot.getWorkoutType() == null) {
            // Случай, когда пользователь пытается изменить запись на
            // время, которое администратор уже удалил.
            showScheduleOnDate();
            return false;
        }

        if (notAccessSubscribe()) {
            return false;
        }

        boolean result = false;

        Optional<Subscription> subscriptionOptional = subscriptionRepo.findByDateAndTimeAndGymAndUser(
                new java.sql.Date(bot.getDate().getTime()),
                new java.sql.Time(bot.getTime().getTime()),
                bot.getGym(),
                bot.getUser()
        );

        if (subscriptionOptional.isEmpty()) {

// TODO: 23.02.2021 Новая разработка

//            List<Subscription> subscriptions = subscriptionRepo.findAllByDateAndTimeAndGym(
//                    new java.sql.Date(bot.getDate().getTime()),
//                    new java.sql.Time(bot.getTime().getTime()),
//                    bot.getGym()
//            );
//
//            if (subscriptions.size() >= bot.getWorkoutType().getMaxUsers()) {
//
//                // Превышение количества человек. Их ставим в очередь
//                SubscribeReserve subscribeReserve = new SubscribeReserve();
//                subscribeReserve.setDate(new java.sql.Date(bot.getDate().getTime()));
//                subscribeReserve.setTime(new java.sql.Time(bot.getTime().getTime()));
//                subscribeReserve.setGym(bot.getGym());
//                subscribeReserve.setUser(bot.getUser());
//                subscribeReserve.setNotSure(bot.isNotSure());
//                if (!bot.isNotSure()) {
//                    subscribeReserve.setCount(1);
//                }
//                subscribeReserve.setNumber(subscribeReserveService.getLastOrder() + 1);
//                subscribeReserveRepo.save(subscribeReserve);
//                result = true;
//
//                if (Utils.dateToString(bot.getDate()).equals(Utils.dateToString(new Date()))) {
//                    String text = "РЕЗЕРВ " + "[" + bot.getUser().getName() + "](" +
//                            "tg://user?id=" + bot.getUser().getChatId() + ") на *" +
//                            Utils.timeToString(bot.getTime()) + "*\n" + bot.getGym().getName();
//                    bot.sendMessageToAdmin(text);
//                }
//
//            } else {

                // Запись
                Subscription subscribe = new Subscription();
                subscribe.setDate(new java.sql.Date(bot.getDate().getTime()));
                subscribe.setTime(new java.sql.Time(bot.getTime().getTime()));
                subscribe.setGym(bot.getGym());
                subscribe.setUser(bot.getUser());
                subscribe.setNotSure(bot.isNotSure());
                if (!bot.isNotSure()) {
                    subscribe.setCount(1);
                }
                subscriptionRepo.save(subscribe);
                result = true;

                if (Utils.dateToString(bot.getDate()).equals(Utils.dateToString(new Date()))) {
                    String text = "ЗАПИСЬ " + "[" + bot.getUser().getName() + "](" +
                            "tg://user?id=" + bot.getUser().getChatId() + ") на *" +
                            Utils.timeToString(bot.getTime()) + "*\n" + bot.getGym().getName();
                    bot.sendMessageToAdmin(text);
                }
                String logMessage = "(state=ACTIVE,userId=" + bot.getUser().getChatId() + "), "
                        + "ЗАПИСЬ "
                        + bot.getUser().getName() + " на "
                        + Utils.dateFormat(bot.getDate(), "dd.MM.yyyy") + " "
                        + Utils.timeToString(bot.getTime()) + " "
                        + bot.getGym().getName();
                LOG.info(INFO_MARKER, logMessage);
//            }

        } else {
            Subscription subscribe = subscriptionOptional.get();
            if (subscribe.isNotSure() != bot.isNotSure()) {
                subscribe.setNotSure(bot.isNotSure());
                if (bot.isNotSure()) {
                    subscribe.setCount(0);
                } else {
                    subscribe.setCount(1);
                }
                subscriptionRepo.save(subscribe);
                result = true;
            }
        }

        return result;

    }

    private boolean unSubscribeUser() {

        if (bot.getWorkoutType() == null) {
            // Случай, когда пользователь пытается изменить запись на
            // время, которое администратор уже удалил.
            showScheduleOnDate();
            return false;
        }

        if (notAccessSubscribe()) {
            return false;
        }

        boolean result = false;

        Optional<Subscription> subscriptionOptional = subscriptionRepo.findByDateAndTimeAndGymAndUser(
                new java.sql.Date(bot.getDate().getTime()),
                new java.sql.Time(bot.getTime().getTime()),
                bot.getGym(),
                bot.getUser()
        );

        if (subscriptionOptional.isPresent()) {

            Subscription subscribe = subscriptionOptional.get();
            subscriptionRepo.delete(subscribe);
            result = true;

            if (Utils.dateToString(bot.getDate()).equals(Utils.dateToString(new Date()))) {
                String text = "ОТМЕНА ЗАПИСИ " + "[" + bot.getUser().getName() + "](" +
                        "tg://user?id=" + bot.getUser().getChatId() + ") на *" +
                        Utils.timeToString(bot.getTime()) + "*\n" + bot.getGym().getName();
                bot.sendMessageToAdmin(text);
            }
            String logMessage = "(state=ACTIVE,userId=" + bot.getUser().getChatId() + "), "
                    + "ОТМЕНА ЗАПИСИ "
                    + bot.getUser().getName() + " на "
                    + Utils.dateFormat(bot.getDate(), "dd.MM.yyyy") + " "
                    + Utils.timeToString(bot.getTime()) + " "
                    + bot.getGym().getName();
            LOG.info(INFO_MARKER, logMessage);

        }

        return result;

    }

    private boolean addSubscribe() {

        if (bot.getWorkoutType() == null) {
            // Случай, когда пользователь пытается изменить запись на
            // время, которое администратор уже удалил.
            showScheduleOnDate();
            return false;
        }

        if (notAccessSubscribe()) {
            return false;
        }

        boolean result = false;

        Optional<Subscription> subscriptionOptional = subscriptionRepo.findByDateAndTimeAndGymAndUser(
                new java.sql.Date(bot.getDate().getTime()),
                new java.sql.Time(bot.getTime().getTime()),
                bot.getGym(),
                bot.getUser()
        );

        if (subscriptionOptional.isPresent()) {
            Subscription subscribe = subscriptionOptional.get();
            if (!subscribe.isNotSure()) {
                subscribe.setCount(subscribe.getCount() + bot.getAddCount());
                if (subscribe.getCount() >= 1) {
                    subscriptionRepo.save(subscribe);
                    result = true;
                }
            }
        }

        return result;

    }

    public boolean notAccessSubscribe() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(bot.getTime());

        long endTimeWorkoutMillis = bot.getDate().getTime()
                + 3600000 * calendar.get(Calendar.HOUR_OF_DAY) + 60000 * calendar.get(Calendar.MINUTE)
                + (long) (3600000 * bot.getWorkoutType().getDuration());
        return endTimeWorkoutMillis < System.currentTimeMillis();

    }

}
