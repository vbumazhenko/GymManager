package com.vb.gymmanager.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HandlerEnterPhone implements BotHandler {

    public static final Logger LOG = LoggerFactory.getLogger(HandlerEnterPhone.class);
    public static final Marker INFO_MARKER = MarkerFactory.getMarker("INFO");
    public static final Marker ERROR_MARKER = MarkerFactory.getMarker("ERROR");

    private BotState next;

    @Autowired
    private ChatBot bot;

    @Override
    public void enter() {
        bot.sendMessage("Введите номер телефона");
    }

    @Override
    public void handleInput() {
        String inputText = bot.getUpdate().getMessage().getText().trim();
        String phone = Utils.getValidPhoneNumber(inputText);
        if (phone != null) {
            bot.getUser().setPhone(phone);
            next = BotState.SEND_QUERY_TO_ADMIN;
        } else {
            String message = "Неверный формат ввода номера телефона";
            bot.sendMessage(message);
            next = BotState.ENTER_PHONE;
            LOG.info(INFO_MARKER, message + ": " + inputText);
        }
    }

    @Override
    public BotState nextState() {
        return next;
    }
}
