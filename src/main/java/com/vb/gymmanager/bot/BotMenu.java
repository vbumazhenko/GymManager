package com.vb.gymmanager.bot;

import com.vb.gymmanager.model.User;
import com.vb.gymmanager.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class BotMenu {

    @Autowired
    private ChatBot bot;

    @Autowired
    private UserRepo userRepo;

    public void showMainMenu(ReplyKeyboard keyboard, User user) {

        // При выводе главного меню, счетчик сообщений необходимо сбросить
        if (bot.getUser().getId() != user.getId()) {
            user.setLastMessageId(0);
            userRepo.save(user);
        }

        SendMessage message = new SendMessage();
        message.setChatId(user.getChatId());
        message.setText(bot.getMainMenuHeader());
        message.setReplyMarkup(keyboard);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    public void showInlineKeyboardMarkup(String text, InlineKeyboardMarkup replyKeyboard) {

        if (bot.getMessageId() > 0) {

            EditMessageText message = new EditMessageText();
            message.enableMarkdown(true);
            message.setMessageId(bot.getMessageId());
            message.setChatId(bot.getUser().getChatId());
            message.setText(text);
            message.setReplyMarkup(replyKeyboard);
            try {
                bot.execute(message);
            } catch (TelegramApiException e) {
                if (!e.getMessage().contains("Error editing message text")) {
                    e.printStackTrace();
                }
            }

        } else {

            SendMessage message = new SendMessage();
            message.enableMarkdown(true);
            message.setChatId(bot.getUser().getChatId());
            message.setText(text);
            message.setReplyMarkup(replyKeyboard);
            try {
                bot.execute(message);
            } catch (TelegramApiException e) {
                if (!e.getMessage().contains("Error editing message text")) {
                    e.printStackTrace();
                }
            }

        }

    }

}
