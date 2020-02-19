package main.repository;

import main.model.Gym;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GymRepo extends CrudRepository<Gym, Integer> {
}
