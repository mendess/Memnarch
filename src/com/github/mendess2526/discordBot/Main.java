package com.github.mendess2526.discordbot;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;

import java.io.IOException;

public class Main {
    public static void main(String[] args){
        Config cfg;
        try{
            cfg = new Config();
        }catch (IOException e){
            LoggerService.log("Can't find file " + e.getMessage(),LoggerService.ERROR);
            return;
        }
        String token = cfg.getToken();

        IDiscordClient client = createClient (token);

        if(client!=null){
            client.getDispatcher().registerListener(new Events());
        }else{
            LoggerService.log("Client is null! Maybe you didn't add the token to settings.ini?",LoggerService.ERROR);
        }
    }
    private static IDiscordClient createClient(String token){
        ClientBuilder cBuilder = new ClientBuilder().withToken(token);
        try{
            return cBuilder.login();
        }catch (DiscordException e){
            e.printStackTrace();
            return null;
        }
    }
}
