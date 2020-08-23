package com.vb.gymmanager.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HandlerEnterName implements BotHandler {

    private BotState next;

    @Autowired
    private ChatBot bot;

    @Override
    public void enter() {
        bot.sendMessage("Введите ваши имя и фамилию,\nнапример, Андрей Иванов");
    }

    @Override
    public void handleInput() {
        String inputText = bot.getUpdate().getMessage().getText().trim();
        if (Utils.isValidName(inputText)) {
            bot.getUser().setName(inputText);
            next = BotState.ENTER_PHONE;
        } else {
            bot.sendMessage("Неверный формат ввода имени, фамилии");
            next = BotState.ENTER_NAME;
        }
    }

    @Override
    public BotState nextState() {
        return next;
    }

}
