package com.vb.gymmanager.bot;

public enum BotState {

    START,
    ENTER_NAME,
    ENTER_PHONE,
    SEND_QUERY_TO_ADMIN,
    WAITING,
    ACTIVE,
    BLOCKED,
    ADMIN,
    ADD_TIME;

    public static BotState getInitialState() {
        return START;
    }

}
