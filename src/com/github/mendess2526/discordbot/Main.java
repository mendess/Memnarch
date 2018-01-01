package com.github.mendess2526.discordbot;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.DiscordException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static com.github.mendess2526.discordbot.LoggerService.*;


public class Main {

    static volatile ScheduledFuture<?> leaveVoice;
    static Map<Long,Greetings> greetings;
    static Map<Long,ServerSettings> serverSettings;

    public static void main(String[] args){
        Config cfg;
        try{
            cfg = new Config();
        }catch (IOException e){
            log(null,"Can't find file " + e.getMessage(), ERROR);
            return;
        }

        String token = cfg.getToken();
        greetings = new HashMap<>();
        serverSettings = new HashMap<>();

        IDiscordClient client = createClient (token);

        if(client!=null){
            client.getDispatcher().registerListener(new Events());
            //TODO make restart more fun
        }else{
            log(null,"Client is null! Maybe you didn't add the token to settings.ini?", ERROR);
        }
    }

    static void initialiseGreetings(IGuild guild) {
        log(guild,"Initializing Greetings.", INFO);
        try {
            greetings.put(guild.getLongID(), new Greetings(guild));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void initialiseServerSettings(IGuild guild) {
        log(guild,"Initializing settings.", INFO);
        try {
            serverSettings.put(guild.getLongID(), new ServerSettings(guild));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static IDiscordClient createClient(String token){
        ClientBuilder cBuilder = new ClientBuilder().withToken(token).withRecommendedShardCount();
        try{
            return cBuilder.login();
        }catch (DiscordException e){
            e.printStackTrace();
            return null;
        }
    }
}