package com.vb.gymmanager.model;

import com.vb.gymmanager.bot.BotState;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;

    @Column(length = 20)
    private String phone;
    private Long chatId;

    @ManyToOne
    @JoinColumn
    private Gym defaultGym;

    @Enumerated(EnumType.STRING)
    private BotState state;
    private boolean isAdmin;
    private int lastMessageId;
    private Date regDate;
    private Date inputDate;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Gym getDefaultGym() {
        return defaultGym;
    }

    public void setDefaultGym(Gym defaultGym) {
        this.defaultGym = defaultGym;
    }

    public BotState getState() {
        return state;
    }

    public void setState(BotState state) {
        this.state = state;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public int getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(int lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public Date getRegDate() {
        return regDate;
    }

    public void setRegDate(Date regDate) {
        this.regDate = regDate;
    }

    public java.util.Date getInputDate() {
        return new java.util.Date(inputDate.getTime());
    }

    public void setInputDate(java.util.Date inputDate) {
        this.inputDate = new Date(inputDate.getTime());
    }

}
