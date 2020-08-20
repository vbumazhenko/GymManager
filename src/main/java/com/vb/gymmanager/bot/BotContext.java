package com.vb.gymmanager.bot;

import com.vb.gymmanager.model.Gym;
import com.vb.gymmanager.model.User;
import com.vb.gymmanager.model.WorkoutType;
import com.vb.gymmanager.repository.*;
import com.vb.gymmanager.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BotContext {

    @Autowired
    protected UserRepo userRepo;

    @Autowired
    public ScheduleRepo scheduleRepo;

    @Autowired
    protected ScheduleService scheduleService;

    @Autowired
    protected SubscribeReserveService subscribeReserveService;

    @Autowired
    protected SubscriptionRepo subscriptionRepo;

    @Autowired
    public SubscribeReserveRepo subscribeReserveRepo;

    @Autowired
    protected GymRepo gymRepo;

    @Autowired
    public WorkoutTypeRepo workoutTypeRepo;

    private ChatBot bot;
    private User currentUser;
    private Update update;
    private int messageId;
    private String callbackData;
    private Date date;
    private Date time;
    private Gym gym;
    private WorkoutType workoutType;
    private boolean notSure;
    private int addCount;
    private User user;
    private BotState state;
    private String userListMode;
    private String mainMenuHeader;

    public void of(ChatBot bot, User currentUser, Update update) {

        this.bot = bot;
        this.currentUser = currentUser;
        this.update = update;
        messageId = 0;
        callbackData = "";
        workoutType = null;
        date = new Date();
        time = null;
        gym = currentUser.getDefaultGym();
        notSure = false;
        addCount = 0;
        user = currentUser;
        state = null;
        userListMode = "";
        mainMenuHeader = "Выберите действие";

        if (update.hasMessage()) {
            if (update.getMessage().getMessageId() > currentUser.getLastMessageId()) {
                currentUser.setLastMessageId(update.getMessage().getMessageId());
            }
        }

        if (update.hasCallbackQuery()) {

            messageId = update.getCallbackQuery().getMessage().getMessageId();
            if (currentUser.getLastMessageId() > 0 && currentUser.getLastMessageId() < messageId) {
                currentUser.setLastMessageId(messageId);
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
                            userRepo.findById(Integer.parseInt(keys[1])).ifPresent(value -> user = value);
                            break;
                        case "st":
                            state = BotState.valueOf(keys[1]);
                            break;
                        case "ulm":
                            userListMode = keys[1];
                    }
                }
            } else {
                callbackData = update.getCallbackQuery().getData();
            }

            if (time != null) {
                List<ScheduleDay> scheduleDayList = scheduleService.getScheduleDayList(this);
                scheduleDayList = scheduleDayList.stream()
                        .filter(s -> s.getTime().equals(time))
                        .collect(Collectors.toList());
                if (scheduleDayList.size() > 0) {
                    workoutTypeRepo.findById(scheduleDayList.get(0).getWorkoutId()).ifPresent(wt -> workoutType = wt);
                }
            }

        }

    }

    public BotContext() {

    }

    public ChatBot getBot() {
        return bot;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Update getUpdate() {
        return update;
    }

    public int getMessageId() {
        return messageId;
    }

    public String getCallbackData() {
        return callbackData;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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

    public WorkoutType getWorkoutType() {
        return workoutType;
    }

    public boolean isNotSure() {
        return notSure;
    }

    public int getAddCount() {
        return addCount;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BotState getState() {
        return state;
    }

    public String getUserListMode() {
        return userListMode;
    }

    public String getMainMenuHeader() {
        return mainMenuHeader;
    }

    public void setMainMenuHeader(String mainMenuHeader) {
        this.mainMenuHeader = mainMenuHeader;
    }

}