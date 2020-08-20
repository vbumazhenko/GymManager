package com.vb.gymmanager.repository;

import com.vb.gymmanager.model.WorkoutType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutTypeRepo extends CrudRepository<WorkoutType, Integer> {

    List<WorkoutType> findAll();

    Optional<WorkoutType> findByCode(String code);

}
