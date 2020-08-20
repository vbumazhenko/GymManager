package com.vb.gymmanager.model;

import javax.persistence.*;

import java.sql.Date;
import java.sql.Time;

@Entity
@Table(name = "subscribe_reserve")
public class SubscribeReserve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private Date date;
    private Time time;

    @ManyToOne
    @JoinColumn
    private Gym gym;

    @ManyToOne
    @JoinColumn
    private User user;
    private int count;
    private boolean notSure;
    private int number;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Gym getGym() {
        return gym;
    }

    public void setGym(Gym gym) {
        this.gym = gym;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        this.time = time;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isNotSure() {
        return notSure;
    }

    public void setNotSure(boolean notSure) {
        this.notSure = notSure;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

}
