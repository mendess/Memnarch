package com.github.mendess2526.discordbot;


import org.ini4j.Wini;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.mendess2526.discordbot.LoggerService.*;

class Config {
    private String token;
    @SuppressWarnings("FieldCanBeLocal")
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    Config() throws IOException {
        File file = new File("./settings.ini");
        if(!file.exists()){
            if(!file.createNewFile()){
                return;
            }
        }
        log(null,"Reading from Ini", INFO);
        Wini iniFile;
        lock.readLock().lock();
        iniFile = new Wini(file);
        lock.readLock().unlock();
        if(!iniFile.containsKey("connection")){
            log(null,"No token set", ERROR);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter token:");
            this.token = br.readLine();
            br.close();
            iniFile.put("connection","token",this.token);
            try{
                lock.writeLock().lock();
                iniFile.store();
            }finally {
                lock.writeLock().unlock();
            }
        }
        this.token = iniFile.get("connection","token",String.class);
        log(null,"ini read!", SUCC);
    }

    String getToken() {
        return this.token;
    }
}
