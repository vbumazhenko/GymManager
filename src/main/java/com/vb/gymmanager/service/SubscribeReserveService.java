package com.vb.gymmanager.service;

import com.vb.gymmanager.bot.BotContext;
import com.vb.gymmanager.model.SubscribeReserve;
import com.vb.gymmanager.repository.SubscribeReserveRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubscribeReserveService {

    @Autowired
    public SubscribeReserveService(SubscribeReserveRepo subscribeReserveRepo) {
    }

    public int getLastOrder(BotContext context) {

        return context.subscribeReserveRepo.findAllByDateAndTimeAndGym(
                new java.sql.Date(context.getDate().getTime()),
                new java.sql.Time(context.getTime().getTime()),
                context.getGym()
        ).stream().mapToInt(SubscribeReserve::getNumber).max().orElse(0);

    }

}
