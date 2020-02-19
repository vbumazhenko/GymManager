package main.repository;

import main.model.WorkoutType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutTypeRepo extends CrudRepository<WorkoutType, Integer> {
}
