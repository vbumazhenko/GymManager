package com.vb.gymmanager.bot;

import com.vb.gymmanager.model.*;
import com.vb.gymmanager.repository.*;
import com.vb.gymmanager.service.GymService;
import com.vb.gymmanager.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ChatBot extends TelegramLongPollingBot {

    public static final Logger LOG = LoggerFactory.getLogger(ChatBot.class);
    public static final Marker INFO_MARKER = MarkerFactory.getMarker("INFO");
    public static final Marker ERROR_MARKER = MarkerFactory.getMarker("ERROR");

    private User user;
    private Update update;
    private int messageId;
    private String callbackData;
    private Date date;
    private Date time;
    private Gym gym;
    private WorkoutType workoutType;
    private boolean notSure;
    private int addCount;
    private User anotherUser;
    private BotState state;
    private String userListMode;
    private String mainMenuHeader;
    private int page;

    @Value("${bot.username}")
    private String botUserName;

    @Value("${bot.token}")
    private String botToken;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private GymRepo gymRepo;

    @Autowired
    private GymService gymService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private WorkoutTypeRepo workoutTypeRepo;

    @Autowired
    private HandlerStart handlerStart;

    @Autowired
    private HandlerEnterName handlerEnterName;

    @Autowired
    private HandlerEnterPhone handlerEnterPhone;

    @Autowired
    private HandlerSendQueryToAdmin handlerSendQueryToAdmin;

    @Autowired
    private HandlerWaiting handlerWaiting;

    @Autowired
    private HandlerActive handlerActive;

    @Autowired
    private HandlerBlocked handlerBlocked;

    @Autowired
    private HandlerAdmin handlerAdmin;

    @Autowired
    private HandlerAddTime handlerAddTime;

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) throws DataAccessResourceFailureException {

        this.update = update;

        long chatId;
        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
        } else {
            return;
        }

        Optional<User> userRec = userRepo.findByChatId(chatId);

        BotState botState;

        if (userRec.isEmpty()) {
            botState = BotState.getInitialState();
            user = new User();
            user.setChatId(chatId);
            user.setState(botState);
            user.setDefaultGym(gymService.getDefaultGym());
            user.setRegDate(new java.sql.Date(new Date().getTime()));
            userRepo.save(user);
            getHandler(botState).enter();
        } else {
            user = userRec.get();
            botState = user.getState();
        }

        setContext();

        if (update.hasMessage()) {
            String logMessage = "(state=" + botState + ",chatId=" + chatId + "), "
                    +  update.getMessage().getText();
            LOG.info(INFO_MARKER, logMessage);
        } else if (update.hasCallbackQuery()){
            String logMessage = "(state=" + botState + ",chatId=" + chatId + "), "
                    + update.getCallbackQuery().getData();
            LOG.info(INFO_MARKER, logMessage);
        }

        BotHandler botHandler = getHandler(botState);
        botHandler.handleInput();

        do {
            botState = botHandler.nextState();
            botHandler = getHandler(botState);
            botHandler.enter();
        } while (!botHandler.isInputNeeded());

        user.setState(botState);
        userRepo.save(user);

    }

    public BotHandler getHandler(BotState state) {
        switch (state) {
            case START:               return handlerStart;
            case ENTER_NAME:          return handlerEnterName;
            case ENTER_PHONE:         return handlerEnterPhone;
            case SEND_QUERY_TO_ADMIN: return handlerSendQueryToAdmin;
            case WAITING:             return handlerWaiting;
            case ACTIVE:              return handlerActive;
            case BLOCKED:             return handlerBlocked;
            case ADMIN:               return handlerAdmin;
            case ADD_TIME:            return handlerAddTime;
        }
        return handlerStart;
    }

    public void setContext() {

        messageId = 0;
        callbackData = "";
        workoutType = null;
        date = new Date();
        time = null;
        gym = user.getDefaultGym();
        notSure = false;
        addCount = 0;
        anotherUser = user;
        state = null;
        userListMode = "";
        mainMenuHeader = "Выберите действие";
        page = 1;

        if (update.hasMessage()) {
            if (update.getMessage().getMessageId() > user.getLastMessageId()) {
                user.setLastMessageId(update.getMessage().getMessageId());
            }
        }

        if (update.hasCallbackQuery()) {

            messageId = update.getCallbackQuery().getMessage().getMessageId();
            if (user.getLastMessageId() > 0 && user.getLastMessageId() < messageId) {
                user.setLastMessageId(messageId);
            }

            String[] args = update.getCallbackQuery().getData().split("\\|", 2);
            if (args.length == 2) {
                callbackData = args[0];
                args = args[1].split(";");
                for (String arg : args) {
                    String[] keys = arg.split("=", 2);
                    switch (keys[0]) {
                        case "d":   // date
                            date = Utils.stringToDate(keys[1]);
                            break;
                        case "t":   // time
                            time = Utils.stringToTime(keys[1]);
                            break;
                        case "gId": // gymId
                            gymRepo.findById(Integer.parseInt(keys[1])).ifPresent(value -> gym = value);
                            break;
                        case "notSure":
                            notSure = keys[1].equals("1");
                            break;
                        case "c":
                            addCount = Integer.parseInt(keys[1]);
                            break;
                        case "uId":
                            userRepo.findById(Integer.parseInt(keys[1])).ifPresent(value -> anotherUser = value);
                            break;
                        case "st":
                            state = BotState.valueOf(keys[1]);
                            break;
                        case "ulm":
                            userListMode = keys[1];
                            break;
                        case "p":
                            page = Integer.parseInt(keys[1]);
                    }
                }
            } else {
                callbackData = update.getCallbackQuery().getData();
            }

            if (time != null) {
                List<ScheduleDay> scheduleDayList = scheduleService.getScheduleDayList();
                scheduleDayList = scheduleDayList.stream()
                        .filter(s -> s.getTime().equals(time))
                        .collect(Collectors.toList());
                if (scheduleDayList.size() > 0) {
                    workoutTypeRepo.findById(scheduleDayList.get(0).getWorkoutId()).ifPresent(wt -> workoutType = wt);
                }
            }

        }

    }

    public void sendMessage(String text, User user) {

        // При выводе сообщения, счетчик сообщений пользователя необходимо сбросить
        if (this.user.getId() != user.getId()) {
            user.setLastMessageId(0);
            userRepo.save(user);
        }

        SendMessage message = new SendMessage();
        message.setChatId(user.getChatId());
        message.setText(text);
        message.enableMarkdown(true);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            LOG.error(ERROR_MARKER, e.getMessage());
        }

    }

    public void sendMessage(String text) {
        sendMessage(text, this.user);
    }

    public void sendMessageToAdmin(String text) {

        List<User> adminList = userRepo.findAllByIsAdmin(true);
        for (User admin : adminList) {
            sendMessage(text, admin);
        }

    }

    public Update getUpdate() {
        return update;
    }

    public User getUser() {
        return user;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public Date getTime() {
        return time;
    }

    public Gym getGym() {
        return gym;
    }

    public void setGym(Gym gym) {
        this.gym = gym;
    }

    public int getMessageId() {
        return messageId;
    }

    public String getMainMenuHeader() {
        return mainMenuHeader;
    }

    public void setMainMenuHeader(String mainMenuHeader) {
        this.mainMenuHeader = mainMenuHeader;
    }

    public String getCallbackData() {
        return callbackData;
    }

    public WorkoutType getWorkoutType() {
        return workoutType;
    }

    public boolean isNotSure() {
        return notSure;
    }

    public int getAddCount() {
        return addCount;
    }

    public User getAnotherUser() {
        return anotherUser;
    }

    public String getUserListMode() {
        return userListMode;
    }

    public BotState getState() {
        return state;
    }

    public int getPage() {
        return page;
    }

}
