package main.model;

import main.bot.BotState;

import javax.persistence.*;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String phone;
    private Long chatId;
    private int tgId;

    @ManyToOne
    @JoinColumn
    private Gym defaultGym;

    @Enumerated(EnumType.STRING)
    private BotState state;
    private boolean isAdmin;
    private int lastMessageId;

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

    public int getTgId() {
        return tgId;
    }

    public void setTgId(int tgId) {
        this.tgId = tgId;
    }

    public int getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(int lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

}
