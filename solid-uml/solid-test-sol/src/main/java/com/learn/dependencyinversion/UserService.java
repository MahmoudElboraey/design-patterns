package com.learn.dependencyinversion;

public class UserService {
    private final DbManager dbManager;

    public UserService(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    public User register (String email){
        return dbManager.insert(new User(email));
    }
}
