package com.vb.gymmanager.repository;

import com.vb.gymmanager.model.*;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Time;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscribeReserveRepo extends CrudRepository<SubscribeReserve, Integer> {

    List<SubscribeReserve> findAllByDateAndTimeAndGym(Date date, Time time, Gym gym);

    Optional<SubscribeReserve> findByDateAndTimeAndGymAndUser(Date date, Time time, Gym gym, User user);

}

