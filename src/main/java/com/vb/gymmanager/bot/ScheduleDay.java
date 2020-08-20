package com.vb.gymmanager.bot;

import java.util.Date;

public class ScheduleDay {

    private int id;
    private int weekday;
    private Date time;
    private int workoutId;
    private String workoutCode;
    private String workoutName;
    private int count;
    private boolean subscribed;
    private boolean notSure;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getWeekday() {
        return weekday;
    }

    public void setWeekday(int weekday) {
        this.weekday = weekday;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public int getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(int workoutId) {
        this.workoutId = workoutId;
    }

    public String getWorkoutCode() {
        return workoutCode;
    }

    public void setWorkoutCode(String workoutCode) {
        this.workoutCode = workoutCode;
    }

    public String getWorkoutName() {
        return workoutName;
    }

    public void setWorkoutName(String workoutName) {
        this.workoutName = workoutName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public boolean isNotSure() {
        return notSure;
    }

    public void setNotSure(boolean notSure) {
        this.notSure = notSure;
    }

}
