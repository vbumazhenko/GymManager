package main.bot;

import main.model.Subscription;
import main.model.User;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.SimpleDateFormat;
import java.util.*;

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
                context.getUser().setName(inputText);
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
                context.getUser().setPhone(phone);
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

            List<User> adminList = context.userRepo.findAllByIsAdmin(true);
            for (User admin:adminList) {
                admin.setState(BotState.ADMIN_ADD_USER);
                context.userRepo.save(admin);
                String text = "Запрос от пользователя: " + context.getUser().getName()
                        + "\nДобавить в список участников? (Да/Нет)";
                sendMessage(context, text, admin.getChatId());
            }

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
    SUBSCRIPTION {
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
                    showMainMenu(context.getBot(), context.getUser().getChatId());
                }

            } else if (context.getUpdate().hasCallbackQuery()) {

                if (context.getMessageId() == context.getUser().getLastMessageId()) {

                    switch (context.getCallbackData()) {
                        case "showSchedule":
                            showScheduleOnDate(context);
                            break;
                        case "showSubscriptions":
                            showSubscriptionsOnTime(context);
                            break;
                        case "subscribe":
                            if (subscribeUser(context)) {
                                showSubscriptionsOnTime(context);
                            }
                            break;
                        case "unSubscribe":
                            if (unSubscribeUser(context)) {
                                showSubscriptionsOnTime(context);
                            }
                            break;
                        case "addSubscribe":
                            if (addSubscribe(context)) {
                                showSubscriptionsOnTime(context);
                            }
                    }

                }

            }

        }

        @Override
        public void showMainMenu(ChatBot bot, long chatId) {

            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add("График тренировок");
            keyboardRowList.add(keyboardRow);

            ReplyKeyboardMarkup replyKeyboard = new ReplyKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardRowList);
            replyKeyboard.setResizeKeyboard(true);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Выберите действие");
            message.setReplyMarkup(replyKeyboard);
            try {
                bot.execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

        }

        private void showScheduleOnDate(BotContext context) {

            String headerText = "График на *" + new SimpleDateFormat("EEEE").format(context.getDate()) + "*\n"
                    + new SimpleDateFormat("d MMMM yyyy").format(context.getDate()) + " г.";

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(context.getDate());

            List<ScheduleDay> scheduleDayList = context.scheduleService.getScheduleDayList(context);

            // Кнопки выбора времени занятия
            List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();

            int colCount = 2;
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            for (ScheduleDay scheduleDay : scheduleDayList) {

                if (rowInline.size() >= colCount) {
                    keyboardInline.add(rowInline);
                    rowInline = new ArrayList<>();
                }
                InlineKeyboardButton keyboardButton = new InlineKeyboardButton();

                StringBuilder text = new StringBuilder();
                if (scheduleDay.isNotSure()) {
                    text.append("❓ ");
                } else if (scheduleDay.isSubscribed()) {
                    text.append("✅ ");
                }
                text.append(Utils.toShortTime(scheduleDay.getTime()))
                        .append(scheduleDay.getWorkoutCode().length() > 0 ? " " + scheduleDay.getWorkoutCode() : "")
                        .append("\n");
                if (scheduleDay.getCount() > 0) {
                    text.append(scheduleDay.getCount()).append(" чел.");
                } else {
                    text.append("-");
                }

                keyboardButton.setText(text.toString());
                keyboardButton.setCallbackData("showSubscriptions|sId=" + scheduleDay.getId()
                        + ";d=" + Utils.dateToString(context.getDate()));
                rowInline.add(keyboardButton);

            }
            while (rowInline.size() > 0 && rowInline.size() < colCount) {
                InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
                keyboardButton.setText(" ");
                keyboardButton.setCallbackData(" ");
                rowInline.add(keyboardButton);
            }
            keyboardInline.add(rowInline);

            // Добавим кнопки перехода по датам
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            java.util.Date leftDate = calendar.getTime();

            calendar.add(Calendar.DAY_OF_MONTH, 2);
            Date rightDate = calendar.getTime();

            rowInline = new ArrayList<>();
            InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
//            keyboardButton.setText("⬅ " + Utils.dateToLocalString(leftDate)
//                    + " (" + new SimpleDateFormat("E").format(leftDate) + ")");
            keyboardButton.setText("       ⬅   Назад       ");
            keyboardButton.setCallbackData("showSchedule|gId=" + context.getGym().getId()
                    + ";d=" + Utils.dateToString(leftDate));
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
//            keyboardButton.setText(Utils.dateToLocalString(rightDate)
//                    + " (" + new SimpleDateFormat("E").format(rightDate) + ")" + " ➡");
            keyboardButton.setText("       Вперед   ➡       ");
            keyboardButton.setCallbackData("showSchedule|gId=" + context.getGym().getId()
                    + ";d=" + Utils.dateToString(rightDate));
            rowInline.add(keyboardButton);

            keyboardInline.add(rowInline);

            InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardInline);

            if (context.getMessageId() > 0) {

                EditMessageText message = new EditMessageText();
                message.enableMarkdown(true);
                message.setMessageId(context.getMessageId());
                message.setChatId(context.getUser().getChatId());
                message.setText(headerText);
                message.setReplyMarkup(replyKeyboard);
                try {
                    context.getBot().execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

            } else {

                SendMessage message = new SendMessage();
                message.enableMarkdown(true);
                message.setChatId(context.getUser().getChatId());
                message.setText(headerText);
                message.setReplyMarkup(replyKeyboard);
                try {
                    context.getBot().execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

            }

        }

        private void showSubscriptionsOnTime(BotContext context) {

            Date date = context.getDate();
            Date time = context.getSchedule().getTime();

            List<Subscription> subscriptions = context.subscriptionRepo.findAllByDateAndTimeAndGym(
                    new java.sql.Date(date.getTime()),
                    new java.sql.Time(time.getTime()),
                    context.getGym());

            StringBuilder text = new StringBuilder();
            text.append("*").append(new SimpleDateFormat("d MMMM yyyy").format(context.getDate()))
                    .append(" г.*\n")
                    .append(context.getSchedule().getWorkoutType().getName()).append(" на ")
                    .append(Utils.timeToString(context.getSchedule().getTime()))
                    .append("\n_Продолжительность ").append(context.getSchedule().getWorkoutType().getDuration())
                    .append(" ч._\n\n");

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
                } else if (subscription.getUser().getId() == context.getUser().getId()) {
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
            keyboardButton.setCallbackData("subscribe|sId=" + context.getSchedule().getId()
                    + ";d=" + Utils.dateToString(context.getDate())
                    + ";notSure=0");
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("❌ Не пойду");
            keyboardButton.setCallbackData("unSubscribe|sId=" + context.getSchedule().getId()
                    + ";d=" + Utils.dateToString(context.getDate()));
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("❓ Возможно");
            keyboardButton.setCallbackData("subscribe|sId=" + context.getSchedule().getId()
                    + ";d=" + Utils.dateToString(context.getDate())
                    + ";notSure=1");
            rowInline.add(keyboardButton);
            keyboardInline.add(rowInline);

            // Второй ряд кнопок
            rowInline = new ArrayList<>();
            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("➕ 1");
            keyboardButton.setCallbackData("addSubscribe|sId=" + context.getSchedule().getId()
                    + ";d=" + Utils.dateToString(context.getDate())
                    + ";c=1");
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("➖ 1");
            keyboardButton.setCallbackData("addSubscribe|sId=" + context.getSchedule().getId()
                    + ";d=" + Utils.dateToString(context.getDate())
                    + ";c=-1");
            rowInline.add(keyboardButton);

            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("⬅ Назад");
            keyboardButton.setCallbackData("showSchedule|gId=" + context.getGym().getId()
                    + ";d=" + Utils.dateToString(date));
            rowInline.add(keyboardButton);
            keyboardInline.add(rowInline);

            InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
            replyKeyboard.setKeyboard(keyboardInline);

            EditMessageText message = new EditMessageText();
            message.enableMarkdown(true);
            message.setMessageId(context.getMessageId());
            message.setChatId(context.getUser().getChatId());
            message.setText(text.toString());
            message.setReplyMarkup(replyKeyboard);

            try {
                context.getBot().execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

        }

        private boolean subscribeUser(BotContext context) {

            if (Utils.notAccessSubscribe(context)) {
                return false;
            }

            boolean result = false;
            Date date = context.getDate();
            Date time = context.getSchedule().getTime();

            Optional<Subscription> subscriptionOptional = context.subscriptionRepo.findByDateAndTimeAndGymAndUser(
                    new java.sql.Date(date.getTime()),
                    new java.sql.Time(time.getTime()),
                    context.getGym(),
                    context.getUser()
            );

            if (subscriptionOptional.isEmpty()) {
                Subscription subscribe = new Subscription();
                subscribe.setDate(new java.sql.Date(date.getTime()));
                subscribe.setTime(new java.sql.Time(time.getTime()));
                subscribe.setGym(context.getGym());
                subscribe.setUser(context.getUser());
                subscribe.setNotSure(context.isNotSure());
                if (!context.isNotSure()) {
                    subscribe.setCount(1);
                }
                context.subscriptionRepo.save(subscribe);
                result = true;
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

            if (Utils.notAccessSubscribe(context)) {
                return false;
            }

            boolean result = false;
            Date date = context.getDate();
            Date time = context.getSchedule().getTime();

            Optional<Subscription> subscriptionOptional = context.subscriptionRepo.findByDateAndTimeAndGymAndUser(
                    new java.sql.Date(date.getTime()),
                    new java.sql.Time(time.getTime()),
                    context.getGym(),
                    context.getUser()
            );

            if (subscriptionOptional.isPresent()) {
                Subscription subscribe = subscriptionOptional.get();
                context.subscriptionRepo.delete(subscribe);
                result = true;
            }

            return result;

        }

        private boolean addSubscribe(BotContext context) {

            if (Utils.notAccessSubscribe(context)) {
                return false;
            }

            boolean result = false;
            Date date = context.getDate();
            Date time = context.getSchedule().getTime();

            Optional<Subscription> subscriptionOptional = context.subscriptionRepo.findByDateAndTimeAndGymAndUser(
                    new java.sql.Date(date.getTime()),
                    new java.sql.Time(time.getTime()),
                    context.getGym(),
                    context.getUser()
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
    ADMIN_ADD_USER {

        @Override
        public void handleInput(BotContext context) {

            if (context.getUpdate().hasMessage()) {  // Обработка сообщения типа Message

                String inputText = context.getUpdate().getMessage().getText();
                if (inputText.equals("Да")) {

                    List<User> userList = context.userRepo.findAllByState(BotState.WAITING);
                    for (User user:userList) {
                        user.setState(BotState.SUBSCRIPTION);
                        context.userRepo.save(user);
                        sendMessage(context, "Ваша заявка одобрена. Добро пожаловать в наш клуб!", user.getChatId());
                        BotState.SUBSCRIPTION.showMainMenu(context.getBot(), user.getChatId());
                    }

                }

            }

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
        sendMessage(context, text, context.getUser().getChatId());
    }

    protected void sendMessage(BotContext context, String text, long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            context.getBot().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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

    public void showMainMenu(ChatBot bot, long chatId) {}

}
