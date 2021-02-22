package com.vb.gymmanager.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HandlerEnterName implements BotHandler {

    public static final Logger LOG = LoggerFactory.getLogger(HandlerEnterName.class);
    public static final Marker INFO_MARKER = MarkerFactory.getMarker("INFO");
    public static final Marker ERROR_MARKER = MarkerFactory.getMarker("ERROR");

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
            String message = "Неверный формат ввода имени, фамилии";
            bot.sendMessage(message);
            next = BotState.ENTER_NAME;
            LOG.info(INFO_MARKER, message + ": " + inputText);
        }
    }

    @Override
    public BotState nextState() {
        return next;
    }

}
