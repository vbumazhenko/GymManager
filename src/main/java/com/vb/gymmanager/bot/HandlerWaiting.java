package com.vb.gymmanager.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HandlerWaiting implements BotHandler {

    @Autowired
    private ChatBot bot;

    @Override
    public void enter() {
        bot.sendMessage("Запрос на добавление участника отправлен администратору. " +
                "Ожидайте ответ в ближайшее время.");
    }

    @Override
    public BotState nextState() {
        return BotState.WAITING;
    }
}
