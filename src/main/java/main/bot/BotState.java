package main.bot;

import main.model.Gym;
import main.model.Subscription;
import main.model.User;
import main.repository.UserRepo;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

            List<User> adminList = context.userRepo.findAllByIsAdmin(true);
            for (User admin : adminList) {
                String text = "Получен новый запрос от пользователя: " + context.getCurrentUser().getName();
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
                    showMainMenu(context);
                }

            } else if (context.getUpdate().hasCallbackQuery()) {

                if (context.getMessageId() == context.getCurrentUser().getLastMessageId()) {

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

        private void showSubscriptionsOnTime(BotContext context) {

            Date date = context.getDate();
            Date time = context.getSchedule().getTime();

            List<Subscription> subscriptions = context.subscriptionRepo.findAllByDateAndTimeAndGym(
                    new java.sql.Date(date.getTime()),
                    new java.sql.Time(time.getTime()),
                    context.getGym());

            StringBuilder text = new StringBuilder();
            String weekDay = new SimpleDateFormat("EEEE ").format(context.getDate());
            text.append("*").append(weekDay.substring(0, 1).toUpperCase()).append(weekDay.substring(1))
                    .append(new SimpleDateFormat("d MMMM yyyy").format(context.getDate()))
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

            showInlineKeyboardMarkup(context.getBot(), context.getMessageId(), context.getCurrentUser().getChatId(),
                    text.toString(), replyKeyboard);

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
                    context.getCurrentUser()
            );

            if (subscriptionOptional.isEmpty()) {
                Subscription subscribe = new Subscription();
                subscribe.setDate(new java.sql.Date(date.getTime()));
                subscribe.setTime(new java.sql.Time(time.getTime()));
                subscribe.setGym(context.getGym());
                subscribe.setUser(context.getCurrentUser());
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
                    context.getCurrentUser()
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

                List<Gym> gymList = context.gymRepo.findAll();
                for (Gym gym : gymList) {
                    if (context.getUpdate().getMessage().getText().contains(gym.getName())) {
                        context.setGym(gym);
                        context.setMainMenuHeader("Изменено на " + gym.getName());
                        changeGym(context);
                        showMainMenu(context);
                        if (context.getCurrentUser().getScheduleType() == 0) {
                            showScheduleOnDate(context);
                        } else {
                            showCommonScheduleOnDate(context);
                        }
                        return;
                    }
                }

                switch (context.getUpdate().getMessage().getText()) {
                    case "График тренировок":
                        if (context.getCurrentUser().getScheduleType() == 0) {
                            showScheduleOnDate(context);
                        } else {
                            showCommonScheduleOnDate(context);
                        }
                        break;
//                    case "Редактировать график":
////                        showScheduleOnDate(context);
//                        break;
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
                        showMainMenu(context);
                        break;
                }

            } else if (context.getUpdate().hasCallbackQuery()) {

                if (context.getMessageId() == context.getCurrentUser().getLastMessageId()) {

                    switch (context.getCallbackData()) {
                        case "showSchedule":
                            if (context.getCurrentUser().getScheduleType() == 0) {
                                showScheduleOnDate(context);
                            } else {
                                showCommonScheduleOnDate(context);
                            }
                            break;
                        case "chScheduleType":
                            changeScheduleType(context);
                            if (context.getCurrentUser().getScheduleType() == 0) {
                                showScheduleOnDate(context);
                            } else {
                                showCommonScheduleOnDate(context);
                            }
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
                context.setMainMenuHeader("Ваша заявка одобрена. Добро пожаловать в наш клуб!");
                showMainMenu(context);
            }

            // Сообщение при блокировке участника
            if (!previousState.equals(BotState.BLOCKED) && context.getState().equals(BotState.BLOCKED)) {
                context.setMainMenuHeader("Вы заблокированы. Для уточнения обратитесь к администратору");
                showMainMenu(context);
            }

            // Сообщение при разблокировке участника
            if (previousState.equals(BotState.BLOCKED) && !context.getState().equals(BotState.BLOCKED)) {
                context.setMainMenuHeader("Вы снова являетесь участником клуба. Выберите действие");
                showMainMenu(context);
            }

            // Сообщение при добавлении в администраторы
            if (!previousState.equals(BotState.ADMIN) && context.getState().equals(BotState.ADMIN)) {
                context.setMainMenuHeader("Вас сделали администратором");
                showMainMenu(context);
            }

            // Сообщение при удалении из администраторов
            if (previousState.equals(BotState.ADMIN) && !context.getState().equals(BotState.ADMIN)) {
                context.setMainMenuHeader("Вы больше не являетесь администратором");
                showMainMenu(context);
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
        sendMessage(context, text, context.getCurrentUser().getChatId());
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

    public ReplyKeyboardMarkup getMainMenu(BotContext context) {

        User user = context.getUser();

        // При выводе главного меню, счетчик сообщений необходимо сбросить
        if (context.getCurrentUser().getId() != user.getId()) {
            user.setLastMessageId(0);
            context.userRepo.save(user);
        }

        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow keyboardRow;

        if (user.isAdmin()) {

            keyboardRow = new KeyboardRow();
            keyboardRow.add("График тренировок");
//            keyboardRow.add("Полный график");
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

        } else {
            keyboardRow = new KeyboardRow();
            keyboardRow.add("График тренировок");
            keyboardRowList.add(keyboardRow);
        }

        ReplyKeyboardMarkup replyKeyboard = new ReplyKeyboardMarkup();
        replyKeyboard.setKeyboard(keyboardRowList);
        replyKeyboard.setResizeKeyboard(true);

        return replyKeyboard;

    }

    public void showMainMenu(BotContext context) {

        User user = context.getUser();

        SendMessage message = new SendMessage();
        message.setChatId(user.getChatId());
        message.setText(context.getMainMenuHeader());

        if (user.getState().equals(BotState.BLOCKED)) {
            message.setReplyMarkup(new ReplyKeyboardRemove());
        } else {
            message.setReplyMarkup(getMainMenu(context));
        }

        try {
            context.getBot().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    public void showCommonScheduleOnDate(BotContext context) {

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
        keyboardButton.setCallbackData("showSchedule|gId=" + context.getGym().getId()
                + ";d=" + Utils.dateToString(leftDate));
        rowInline.add(keyboardButton);

        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("\uD83D\uDD01");
        keyboardButton.setCallbackData("showSchedule|gId=" + context.getGym().getId()
                + ";d=" + Utils.dateToString(context.getDate()));
        rowInline.add(keyboardButton);

        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("  Вперед  ➡  ");
        keyboardButton.setCallbackData("showSchedule|gId=" + context.getGym().getId()
                + ";d=" + Utils.dateToString(rightDate));
        rowInline.add(keyboardButton);

        keyboardInline.add(rowInline);

        // Кнопка изменения вида расписания
        rowInline = new ArrayList<>();
        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("Изменить вид");
        keyboardButton.setCallbackData("chScheduleType|gId=" + context.getGym().getId()
                + ";d=" + Utils.dateToString(context.getDate()));
        rowInline.add(keyboardButton);
        keyboardInline.add(rowInline);

        // Вывод клавиатуры
        InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
        replyKeyboard.setKeyboard(keyboardInline);

        showInlineKeyboardMarkup(context.getBot(), context.getMessageId(), context.getCurrentUser().getChatId(),
                headerText.toString(), replyKeyboard);

    }

    public void showScheduleOnDate(BotContext context) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(context.getDate());

        StringBuilder headerText = new StringBuilder();
        headerText.append("График на *")
                .append(new SimpleDateFormat("EEEE d MMMM").format(context.getDate())).append("*\n")
                .append(context.getCurrentUser().getDefaultGym().getName());

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
            keyboardButton.setCallbackData("showSubscriptions|sId=" + scheduleDay.getId()
                    + ";d=" + Utils.dateToString(context.getDate()));
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
        keyboardButton.setCallbackData("showSchedule|gId=" + context.getGym().getId()
                + ";d=" + Utils.dateToString(leftDate));
        rowInline.add(keyboardButton);

        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("\uD83D\uDD01");
        keyboardButton.setCallbackData("showSchedule|gId=" + context.getGym().getId()
                + ";d=" + Utils.dateToString(context.getDate()));
        rowInline.add(keyboardButton);

        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("  Вперед  ➡  ");
        keyboardButton.setCallbackData("showSchedule|gId=" + context.getGym().getId()
                + ";d=" + Utils.dateToString(rightDate));
        rowInline.add(keyboardButton);

        keyboardInline.add(rowInline);

        // Кнопка изменения вида расписания
        if (context.getCurrentUser().isAdmin()) {
            rowInline = new ArrayList<>();
            keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("Изменить вид");
            keyboardButton.setCallbackData("chScheduleType|gId=" + context.getGym().getId()
                    + ";d=" + Utils.dateToString(context.getDate()));
            rowInline.add(keyboardButton);
            keyboardInline.add(rowInline);
        }

        // Вывод клавиатуры
        InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
        replyKeyboard.setKeyboard(keyboardInline);

        showInlineKeyboardMarkup(context.getBot(), context.getMessageId(), context.getCurrentUser().getChatId(),
                headerText.toString(), replyKeyboard);

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

    public void changeScheduleType(BotContext context) {

        User user = context.getCurrentUser();
        if (user.getScheduleType() == 0) {
            user.setScheduleType(1);
        } else {
            user.setScheduleType(0);
        }

    }

}
