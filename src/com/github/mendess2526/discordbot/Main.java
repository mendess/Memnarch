package com.github.mendess2526.discordbot;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;

public class Main {
    public static void main(String[] args){
        IDiscordClient client = createClient ("token", false);

    }
    private static IDiscordClient createClient(String token, boolean login){
        ClientBuilder cBuilder = new ClientBuilder().withToken(token);
        try{
            if(login){
                return cBuilder.login();
            }else{
                return cBuilder.build();
            }
        }catch (DiscordException e){
            e.printStackTrace();
            return null;
        }
    }
}
