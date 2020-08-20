package com.vb.gymmanager.repository;

import com.vb.gymmanager.model.Gym;
import com.vb.gymmanager.model.Subscription;
import com.vb.gymmanager.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Time;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepo extends CrudRepository<Subscription, Integer> {

    List<Subscription> findAllByDateAndTimeAndGym(Date date, Time time, Gym gym);

    Optional<Subscription> findByDateAndTimeAndGymAndUser(Date date, Time time, Gym gym, User user);

}
