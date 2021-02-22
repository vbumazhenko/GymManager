package com.vb.gymmanager.bot;

import org.springframework.stereotype.Component;

@Component
public class HandlerBlocked implements BotHandler {

    @Override
    public void enter() {

    }

    @Override
    public void handleInput() {

    }

    @Override
    public BotState nextState() {
        return BotState.BLOCKED;
    }

}
