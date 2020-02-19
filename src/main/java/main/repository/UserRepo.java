package main.repository;

import main.bot.BotState;
import main.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.net.UnknownServiceException;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends CrudRepository<User, Integer> {

    Optional<User> findByChatId(Long chatId);

    List<User> findAllByIsAdmin(boolean isAdmin);

    List<User> findAllByState(BotState state);

    List<User> findAll();

}
