package com.vb.gymmanager.repository;

import com.vb.gymmanager.model.Gym;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GymRepo extends CrudRepository<Gym, Integer> {

    List<Gym> findAll();

}
