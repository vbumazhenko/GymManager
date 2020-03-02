package main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

@SpringBootApplication
public class Application {

    public static void main(String[] args) throws TelegramApiRequestException {
        ApiContextInitializer.init();
        SpringApplication.run(Application.class, args);

        /**
         * TODO-LIST:
         * + 1. Добавить messageId в пользователя и не давать нажимать кнопки на старых сообщениях
         * + 2. Не давать записаться, если занятие уже закончилось. Пока занятие идет, записаться можно
         * + 3. Сделать админку для админа, минимум функций: просморт занятий, добавление и удаление участников
         * 4. За час (опционально) до тренировки попросить определиться тех, кто под вопросом (написать сообщение)
         * 5. Добавить логирование
         * 6. Изменить идентификацию расписания на дату+время
         * 7. Добавить ограничение на то, чтобы нельзя быдо редактировать прошедшее время
         *
         *
         *
         * ВОПРОСЫ:
         * + 1. Что делать, если кто-то записался на занятие, а его отменили - скорее всего послать сообщение пользователю
         * о том, что занятие отменено
         *
         *
         * ПЛАНЫ на будущее:
         * Для пользователя сделать возможность оповещать о тренировке с заданием времени оповещения.
         *
         *
         *
         */

        //ApplicationContext applicationContext = SpringApplication.run(Application.class, args);

        // Активирует Webhook у бота после загузки Tomcat.
        //ChatBot2 chatBot2 = applicationContext.getBean(ChatBot2.class);
        //chatBot2.setWebhook("https://1e0c4096.ngrok.io/bot", null);
    }

}
