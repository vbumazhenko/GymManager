package com.vb.gymmanager.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HandlerSendQueryToAdmin implements BotHandler {

    @Autowired
    private ChatBot bot;

    @Override
    public void enter() {
        String text = "Запрос на добавление от пользователя " + "[" +
                bot.getUser().getName() + "](" +
                "tg://user?id=" + bot.getUser().getChatId() + ")";
        bot.sendMessageToAdmin(text);
    }

    @Override
    public BotState nextState() {
        return BotState.WAITING;
    }

    @Override
    public boolean isInputNeeded() {
        return false;
    }

}
