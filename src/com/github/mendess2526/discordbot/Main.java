package com.github.mendess2526.discordbot;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;

import java.io.IOException;

public class Main {
    public static void main(String[] args){
        /*Config cfg;
        try{
            cfg = new Config();
        }catch (IOException e){
            System.err.println("Can't find file " + e.getMessage());
            return;
        }
        String token = cfg.getToken();
        */
        String token = "lul";
        IDiscordClient client = createClient (token);

        if(client!=null){
            client.getDispatcher().registerListener(new Events());
        }else{
            System.err.println("Client is null");
        }

    }
    private static IDiscordClient createClient(String token/*, boolean login*/){
        ClientBuilder cBuilder = new ClientBuilder().withToken(token);
        try{
            return cBuilder.login();
        }catch (DiscordException e){
            e.printStackTrace();
            return null;
        }
        /*try{
            if(login){
                return cBuilder.login();
            }else{
                return cBuilder.build();
            }
        }catch (DiscordException e){
            e.printStackTrace();
            return null;
        }*/
    }
}
