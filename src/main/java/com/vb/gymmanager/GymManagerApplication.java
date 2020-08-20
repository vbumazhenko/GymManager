package com.vb.gymmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
public class GymManagerApplication {

	public static void main(String[] args) {

		ApiContextInitializer.init();
		SpringApplication.run(GymManagerApplication.class, args);

		/**
		 * TODO-LIST:
		 * + 1. Добавить messageId в пользователя и не давать нажимать кнопки на старых сообщениях
		 * + 2. Не давать записаться, если занятие уже закончилось. Пока занятие идет, записаться можно
		 * + 3. Сделать админку для админа, минимум функций: просморт занятий, добавление и удаление участников
		 * 4. За час (опционально) до тренировки попросить определиться тех, кто под вопросом (написать сообщение)
		 * 5. Добавить логирование
		 * + 6. Изменить идентификацию расписания на дату+время
		 * 7. Добавить ограничение на то, чтобы нельзя было редактировать прошедшее время
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

	}

}
