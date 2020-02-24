package main.bot;

import main.model.*;
import main.repository.*;
import main.service.GymService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.sql.Date;
import java.util.*;

@Component
public class ChatBot extends TelegramLongPollingBot {

    @Autowired
    private BotContext context;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private GymService gymService;

    @Value("${bot.username}")
    private String botUserName;

    @Value("${bot.token}")
    private String botToken;

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

        long chatId;
        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
        } else {
            return;
        }

        Optional<User> userRec = userRepo.findByChatId(chatId);

        User currentUser;
        BotState state;

        if (userRec.isEmpty()) {
            state = BotState.getInitialState();
            currentUser = new User();
            currentUser.setChatId(chatId);
            currentUser.setState(state);
            currentUser.setDefaultGym(gymService.getDefaultGym());
            currentUser.setRegDate(new Date(new java.util.Date().getTime()));
            userRepo.save(currentUser);

            context.of(this, currentUser, update);
            state.enter(context);
        } else {
            currentUser = userRec.get();
            context.of(this, currentUser, update);
            state = currentUser.getState();
        }

        state.handleInput(context);

        do {
            state = state.nextState();
            state.enter(context);
        } while (!state.isInputNeeded());

        currentUser.setState(state);
        userRepo.save(currentUser);

    }

}
