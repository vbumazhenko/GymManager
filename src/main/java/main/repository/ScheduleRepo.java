package main.repository;

import main.model.Gym;
import main.model.Schedule;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Time;
import java.util.Optional;

@Repository
public interface ScheduleRepo extends CrudRepository<Schedule, Integer> {

    Optional<Schedule> findByDateAndTimeAndWeekdayAndGym(Date d, Time t, int w, Gym g);

}
