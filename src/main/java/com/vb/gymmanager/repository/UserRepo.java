package com.vb.gymmanager.repository;

import com.vb.gymmanager.model.Gym;
import com.vb.gymmanager.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends CrudRepository<User, Integer> {

    Optional<User> findByChatId(Long chatId);

    List<User> findAllByIsAdmin(boolean isAdmin);

    List<User> findAllByDefaultGym(Gym gym);

    List<User> findAll();

}
