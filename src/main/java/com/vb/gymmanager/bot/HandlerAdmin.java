package com.vb.gymmanager.bot;

import com.vb.gymmanager.model.Gym;
import com.vb.gymmanager.model.Schedule;
import com.vb.gymmanager.model.Subscription;
import com.vb.gymmanager.model.User;
import com.vb.gymmanager.repository.GymRepo;
import com.vb.gymmanager.repository.ScheduleRepo;
import com.vb.gymmanager.repository.SubscriptionRepo;
import com.vb.gymmanager.repository.UserRepo;
import com.vb.gymmanager.service.ScheduleService;
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
import java.util.stream.Collectors;

@Component
public class HandlerAdmin implements BotHandler {

    private BotState next;

    @Autowired
    private ChatBot bot;

    @Autowired
    private BotMenu botMenu;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private GymRepo gymRepo;

    @Autowired
    private SubscriptionRepo subscriptionRepo;

    @Autowired
    private ScheduleRepo scheduleRepo;

    @Autowired
    private ScheduleService scheduleService;

    @Override
    public void enter() {

        if (bot.getUpdate().hasCallbackQuery()) {
            AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
            answerCallbackQuery.setCallbackQueryId(bot.getUpdate().getCallbackQuery().getId());
            try {
                bot.execute(answerCallbackQuery);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void handleInput() {

        next = BotState.ADMIN;

        if (bot.getUpdate().hasMessage()) {

            List<Gym> gymList = gymRepo.findAll();
            for (Gym gym : gymList) {
                if (bot.getUpdate().getMessage().getText().contains(gym.getName())) {
                    bot.setGym(gym);
                    bot.setMainMenuHeader("Изменено на " + gym.getName());
                    changeGym(bot.getUser());
                    botMenu.showMainMenu(getMainMenu(), bot.getUser());
                    showScheduleOnDate();
                    return;
                }
            }

            switch (bot.getUpdate().getMessage().getText()) {
                case "График тренировок":
                    showScheduleOnDate();
                    break;
                case "Заявки":
                    showWaitingUsers();
                    break;
                case "Все участники":
                    showUserList();
                    break;
                case "Заблокированные":
                    showBlockedUsers();
                    break;
                default:
                    botMenu.showMainMenu(getMainMenu(), bot.getUser());
                    break;
            }

        } else if (bot.getUpdate().hasCallbackQuery()) {

            if (bot.getMessageId() == bot.getUser().getLastMessageId()) {

                switch (bot.getCallbackData()) {
                    case "showScheduleOnDate":
                        showScheduleOnDate();
                        break;
                    case "editSchedule":
                        editSchedule();
                        break;
                    case "addTime":
                        bot.getUser().setInputDate(bot.getDate());
                        next = BotState.ADD_TIME;
                        break;
                    case "delTime":
                        deleteTimeOnSchedule();
                        break;
                    case "showWaitingUsers":
                        showWaitingUsers();
                        break;
                    case "showUserList":
                        showUserList();
                        break;
                    case "showBlockedUsers":
                        showBlockedUsers();
                        break;
                    case "showUserData":
                        showUserData();
                        break;
                    case "chState":
                        changeState();
                        showUserData();
                        break;
                    case "showGymList":
                        showGymList();
                        break;
                    case "chGym":
                        if (changeGym(bot.getAnotherUser())) {
                            showGymList();
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
    public ReplyKeyboard getMainMenu() {

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

        List<Gym> gymList = gymRepo.findAll();

        keyboardRow = new KeyboardRow();
        for (Gym gym : gymList) {
            StringBuilder buttonText = new StringBuilder();
            // TODO: Разобраться с другим пользователем
            if (bot.getAnotherUser().getDefaultGym().getId() == gym.getId()) {
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

    private void showScheduleOnDate() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(bot.getDate());

        StringBuilder headerText = new StringBuilder();
        headerText.append("График на *")
                .append(new SimpleDateFormat("EEEE d MMMM").format(bot.getDate())).append("*\n")
                .append(bot.getUser().getDefaultGym().getName()).append("\n");

        List<ScheduleDay> scheduleDayList = scheduleService.getScheduleDayList();

        for (ScheduleDay scheduleDay : scheduleDayList) {

            headerText.append("\n*").append(Utils.timeToString(scheduleDay.getTime()))
                    .append(scheduleDay.getWorkoutCode().length() > 0 ? " " + scheduleDay.getWorkoutCode() : "");
            if (scheduleDay.getCount() > 0) {
                headerText.append(" - ").append(scheduleDay.getCount());
            }
            headerText.append("*\n");
            List<Subscription> subscriptions = subscriptionRepo.findAllByDateAndTimeAndGym(
                    new java.sql.Date(bot.getDate().getTime()),
                    new java.sql.Time(scheduleDay.getTime().getTime()),
                    bot.getGym());

            if (subscriptions.size() == 0) {
                headerText.append("(пусто)\n");
            }

            for (Subscription subscription : subscriptions) {
                if (subscription.isNotSure()) {
                    headerText.append("❓ ");
                } else if (subscription.getUser().getId() == bot.getUser().getId()) {
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
        keyboardButton.setCallbackData("showScheduleOnDate|d=" + Utils.dateToString(bot.getDate()));
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
        keyboardButton.setCallbackData("editSchedule|d=" + Utils.dateToString(bot.getDate()));
        rowInline.add(keyboardButton);
        keyboardInline.add(rowInline);

        // Вывод клавиатуры
        InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
        replyKeyboard.setKeyboard(keyboardInline);

        botMenu.showInlineKeyboardMarkup(headerText.toString(), replyKeyboard);

    }

    private void deleteTimeOnSchedule() {

        scheduleService.copyScheduleOnDay();

        Optional<Schedule> schedules = scheduleRepo.findByDateAndTimeAndWeekdayAndGym(
                new java.sql.Date(bot.getDate().getTime()),
                new java.sql.Time(bot.getTime().getTime()),
                0,
                bot.getUser().getDefaultGym()
        );

        schedules.ifPresent(schedule -> scheduleRepo.delete(schedule));

        // После удаления, необходимо отменить подписку пользователей на это время
        List<Subscription> subscriptionList = subscriptionRepo.findAllByDateAndTimeAndGym(
                new java.sql.Date(bot.getDate().getTime()),
                new java.sql.Time(bot.getTime().getTime()),
                bot.getUser().getDefaultGym()
        );

        // Отправка сообщения об отмене занятия
        for (Subscription subscription : subscriptionList) {
            String text = "Тренировка на *" + Utils.timeToString(subscription.getTime()) + " "
                    + new SimpleDateFormat("EEEE d MMMM yyyy").format(bot.getDate())
                    + " г.* отменена. Пожалуйста, запишитесь на другое время";
            bot.sendMessage(text, subscription.getUser());
        }

        // Удаление подписки всех пользователей на данное время
        subscriptionRepo.deleteAll(subscriptionList);

        // Вывод обновленного расписания
        editSchedule();

    }

    private void showWaitingUsers() {

        List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline;

        List<User> userList = userRepo.findAll();
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

        String text = bot.getUser().getDefaultGym().getName() + "\n" +
                "Новые заявки (" + count + "):";

        botMenu.showInlineKeyboardMarkup(text, replyKeyboard);

    }

    private void showUserList() {

        List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline;

        List<User> userList = userRepo.findAllByDefaultGym(bot.getUser().getDefaultGym());
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

        String text = bot.getUser().getDefaultGym().getName() + "\n" +
                "Все участники (" + count + "):";

        botMenu.showInlineKeyboardMarkup(text, replyKeyboard);

    }

    private void showBlockedUsers() {

        List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline;

        List<User> userList = userRepo.findAllByDefaultGym(bot.getUser().getDefaultGym());
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

        String text = bot.getUser().getDefaultGym().getName() + "\n" +
                "Заблокированные (" + count + "):";

        botMenu.showInlineKeyboardMarkup(text, replyKeyboard);

    }

    private void showUserData() {

        User user = bot.getAnotherUser();
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

        if (user.getId() != bot.getUser().getId()) {

            // Добавить / заблокировать / разблокировать
            if (user.getState().equals(BotState.WAITING)) {
                rowInline = new ArrayList<>();
                keyboardButton = new InlineKeyboardButton();
                keyboardButton.setText("Добавить");
                keyboardButton.setCallbackData("chState|uId=" + user.getId()
                        + ";st=" + BotState.ACTIVE
                        + ";ulm=" + bot.getUserListMode());
                rowInline.add(keyboardButton);
                keyboardInline.add(rowInline);
            }
            if (user.getState().equals(BotState.ACTIVE) || user.getState().equals(BotState.WAITING)) {
                rowInline = new ArrayList<>();
                keyboardButton = new InlineKeyboardButton();
                keyboardButton.setText("Заблокировать");
                keyboardButton.setCallbackData("chState|uId=" + user.getId()
                        + ";st=" + BotState.BLOCKED
                        + ";ulm=" + bot.getUserListMode());
                rowInline.add(keyboardButton);
                keyboardInline.add(rowInline);
            }
            if (user.getState().equals(BotState.BLOCKED)) {
                rowInline = new ArrayList<>();
                keyboardButton = new InlineKeyboardButton();
                keyboardButton.setText("Разблокировать");
                keyboardButton.setCallbackData("chState|uId=" + user.getId()
                        + ";st=" + BotState.ACTIVE
                        + ";ulm=" + bot.getUserListMode());
                rowInline.add(keyboardButton);
                keyboardInline.add(rowInline);
            }

            // Изменить клуб (только для активных участников)
            if (user.getState().equals(BotState.ACTIVE)) {
                rowInline = new ArrayList<>();
                keyboardButton = new InlineKeyboardButton();
                keyboardButton.setText("Изменить клуб");
                keyboardButton.setCallbackData("showGymList|uId=" + user.getId()
                        + ";ulm=" + bot.getUserListMode());
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
                        + ";ulm=" + bot.getUserListMode());
                rowInline.add(keyboardButton);
                keyboardInline.add(rowInline);
            } else if (user.getState().equals(BotState.ADMIN)) {
                rowInline = new ArrayList<>();
                keyboardButton = new InlineKeyboardButton();
                keyboardButton.setText("Удалить из администраторов");
                keyboardButton.setCallbackData("chState|uId=" + user.getId()
                        + ";st=" + BotState.ACTIVE
                        + ";ulm=" + bot.getUserListMode());
                rowInline.add(keyboardButton);
                keyboardInline.add(rowInline);
            }

        }

        // Назад
        rowInline = new ArrayList<>();
        keyboardButton = new InlineKeyboardButton();
        keyboardButton.setText("« Назад");
        keyboardButton.setCallbackData(bot.getUserListMode());
        rowInline.add(keyboardButton);
        keyboardInline.add(rowInline);

        InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
        replyKeyboard.setKeyboard(keyboardInline);

        botMenu.showInlineKeyboardMarkup(text.toString(), replyKeyboard);

    }

    private void changeState() {

        User user = bot.getAnotherUser();
        BotState previousState = user.getState();
        user.setState(bot.getState());
        user.setAdmin(bot.getState().equals(BotState.ADMIN));
        userRepo.save(user);

        // Сообщение при добавлении нового участника
        if (previousState.equals(BotState.WAITING) && bot.getState().equals(BotState.ACTIVE)) {
            bot.setMainMenuHeader("Ваша заявка одобрена.\nДобро пожаловать в наш клуб!");
            botMenu.showMainMenu(bot.getHandler(bot.getAnotherUser().getState()).getMainMenu(), user);
        }

        // Сообщение при блокировке участника
        if (!previousState.equals(BotState.BLOCKED) && bot.getState().equals(BotState.BLOCKED)) {
            bot.setMainMenuHeader("Вы заблокированы.\nДля уточнения обратитесь к администратору");
            botMenu.showMainMenu(bot.getHandler(bot.getAnotherUser().getState()).getMainMenu(), user);
        }

        // Сообщение при разблокировке участника
        if (previousState.equals(BotState.BLOCKED) && !bot.getState().equals(BotState.BLOCKED)) {
            bot.setMainMenuHeader("Вы снова являетесь участником клуба.\nВыберите действие");
            botMenu.showMainMenu(bot.getHandler(bot.getAnotherUser().getState()).getMainMenu(), user);
        }

        // Сообщение при добавлении в администраторы
        if (!previousState.equals(BotState.ADMIN) && bot.getState().equals(BotState.ADMIN)) {
            bot.setMainMenuHeader("Вас сделали администратором");
            botMenu.showMainMenu(bot.getHandler(bot.getAnotherUser().getState()).getMainMenu(), user);
        }

        // Сообщение при удалении из администраторов
        if (previousState.equals(BotState.ADMIN) && !bot.getState().equals(BotState.ADMIN)) {
            bot.setMainMenuHeader("Вы больше не являетесь администратором");
            botMenu.showMainMenu(bot.getHandler(bot.getAnotherUser().getState()).getMainMenu(), user);
        }

    }

    private void showGymList() {

        User user = bot.getAnotherUser();
        String headerText = "Изменить текущий клуб";
        if (user.getId() == bot.getUser().getId()) {
            headerText += ":";
        } else {
            headerText += " для\n"
                    + "[" + user.getName() + "](tg://user?id=" + user.getChatId() + ")";
        }

        List<Gym> gymList = gymRepo.findAll();

        List<List<InlineKeyboardButton>> keyboardInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline;

        for (Gym gym : gymList) {

            StringBuilder text = new StringBuilder();
            if (user.getDefaultGym().getId() == gym.getId()) {
                text.append("✅ ");
            }
            text.append(gym.getName());

            InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText(text.toString());
            keyboardButton.setCallbackData("chGym|uId=" + user.getId()
                    + ";gId=" + gym.getId()
                    + ";ulm=" + bot.getUserListMode());

            rowInline = new ArrayList<>();
            rowInline.add(keyboardButton);
            keyboardInline.add(rowInline);

        }

        // Назад
        if (user.getId() != bot.getUser().getId()) {
            rowInline = new ArrayList<>();
            InlineKeyboardButton keyboardButton = new InlineKeyboardButton();
            keyboardButton.setText("« Назад");
            keyboardButton.setCallbackData("showUserData|uId=" + user.getId()
                    + ";ulm=" + bot.getUserListMode());
            rowInline.add(keyboardButton);
            keyboardInline.add(rowInline);
        }

        InlineKeyboardMarkup replyKeyboard = new InlineKeyboardMarkup();
        replyKeyboard.setKeyboard(keyboardInline);

        botMenu.showInlineKeyboardMarkup(headerText, replyKeyboard);

    }

    private boolean changeGym(User user) {

        if (user.getDefaultGym().getId() != bot.getGym().getId()) {
            user.setDefaultGym(bot.getGym());
            userRepo.save(user);
            return true;
        }

        return false;

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

}
