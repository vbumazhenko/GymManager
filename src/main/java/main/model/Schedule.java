package main.model;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Time;

@Entity
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private Date date;
    private Time time;
    private int weekday;

    @ManyToOne
    @JoinColumn
    private Gym gym;

    @ManyToOne
    @JoinColumn
    private WorkoutType workoutType;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public java.util.Date getDate() {
        return new java.util.Date(date.getTime());
    }

    public void setDate(java.util.Date date) {
        this.date = new Date(date.getTime());
    }

    public java.util.Date getTime() {
        return new java.util.Date(time.getTime());
    }

    public void setTime(java.util.Date time) {
        this.time = new Time(time.getTime());
    }

    public int getWeekday() {
        return weekday;
    }

    public void setWeekday(int weekday) {
        this.weekday = weekday;
    }

    public Gym getGym() {
        return gym;
    }

    public void setGym(Gym gym) {
        this.gym = gym;
    }

    public WorkoutType getWorkoutType() {
        return workoutType;
    }

    public void setWorkoutType(WorkoutType workoutType) {
        this.workoutType = workoutType;
    }

}
