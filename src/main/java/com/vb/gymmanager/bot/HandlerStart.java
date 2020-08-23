package com.vb.gymmanager.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HandlerStart implements BotHandler {

    @Autowired
    private ChatBot bot;

    @Override
    public void enter() {
        String text = "Для начала давайте познакомимся";
        bot.sendMessage(text);
    }

    @Override
    public BotState nextState() {
        return BotState.ENTER_NAME;
    }

}
