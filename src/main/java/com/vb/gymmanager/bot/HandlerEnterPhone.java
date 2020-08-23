package com.vb.gymmanager.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HandlerEnterPhone implements BotHandler {

    private BotState next;

    @Autowired
    private ChatBot bot;

    @Override
    public void enter() {
        bot.sendMessage("Введите номер телефона");
    }

    @Override
    public void handleInput() {
        String phone = Utils.getValidPhoneNumber(bot.getUpdate().getMessage().getText().trim());
        if (phone != null) {
            bot.getUser().setPhone(phone);
            next = BotState.SEND_QUERY_TO_ADMIN;
        } else {
            bot.sendMessage("Неверный формат ввода номера телефона");
            next = BotState.ENTER_PHONE;
        }
    }

    @Override
    public BotState nextState() {
        return next;
    }
}
