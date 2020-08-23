package com.vb.gymmanager.bot;

import org.springframework.stereotype.Component;

@Component
public class HandlerAddTime implements BotHandler {

    private BotState next;

    @Override
    public void enter() {

    }

    @Override
    public void handleInput() {

    }

    @Override
    public BotState nextState() {
        return next;
    }
}
