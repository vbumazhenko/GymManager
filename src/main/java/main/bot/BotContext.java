package main.bot;

import main.model.Gym;
import main.model.Schedule;
import main.model.User;
import main.repository.GymRepo;
import main.repository.ScheduleRepo;
import main.repository.SubscriptionRepo;
import main.repository.UserRepo;
import main.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Date;

@Component
public class BotContext {

    @Autowired
    protected UserRepo userRepo;

    @Autowired
    protected ScheduleRepo scheduleRepo;

    @Autowired
    protected ScheduleService scheduleService;

    @Autowired
    protected SubscriptionRepo subscriptionRepo;

    @Autowired
    protected GymRepo gymRepo;

    private ChatBot bot;
    private User currentUser;
    private Update update;
    private int messageId;
    private String callbackData;
    private Date date;
    private Gym gym;
    private Schedule schedule;
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
        schedule = null;
        date = new Date();
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
                        case "sId":  // scheduleId
                            scheduleRepo.findById(Integer.parseInt(keys[1])).ifPresent(value -> schedule = value);
                            gym = schedule.getGym();
                            break;
                        case "d":   // date
                            date = Utils.stringToDate(keys[1]);
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

    public Gym getGym() {
        return gym;
    }

    public void setGym(Gym gym) {
        this.gym = gym;
    }

    public Schedule getSchedule() {
        return schedule;
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
