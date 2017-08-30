package com.github.mendess2526.discordbot;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Config {
    private String token;

    public Config() throws IOException {
        FileReader fr = new FileReader("settings.txt");
        BufferedReader textReader = new BufferedReader(fr);
        this.token = textReader.readLine();

    }
    public String getToken(){
        return this.token;
    }
}
