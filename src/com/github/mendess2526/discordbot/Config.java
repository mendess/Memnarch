package com.github.mendess2526.discordbot;


import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("WeakerAccess")
public class Config {
    private String token;

    public Config() throws IOException {

        LoggerService.log("Reading from Ini",LoggerService.INFO);
        Wini iniFile;
        try{
            iniFile = new Wini(new File("./settings.ini"));
        }catch (IOException e){
            LoggerService.log(e.getMessage(), LoggerService.ERROR);
            return;
        }
        try{
            this.token = iniFile.get("connection","token",String.class);
        }catch (Exception e){
            LoggerService.log(e.getMessage(),LoggerService.ERROR);
            return;
        }
        LoggerService.log("ini read!",LoggerService.SUCC);
    }

    public String getToken() {
        return this.token;
    }
}
