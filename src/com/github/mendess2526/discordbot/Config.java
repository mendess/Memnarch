package com.github.mendess2526.discordbot;


import org.ini4j.Wini;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings("WeakerAccess")
public class Config {
    private String token;
    private ReadWriteLock lock;

    public Config() throws IOException {
        lock = new ReentrantReadWriteLock();
        File file = new File("./settings.ini");
        if(!file.exists()){
            if(!file.createNewFile()){
                return;
            }
        }
        LoggerService.log(null,"Reading from Ini",LoggerService.INFO);
        Wini iniFile;
        iniFile = new Wini(file);
        if(!iniFile.containsKey("connection")){
            LoggerService.log(null,"No token set",LoggerService.ERROR);
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
        LoggerService.log(null,"ini read!",LoggerService.SUCC);
    }

    public String getToken() {
        return this.token;
    }

}
