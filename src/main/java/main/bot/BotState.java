package main.bot;

import main.model.*;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public enum BotState {

    START {
        @Override
        public void enter(BotContext context) {
            String text = "Для начала давайте познакомимся";
            sendMessage(context, text);
        }

        @Override
        public BotState nextState() {
            return ENTER_NAME;
        }
    },
    ENTER_NAME {
        private BotState next;

        @Override
        public void enter(BotContext context) {
            sendMessage(context, "Введите ваши имя и фамилию,\nнапример, Андрей Иванов");
        }

        @Override
        public void handleInput(BotContext context) {

            String inputText = context.getUpdate().getMessage().getText().trim();
            if (Utils.isValidName(inputText)) {
                context.getCurrentUser().setName(inputText);
                next = ENTER_PHONE;
            } else {
                sendMessage(context, "Неверный формат ввода имени, фамилии");
                next = ENTER_NAME;
            }

        }

        @Override
        public BotState nextState() {
            return next;
        }
    },
    ENTER_PHONE {
        private BotState next;

        @Override
        public void enter(BotContext context) {
            sendMessage(context, "Введите номер телефона");
        }

        @Override
        public void handleInput(BotContext context) {

            String phone = Utils.getValidPhoneNumber(context.getUpdate().getMessage().getText().trim());
            if (phone != null) {
                context.getCurrentUser().setPhone(phone);
                next = SEND_QUERY_TO_ADMIN;
            } else {
                sendMessage(context, "Неверный формат ввода номера телефона");
                next = ENTER_PHONE;
            }

        }

        @Override
        public BotState nextState() {
            return next;
        }
    },
    SEND_QUERY_TO_ADMIN(false) {
        @Override
        public void enter(BotContext context) {

            String text = "Запрос на добавление от пользователя " + "[" +
                    context.getCurrentUser().getName() + "](" +
                    "tg://user?id=" + context.getCurrentUser().getChatId() + ")";
            sendMessageToAdmin(context, text);

        }

        @Override
        public BotState nextState() {
            return WAITING;
        }
    },
    WAITING {
        @Override
        public void enter(BotContext context) {
            sendMessage(context, "Запрос на добавление участника отправлен администратору. " +
                    "Ожидайте ответ в ближайшее время.");
        }

        @Override
        public BotState nextState() {
            return WAITING;
        }
    },
    ACTIVE {

        @Override
        public void enter(BotContext context) {

            if (context.getUpdate().hasCallbackQuery()) {
                AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                answerCallbackQuery.setCallbackQueryId(context.getUpdate().getCallbackQuery().getId());
                try {
                    context.getBot().execute(answerCallbackQuery);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        public void handleInput(BotContext context) {

            if (context.getUpdate().hasMessage()) {  // Обработка сообщения типа Message

                if (context.getUpdate().getMessage().getText().equals("График тренировок")) {
                    showScheduleOnDate(context);
                } else {
                    showMainMenu(context, getMainMenu(context));
                }

            } else if (context.getUpdate().hasCallbackQuery()) {

                if (context.getMessageId() == context.getCurrentUser().getLastMessageId()) {

                    switch (context.getCallbackData()) {
                        case "showScheduleOnDate":
                            showScheduleOnDate(context);
                            break;
                        case "showScheduleOnTime":
                            showScheduleOnTime(context);
                            break;
                        case "subscribe":
                            if (subscribeUser(context)) {
                                showScheduleOnTime(context);
                            }
                            break;
                        case "unSubscribe":
                            if (unSubscribeUser(context)) {
                                showScheduleOnTime(context);
                            }
                            break;
                        case "addSubscribe":
                            if (addSubscribe(context)) {
                                showScheduleOnTime(context);
                            }
                    }

                }

            }

        }

        @Override
        public ReplyKeyboard getMainMenu(BotContext context) {

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

        private void showScheduleOnDate(BotContext context) {

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(context.getDate());

            String headerText = "График на *" +
                    new SimpleDateFormat("EEEE d MMMM").format(context.getDate()) + "*\n" +
                    context.getCurrentUser().getDefaultGym().getName();

            List<ScheduleDay> scheduleDayList = context.scheduleService.getScheduleDayList(context);

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
                keyboardButton.setCallbackData("showScheduleOnTime|d=" + Utils.dateToString(context.getDate())
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
            java.util.Date leftDate = calendar.getTime();

            calendar.add(Calendar.DAY_OF_MONTH, 2);
            Date rightDate = calendar.getTime();

            rowInline = new ArrayList<>();
            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("  ⬅  Назад  ");
            keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(leftDate));
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("\uD83D\uDD01");
            keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(context.getDate()));
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("  Вперед  ➡  ");
            keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(rightDate));
            rowInline.add(keyboardButton);

            keyboardInline.add(rowInline);

            // Вывод клавиатуры
            InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardInline);

            showInlineKeyboardMarkup(context.getBot(), context.getMessageId(), context.getCurrentUser().getChatId(),
                    headerText, replyKeyboard);

        }

        private void showScheduleOnTime(BotContext context) {

            if (context.getWorkoutType() == null) {
                // Случай, когда пользователь пытается открыть расписание на
                // время, которое администратор уже удалил.
                showScheduleOnDate(context);
                return;
            }

            List<Subscription> subscriptions = context.subscriptionRepo.findAllByDateAndTimeAndGym(
                    new java.sql.Date(context.getDate().getTime()),
                    new java.sql.Time(context.getTime().getTime()),
                    context.getGym());

            StringBuilder text = new StringBuilder();
            String weekDay = new SimpleDateFormat("EEEE ").format(context.getDate());
            text.append("*").append(weekDay.substring(0, 1).toUpperCase()).append(weekDay.substring(1))
                    .append(new SimpleDateFormat("d MMMM yyyy").format(context.getDate()))
                    .append(" г.*\n")
                    .append(context.getWorkoutType().getName()).append(" на ")
                    .append(Utils.timeToString(context.getTime()))
                    .append("\n_Продолжительность ").append(context.getWorkoutType().getDuration())
                    .append(" ч._\n").append(context.getGym().getName()).append("\n\n");

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
                } else if (subscription.getUser().getId() == context.getCurrentUser().getId()) {
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
            keyboardButton.setCallbackData("subscribe|d=" + Utils.dateToString(context.getDate())
                    + ";t=" + Utils.timeToString(context.getTime())
                    + ";notSure=0");
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("❌ Не пойду");
            keyboardButton.setCallbackData("unSubscribe|d=" + Utils.dateToString(context.getDate())
                    + ";t=" + Utils.timeToString(context.getTime()));
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("❓ Возможно");
            keyboardButton.setCallbackData("subscribe|d=" + Utils.dateToString(context.getDate())
                    + ";t=" + Utils.timeToString(context.getTime())
                    + ";notSure=1");
            rowInline.add(keyboardButton);
            keyboardInline.add(rowInline);

            // Второй ряд кнопок
            rowInline = new ArrayList<>();
            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("➕ 1");
            keyboardButton.setCallbackData("addSubscribe|d=" + Utils.dateToString(context.getDate())
                    + ";t=" + Utils.timeToString(context.getTime())
                    + ";c=1");
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("➖ 1");
            keyboardButton.setCallbackData("addSubscribe|d=" + Utils.dateToString(context.getDate())
                    + ";t=" + Utils.timeToString(context.getTime())
                    + ";c=-1");
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("« Назад");
            keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(context.getDate()));
            rowInline.add(keyboardButton);
            keyboardInline.add(rowInline);

            InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardInline);

            showInlineKeyboardMarkup(context.getBot(), context.getMessageId(), context.getCurrentUser().getChatId(),
                    text.toString(), replyKeyboard);

        }

        private boolean subscribeUser(BotContext context) {

            if (context.getWorkoutType() == null) {
                // Случай, когда пользователь изменить запись на
                // время, которое администратор уже удалил.
                showScheduleOnDate(context);
                return false;
            }

            if (Utils.notAccessSubscribe(context)) {
                return false;
            }

            boolean result = false;

            Optional<Subscription> subscriptionOptional = context.subscriptionRepo.findByDateAndTimeAndGymAndUser(
                    new java.sql.Date(context.getDate().getTime()),
                    new java.sql.Time(context.getTime().getTime()),
                    context.getGym(),
                    context.getCurrentUser()
            );

            if (subscriptionOptional.isEmpty()) {

                Subscription subscribe = new Subscription();
                subscribe.setDate(new java.sql.Date(context.getDate().getTime()));
                subscribe.setTime(new java.sql.Time(context.getTime().getTime()));
                subscribe.setGym(context.getGym());
                subscribe.setUser(context.getCurrentUser());
                subscribe.setNotSure(context.isNotSure());
                if (!context.isNotSure()) {
                    subscribe.setCount(1);
                }
                context.subscriptionRepo.save(subscribe);
                result = true;

                if (Utils.dateToString(context.getDate()).equals(Utils.dateToString(new Date()))) {
                    String text = "ЗАПИСЬ " + "[" + context.getCurrentUser().getName() + "](" +
                            "tg://user?id=" + context.getCurrentUser().getChatId() + ") на *" +
                            Utils.timeToString(context.getTime()) + "*\n" + context.getGym().getName();
                    sendMessageToAdmin(context, text);
                }

            } else {
                Subscription subscribe = subscriptionOptional.get();
                if (subscribe.isNotSure() != context.isNotSure()) {
                    subscribe.setNotSure(context.isNotSure());
                    if (context.isNotSure()) {
                        subscribe.setCount(0);
                    } else {
                        subscribe.setCount(1);
                    }
                    context.subscriptionRepo.save(subscribe);
                    result = true;
                }
            }

            return result;

        }

        private boolean unSubscribeUser(BotContext context) {

            if (context.getWorkoutType() == null) {
                // Случай, когда пользователь изменить запись на
                // время, которое администратор уже удалил.
                showScheduleOnDate(context);
                return false;
            }

            if (Utils.notAccessSubscribe(context)) {
                return false;
            }

            boolean result = false;

            Optional<Subscription> subscriptionOptional = context.subscriptionRepo.findByDateAndTimeAndGymAndUser(
                    new java.sql.Date(context.getDate().getTime()),
                    new java.sql.Time(context.getTime().getTime()),
                    context.getGym(),
                    context.getCurrentUser()
            );

            if (subscriptionOptional.isPresent()) {

                Subscription subscribe = subscriptionOptional.get();
                context.subscriptionRepo.delete(subscribe);
                result = true;

                if (Utils.dateToString(context.getDate()).equals(Utils.dateToString(new Date()))) {
                    String text = "ОТМЕНА ЗАПИСИ " + "[" + context.getCurrentUser().getName() + "](" +
                            "tg://user?id=" + context.getCurrentUser().getChatId() + ") на *" +
                            Utils.timeToString(context.getTime()) + "*\n" + context.getGym().getName();
                    sendMessageToAdmin(context, text);
                }

            }

            return result;

        }

        private boolean addSubscribe(BotContext context) {

            if (context.getWorkoutType() == null) {
                // Случай, когда пользователь изменить запись на
                // время, которое администратор уже удалил.
                showScheduleOnDate(context);
                return false;
            }

            if (Utils.notAccessSubscribe(context)) {
                return false;
            }

            boolean result = false;

            Optional<Subscription> subscriptionOptional = context.subscriptionRepo.findByDateAndTimeAndGymAndUser(
                    new java.sql.Date(context.getDate().getTime()),
                    new java.sql.Time(context.getTime().getTime()),
                    context.getGym(),
                    context.getCurrentUser()
            );

            if (subscriptionOptional.isPresent()) {
                Subscription subscribe = subscriptionOptional.get();
                if (!subscribe.isNotSure()) {
                    subscribe.setCount(subscribe.getCount() + context.getAddCount());
                    if (subscribe.getCount() >= 1) {
                        context.subscriptionRepo.save(subscribe);
                        result = true;
                    }
                }
            }

            return result;

        }

    },
    BLOCKED,
    ADMIN {

        private BotState next;

        @Override
        public void enter(BotContext context) {

            if (context.getUpdate().hasCallbackQuery()) {
                AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                answerCallbackQuery.setCallbackQueryId(context.getUpdate().getCallbackQuery().getId());
                try {
                    context.getBot().execute(answerCallbackQuery);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        public void handleInput(BotContext context) {

            next = ADMIN;

            if (context.getUpdate().hasMessage()) {

                List<Gym> gymList = context.gymRepo.findAll();
                for (Gym gym : gymList) {
                    if (context.getUpdate().getMessage().getText().contains(gym.getName())) {
                        context.setGym(gym);
                        context.setMainMenuHeader("Изменено на " + gym.getName());
                        changeGym(context);
                        showMainMenu(context, getMainMenu(context));
                        showScheduleOnDate(context);
                        return;
                    }
                }

                switch (context.getUpdate().getMessage().getText()) {
                    case "График тренировок":
                        showScheduleOnDate(context);
                        break;
                    case "Заявки":
                        showWaitingUsers(context);
                        break;
                    case "Все участники":
                        showUserList(context);
                        break;
                    case "Заблокированные":
                        showBlockedUsers(context);
                        break;
                    default:
                        showMainMenu(context, getMainMenu(context));
                        break;
                }

            } else if (context.getUpdate().hasCallbackQuery()) {

                if (context.getMessageId() == context.getCurrentUser().getLastMessageId()) {

                    switch (context.getCallbackData()) {
                        case "showScheduleOnDate":
                            showScheduleOnDate(context);
                            break;
                        case "editSchedule":
                            editSchedule(context);
                            break;
                        case "addTime":
                            context.getCurrentUser().setInputDate(context.getDate());
                            next = ADD_TIME;
                            break;
                        case "delTime":
                            deleteTimeOnSchedule(context);
                            break;
                        case "showWaitingUsers":
                            showWaitingUsers(context);
                            break;
                        case "showUserList":
                            showUserList(context);
                            break;
                        case "showBlockedUsers":
                            showBlockedUsers(context);
                            break;
                        case "showUserData":
                            showUserData(context);
                            break;
                        case "chState":
                            changeState(context);
                            showUserData(context);
                            break;
                        case "showGymList":
                            showGymList(context);
                            break;
                        case "chGym":
                            if (changeGym(context)) {
                                showGymList(context);
                            }
                            break;
                    }

                }

            }

        }

        @Override
        public BotState nextState() {
            return next;
        }

        @Override
        public ReplyKeyboard getMainMenu(BotContext context) {

            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow;

            keyboardRow = new KeyboardRow();
            keyboardRow.add("График тренировок");
            keyboardRowList.add(keyboardRow);

            keyboardRow = new KeyboardRow();
            keyboardRow.add("Заявки");
            keyboardRow.add("Все участники");
            keyboardRow.add("Заблокированные");
            keyboardRowList.add(keyboardRow);

            List<Gym> gymList = context.gymRepo.findAll();

            keyboardRow = new KeyboardRow();
            for (Gym gym : gymList) {
                StringBuilder buttonText = new StringBuilder();
                if (context.getUser().getDefaultGym().getId() == gym.getId()) {
                    buttonText.append("✅ ");
                }
                buttonText.append(gym.getName());
                keyboardRow.add(buttonText.toString());
            }
            keyboardRowList.add(keyboardRow);

            ReplyKeyboardMarkup replyKeyboard = new ReplyKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardRowList);
            replyKeyboard.setResizeKeyboard(true);

            return replyKeyboard;

        }

        private void showScheduleOnDate(BotContext context) {

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(context.getDate());

            StringBuilder headerText = new StringBuilder();
            headerText.append("График на *")
                    .append(new SimpleDateFormat("EEEE d MMMM").format(context.getDate())).append("*\n")
                    .append(context.getCurrentUser().getDefaultGym().getName()).append("\n");

            List<ScheduleDay> scheduleDayList = context.scheduleService.getScheduleDayList(context);

            for (ScheduleDay scheduleDay : scheduleDayList) {

                headerText.append("\n*").append(Utils.timeToString(scheduleDay.getTime()))
                        .append(scheduleDay.getWorkoutCode().length() > 0 ? " " + scheduleDay.getWorkoutCode() : "");
                if (scheduleDay.getCount() > 0) {
                    headerText.append(" - ").append(scheduleDay.getCount());
                }
                headerText.append("*\n");
                List<Subscription> subscriptions = context.subscriptionRepo.findAllByDateAndTimeAndGym(
                        new java.sql.Date(context.getDate().getTime()),
                        new java.sql.Time(scheduleDay.getTime().getTime()),
                        context.getGym());

                if (subscriptions.size() == 0) {
                    headerText.append("(пусто)\n");
                }

                for (Subscription subscription : subscriptions) {
                    if (subscription.isNotSure()) {
                        headerText.append("❓ ");
                    } else if (subscription.getUser().getId() == context.getCurrentUser().getId()) {
                        headerText.append("✅ ");
                    } else {
                        headerText.append("☑ ");
                    }
                    headerText.append("[").append(subscription.getUser().getName()).append("](")
                            .append("tg://user?id=").append(subscription.getUser().getChatId()).append(")\n");
                    if (subscription.getCount() > 1) {
                        headerText.append("➕ ").append(subscription.getCount() - 1).append(", от [")
                                .append(subscription.getUser().getName()).append("](")
                                .append("tg://user?id=").append(subscription.getUser().getChatId()).append(")\n");
                    }
                }

            }

            List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();
            List<InlineKeyboardButton> rowInline;
            InlineKeyboardButton keyboardButton;

            // Кнопки перехода по датам
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            java.util.Date leftDate = calendar.getTime();

            calendar.add(Calendar.DAY_OF_MONTH, 2);
            Date rightDate = calendar.getTime();

            rowInline = new ArrayList<>();
            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("  ⬅  Назад  ");
            keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(leftDate));
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("\uD83D\uDD01");
            keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(context.getDate()));
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("  Вперед  ➡  ");
            keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(rightDate));
            rowInline.add(keyboardButton);
            keyboardInline.add(rowInline);

            // Кнопка редактирования расписания
            rowInline = new ArrayList<>();
            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("Редактировать");
            keyboardButton.setCallbackData("editSchedule|d=" + Utils.dateToString(context.getDate()));
            rowInline.add(keyboardButton);
            keyboardInline.add(rowInline);

            // Вывод клавиатуры
            InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardInline);

            showInlineKeyboardMarkup(context.getBot(), context.getMessageId(), context.getCurrentUser().getChatId(),
                    headerText.toString(), replyKeyboard);

        }

        private void deleteTimeOnSchedule(BotContext context) {

            context.scheduleService.copyScheduleOnDay(context);

            Optional<Schedule> schedules = context.scheduleRepo.findByDateAndTimeAndWeekdayAndGym(
                    new java.sql.Date(context.getDate().getTime()),
                    new java.sql.Time(context.getTime().getTime()),
                    0,
                    context.getCurrentUser().getDefaultGym()
            );

            schedules.ifPresent(schedule -> context.scheduleRepo.delete(schedule));

            // После удаления, необходимо отменить подписку пользователей на это время
            List<Subscription> subscriptionList = context.subscriptionRepo.findAllByDateAndTimeAndGym(
                    new java.sql.Date(context.getDate().getTime()),
                    new java.sql.Time(context.getTime().getTime()),
                    context.getCurrentUser().getDefaultGym()
            );

            // Отправка сообщения об отмене занятия
            for (Subscription subscription : subscriptionList) {
                String text = "Тренировка на *" + Utils.timeToString(subscription.getTime()) + " "
                        + new SimpleDateFormat("EEEE d MMMM yyyy").format(context.getDate())
                        + " г.* отменена. Пожалуйста, запишитесь на другое время";
                sendMessage(context, text, subscription.getUser());
            }

            // Удаление подписки всех пользователей на данное время
            context.subscriptionRepo.deleteAll(subscriptionList);

            // Вывод обновленного расписания
            editSchedule(context);

        }

        private void showWaitingUsers(BotContext context) {

            List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();
            List<InlineKeyboardButton> rowInline;

            List<User> userList = context.userRepo.findAll();
            userList = userList.stream()
                    .filter(user -> user.getState().equals(BotState.WAITING))
                    .sorted(Comparator.comparing(User::getName))
                    .collect(Collectors.toList());

            int count = 0;
            for (User user : userList) {
                rowInline = new ArrayList<>();
                InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
                keyboardButton.setText(user.getName() + (user.isAdmin() ? " (админ)" : ""));
                keyboardButton.setCallbackData("showUserData|uId=" + user.getId()
                        + ";ulm=showWaitingUsers");
                rowInline.add(keyboardButton);
                keyboardInline.add(rowInline);
                count++;
            }

            InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardInline);

            String text = context.getCurrentUser().getDefaultGym().getName() + "\n" +
                    "Новые заявки (" + count + "):";

            showInlineKeyboardMarkup(context.getBot(), context.getMessageId(), context.getCurrentUser().getChatId(),
                    text, replyKeyboard);

        }

        private void showUserList(BotContext context) {

            List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();
            List<InlineKeyboardButton> rowInline;

            List<User> userList = context.userRepo.findAllByDefaultGym(context.getCurrentUser().getDefaultGym());
            userList = userList.stream()
                    .filter(user -> user.getState().equals(BotState.ACTIVE)
                            || user.getState().equals(BotState.ADMIN))
                    .sorted((o1, o2) -> {
                        int sortIsAdmin = Boolean.compare(o2.isAdmin(), o1.isAdmin());
                        if (sortIsAdmin != 0) {
                            return sortIsAdmin;
                        } else {
                            return o1.getName().compareTo(o2.getName());
                        }
                    }).collect(Collectors.toList());

            int count = 0;
            for (User user : userList) {
                rowInline = new ArrayList<>();
                InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
                keyboardButton.setText(user.getName() + (user.isAdmin() ? " (админ)" : ""));
                keyboardButton.setCallbackData("showUserData|uId=" + user.getId()
                        + ";ulm=showUserList");
                rowInline.add(keyboardButton);
                keyboardInline.add(rowInline);
                count++;
            }

            InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardInline);

            String text = context.getCurrentUser().getDefaultGym().getName() + "\n" +
                    "Все участники (" + count + "):";

            showInlineKeyboardMarkup(context.getBot(), context.getMessageId(), context.getCurrentUser().getChatId(),
                    text, replyKeyboard);

        }

        private void showBlockedUsers(BotContext context) {

            List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();
            List<InlineKeyboardButton> rowInline;

            List<User> userList = context.userRepo.findAllByDefaultGym(context.getCurrentUser().getDefaultGym());
            userList = userList.stream()
                    .filter(user -> user.getState().equals(BotState.BLOCKED))
                    .sorted(Comparator.comparing(User::getName))
                    .collect(Collectors.toList());

            int count = 0;
            for (User user : userList) {
                rowInline = new ArrayList<>();
                InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
                keyboardButton.setText(user.getName() + (user.isAdmin() ? " (админ)" : ""));
                keyboardButton.setCallbackData("showUserData|uId=" + user.getId()
                        + ";ulm=showBlockedUsers");
                rowInline.add(keyboardButton);
                keyboardInline.add(rowInline);
                count++;
            }

            InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardInline);

            String text = context.getCurrentUser().getDefaultGym().getName() + "\n" +
                    "Заблокированные (" + count + "):";

            showInlineKeyboardMarkup(context.getBot(), context.getMessageId(), context.getCurrentUser().getChatId(),
                    text, replyKeyboard);

        }

        private void showUserData(BotContext context) {

            User user = context.getUser();
            StringBuilder text = new StringBuilder();
            text.append("[").append(user.getName()).append("](")
                    .append("tg://user?id=").append(user.getChatId()).append(")")
                    .append(user.isAdmin() ? " (админ)\n" : "\n");
            if (!user.isAdmin()) {
                text.append(user.getDefaultGym().getName()).append("\n");
            }
            text.append("Дата регистрации: ")
                    .append(new SimpleDateFormat("dd.MM.yyyy").format(user.getRegDate()));

            List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();
            List<InlineKeyboardButton> rowInline;
            InlineKeyboardButton keyboardButton;

            if (user.getId() != context.getCurrentUser().getId()) {

                // Добавить / заблокировать / разблокировать
                if (user.getState().equals(BotState.WAITING)) {
                    rowInline = new ArrayList<>();
                    keyboardButton = new InlineKeyboardButton();
                    keyboardButton.setText("Добавить");
                    keyboardButton.setCallbackData("chState|uId=" + user.getId()
                            + ";st=" + BotState.ACTIVE
                            + ";ulm=" + context.getUserListMode());
                    rowInline.add(keyboardButton);
                    keyboardInline.add(rowInline);
                }
                if (user.getState().equals(BotState.ACTIVE) || user.getState().equals(BotState.WAITING)) {
                    rowInline = new ArrayList<>();
                    keyboardButton = new InlineKeyboardButton();
                    keyboardButton.setText("Заблокировать");
                    keyboardButton.setCallbackData("chState|uId=" + user.getId()
                            + ";st=" + BotState.BLOCKED
                            + ";ulm=" + context.getUserListMode());
                    rowInline.add(keyboardButton);
                    keyboardInline.add(rowInline);
                }
                if (user.getState().equals(BotState.BLOCKED)) {
                    rowInline = new ArrayList<>();
                    keyboardButton = new InlineKeyboardButton();
                    keyboardButton.setText("Разблокировать");
                    keyboardButton.setCallbackData("chState|uId=" + user.getId()
                            + ";st=" + BotState.ACTIVE
                            + ";ulm=" + context.getUserListMode());
                    rowInline.add(keyboardButton);
                    keyboardInline.add(rowInline);
                }

                // Изменить клуб (только для активных участников)
                if (user.getState().equals(BotState.ACTIVE)) {
                    rowInline = new ArrayList<>();
                    keyboardButton = new InlineKeyboardButton();
                    keyboardButton.setText("Изменить клуб");
                    keyboardButton.setCallbackData("showGymList|uId=" + user.getId()
                            + ";ulm=" + context.getUserListMode());
                    rowInline.add(keyboardButton);
                    keyboardInline.add(rowInline);
                }

                // Добавить в администраторы / удалить из администраторов
                if (user.getState().equals(BotState.ACTIVE)) {
                    rowInline = new ArrayList<>();
                    keyboardButton = new InlineKeyboardButton();
                    keyboardButton.setText("Добавить в администраторы");
                    keyboardButton.setCallbackData("chState|uId=" + user.getId()
                            + ";st=" + BotState.ADMIN
                            + ";ulm=" + context.getUserListMode());
                    rowInline.add(keyboardButton);
                    keyboardInline.add(rowInline);
                } else if (user.getState().equals(BotState.ADMIN)) {
                    rowInline = new ArrayList<>();
                    keyboardButton = new InlineKeyboardButton();
                    keyboardButton.setText("Удалить из администраторов");
                    keyboardButton.setCallbackData("chState|uId=" + user.getId()
                            + ";st=" + BotState.ACTIVE
                            + ";ulm=" + context.getUserListMode());
                    rowInline.add(keyboardButton);
                    keyboardInline.add(rowInline);
                }

            }

            // Назад
            rowInline = new ArrayList<>();
            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("« Назад");
            keyboardButton.setCallbackData(context.getUserListMode());
            rowInline.add(keyboardButton);
            keyboardInline.add(rowInline);

            InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardInline);

            showInlineKeyboardMarkup(context.getBot(), context.getMessageId(), context.getCurrentUser().getChatId(),
                    text.toString(), replyKeyboard);

        }

        private void changeState(BotContext context) {

            User user = context.getUser();
            BotState previousState = user.getState();
            user.setState(context.getState());
            user.setAdmin(context.getState().equals(BotState.ADMIN));
            context.userRepo.save(user);

            // Сообщение при добавлении нового участника
            if (previousState.equals(BotState.WAITING) && context.getState().equals(BotState.ACTIVE)) {
                context.setMainMenuHeader("Ваша заявка одобрена.\nДобро пожаловать в наш клуб!");
                showMainMenu(context, context.getState().getMainMenu(context));
            }

            // Сообщение при блокировке участника
            if (!previousState.equals(BotState.BLOCKED) && context.getState().equals(BotState.BLOCKED)) {
                context.setMainMenuHeader("Вы заблокированы.\nДля уточнения обратитесь к администратору");
                showMainMenu(context, context.getState().getMainMenu(context));
            }

            // Сообщение при разблокировке участника
            if (previousState.equals(BotState.BLOCKED) && !context.getState().equals(BotState.BLOCKED)) {
                context.setMainMenuHeader("Вы снова являетесь участником клуба.\nВыберите действие");
                showMainMenu(context, context.getState().getMainMenu(context));
            }

            // Сообщение при добавлении в администраторы
            if (!previousState.equals(BotState.ADMIN) && context.getState().equals(BotState.ADMIN)) {
                context.setMainMenuHeader("Вас сделали администратором");
                showMainMenu(context, context.getState().getMainMenu(context));
            }

            // Сообщение при удалении из администраторов
            if (previousState.equals(BotState.ADMIN) && !context.getState().equals(BotState.ADMIN)) {
                context.setMainMenuHeader("Вы больше не являетесь администратором");
                showMainMenu(context, context.getState().getMainMenu(context));
            }

        }

        private void showGymList(BotContext context) {

            String headerText = "Изменить текущий клуб";
            if (context.getUser().getId() == context.getCurrentUser().getId()) {
                headerText += ":";
            } else {
                headerText += " для\n"
                        + "[" + context.getUser().getName() + "](tg://user?id=" + context.getUser().getChatId() + ")";
            }

            List<Gym> gymList = context.gymRepo.findAll();

            List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();
            List<InlineKeyboardButton> rowInline;

            for (Gym gym : gymList) {

                StringBuilder text = new StringBuilder();
                if (context.getUser().getDefaultGym().getId() == gym.getId()) {
                    text.append("✅ ");
                }
                text.append(gym.getName());

                InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
                keyboardButton.setText(text.toString());
                keyboardButton.setCallbackData("chGym|uId=" + context.getUser().getId()
                        + ";gId=" + gym.getId()
                        + ";ulm=" + context.getUserListMode());

                rowInline = new ArrayList<>();
                rowInline.add(keyboardButton);
                keyboardInline.add(rowInline);

            }

            // Назад
            if (context.getUser().getId() != context.getCurrentUser().getId()) {
                rowInline = new ArrayList<>();
                InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
                keyboardButton.setText("« Назад");
                keyboardButton.setCallbackData("showUserData|uId=" + context.getUser().getId()
                        + ";ulm=" + context.getUserListMode());
                rowInline.add(keyboardButton);
                keyboardInline.add(rowInline);
            }

            InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardInline);

            showInlineKeyboardMarkup(context.getBot(), context.getMessageId(), context.getCurrentUser().getChatId(),
                    headerText, replyKeyboard);

        }

        private boolean changeGym(BotContext context) {

            boolean result = false;

            if (context.getUser().getId() == context.getCurrentUser().getId()) {
                context.setUser(context.getCurrentUser());
            }
            User user = context.getUser();

            if (user.getDefaultGym().getId() != context.getGym().getId()) {
                user.setDefaultGym(context.getGym());
                context.userRepo.save(user);
                result = true;
            }

            return result;

        }

    },
    ADD_TIME {

        private BotState next;

        @Override
        public void enter(BotContext context) {

            if (context.getUpdate().hasCallbackQuery()) {
                AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                answerCallbackQuery.setCallbackQueryId(context.getUpdate().getCallbackQuery().getId());
                try {
                    context.getBot().execute(answerCallbackQuery);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            context.setMainMenuHeader("Введите время тренировки и код занятия, например: 8:00 CF");
            showMainMenu(context, getMainMenu(context));

        }

        @Override
        public void handleInput(BotContext context) {

            if (!context.getUpdate().hasMessage()) {
                next = ADD_TIME;
                return;
            }

            context.setDate(context.getCurrentUser().getInputDate());
            String inputText = context.getUpdate().getMessage().getText().trim();

            // Нажали отмену
            if (inputText.equals("Отмена")) {
                context.setMainMenuHeader("Добавление времени отменено");
                showMainMenu(context, ADMIN.getMainMenu(context));
                editSchedule(context);
                next = ADMIN;
                return;
            }

            // Разбор того, что ввели
            String[] parsedTime = parseInputTime(context, inputText);
            if (parsedTime != null) {

                // Получение вида тренировки по коду
                Optional<WorkoutType> workoutTypeOptional = context.workoutTypeRepo.findByCode(parsedTime[1]);
                if (workoutTypeOptional.isEmpty()) {
                    sendMessage(context, "Неверный формат вида тренировки");
                    next = ADD_TIME;
                    return;
                }

                // Копирование расписания
                context.scheduleService.copyScheduleOnDay(context);

                // Проверка, что такого времени нет в расписании
                Optional<Schedule> schedules = context.scheduleRepo.findByDateAndTimeAndWeekdayAndGym(
                        new java.sql.Date(context.getDate().getTime()),
                        new java.sql.Time(Utils.stringToTime(parsedTime[0]).getTime()),
                        0,
                        context.getCurrentUser().getDefaultGym()
                );

                String textMessage;
                if (schedules.isEmpty()) {
                    // Добавление времени в расписание
                    Schedule schedule = new Schedule();
                    schedule.setDate(context.getDate());
                    schedule.setTime(Utils.stringToTime(parsedTime[0]));
                    schedule.setWeekday(0);
                    schedule.setGym(context.getCurrentUser().getDefaultGym());
                    workoutTypeOptional.ifPresent(schedule::setWorkoutType);
                    context.scheduleRepo.save(schedule);
                    textMessage = "Время добавлено в график";
                } else {
                    textMessage ="Такое время уже есть в графике";
                }

                // Вызов редактора расписания на день и переход в главное меню
                context.setMainMenuHeader(textMessage);
                showMainMenu(context, ADMIN.getMainMenu(context));
                editSchedule(context);
                next = ADMIN;

            } else {
                sendMessage(context, "Неверный формат времени");
                next = ADD_TIME;
            }

        }

        @Override
        public BotState nextState() {
            return next;
        }

        @Override
        public ReplyKeyboard getMainMenu(BotContext context) {

            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow;

            keyboardRow = new KeyboardRow();
            keyboardRow.add("Отмена");
            keyboardRowList.add(keyboardRow);

            ReplyKeyboardMarkup replyKeyboard = new ReplyKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardRowList);
            replyKeyboard.setResizeKeyboard(true);

            return replyKeyboard;

        }

        private String[] parseInputTime(BotContext context, String str) {

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
                return null;
            }

            // Парсим тип тренировки
            List<WorkoutType> workoutTypeList = context.workoutTypeRepo.findAll();
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

    };

    private final boolean inputNeeded;

    BotState() {
        this.inputNeeded = true;
    }

    BotState(boolean inputNeeded) {
        this.inputNeeded = inputNeeded;
    }

    public static BotState getInitialState() {
        return START;
    }

    protected void sendMessage(BotContext context, String text) {
        sendMessage(context, text, context.getCurrentUser());
    }

    protected void sendMessage(BotContext context, String text, User user) {

        // При выводе сообщения, счетчик сообщений пользователя необходимо сбросить
        if (context.getCurrentUser().getId() != user.getId()) {
            user.setLastMessageId(0);
            context.userRepo.save(user);
        }

        SendMessage message = new SendMessage();
        message.setChatId(user.getChatId());
        message.setText(text);
        message.enableMarkdown(true);
        try {
            context.getBot().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    protected void sendMessageToAdmin(BotContext context, String text) {

        List<User> adminList = context.userRepo.findAllByIsAdmin(true);
        for (User admin : adminList) {
            sendMessage(context, text, admin);
        }

    }

    public boolean isInputNeeded() {
        return inputNeeded;
    }

    public void handleInput(BotContext context) {
    }

    public void enter(BotContext context) {
    }

    public BotState nextState() {
        return this;
    }

    public ReplyKeyboard getMainMenu(BotContext context) {
        return new ReplyKeyboardRemove();
    }

    public void showMainMenu(BotContext context, ReplyKeyboard keyboard) {

        User user = context.getUser();

        // При выводе главного меню, счетчик сообщений необходимо сбросить
        if (context.getCurrentUser().getId() != user.getId()) {
            user.setLastMessageId(0);
            context.userRepo.save(user);
        }

        SendMessage message = new SendMessage();
        message.setChatId(user.getChatId());
        message.setText(context.getMainMenuHeader());
        message.setReplyMarkup(keyboard);

        try {
            context.getBot().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    public void showInlineKeyboardMarkup(ChatBot chatBot, int messageId, long chatId,
                                         String text, InlineKeyboardMarkup replyKeyboard) {

        if (messageId > 0) {

            EditMessageText message = new EditMessageText();
            message.enableMarkdown(true);
            message.setMessageId(messageId);
            message.setChatId(chatId);
            message.setText(text);
            message.setReplyMarkup(replyKeyboard);
            try {
                chatBot.execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

        } else {

            SendMessage message = new SendMessage();
            message.enableMarkdown(true);
            message.setChatId(chatId);
            message.setText(text);
            message.setReplyMarkup(replyKeyboard);
            try {
                chatBot.execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

        }

    }

    public void editSchedule(BotContext context) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(context.getDate());

        String headerText = "График на *" +
                new SimpleDateFormat("EEEE d MMMM").format(context.getDate()) + "*\n" +
                context.getCurrentUser().getDefaultGym().getName();

        List<ScheduleDay> scheduleDayList = context.scheduleService.getScheduleDayList(context);

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
            keyboardButton.setCallbackData("delTime|d=" + Utils.dateToString(context.getDate())
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
        keyboardButton.setCallbackData("addTime|d=" + Utils.dateToString(context.getDate()));
        rowInline.add(keyboardButton);

        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("« Назад");
        keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(context.getDate()));
        rowInline.add(keyboardButton);

        keyboardInline.add(rowInline);

        // Вывод клавиатуры
        InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
        replyKeyboard.setKeyboard(keyboardInline);

        showInlineKeyboardMarkup(context.getBot(), context.getMessageId(), context.getCurrentUser().getChatId(),
                headerText, replyKeyboard);

    }

}
