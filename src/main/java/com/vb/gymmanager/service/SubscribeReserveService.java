package com.vb.gymmanager.service;

import com.vb.gymmanager.bot.ChatBot;
import com.vb.gymmanager.model.SubscribeReserve;
import com.vb.gymmanager.repository.SubscribeReserveRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubscribeReserveService {

    private final ChatBot bot;
    private final SubscribeReserveRepo subscribeReserveRepo;

    @Autowired
    public SubscribeReserveService(ChatBot bot, SubscribeReserveRepo subscribeReserveRepo) {
        this.bot = bot;
        this.subscribeReserveRepo = subscribeReserveRepo;
    }

    public int getLastOrder() {

        return subscribeReserveRepo.findAllByDateAndTimeAndGym(
                new java.sql.Date(bot.getDate().getTime()),
                new java.sql.Time(bot.getTime().getTime()),
                bot.getGym()
        ).stream().mapToInt(SubscribeReserve::getNumber).max().orElse(0);

    }

}
