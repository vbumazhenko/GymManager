package com.vb.gymmanager.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

@Component
public interface BotHandler {

    void enter();

    default void handleInput() {

    }

    BotState nextState();

    default boolean isInputNeeded() {
        return true;
    }

    default ReplyKeyboard getMainMenu() {
        return new ReplyKeyboardRemove();
    }

}
