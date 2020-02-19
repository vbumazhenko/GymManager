package main.repository;

import main.model.Gym;
import main.model.Subscription;
import main.model.User;
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
