package com.github.mendess2526.discordbot;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.DiscordException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@SuppressWarnings("WeakerAccess")
public class Main {

    @SuppressWarnings("WeakerAccess")
    public static volatile ScheduledFuture<?> leaveVoice;
    public static Map<Long,Greetings> greetings;
    public static Map<Long,ServerSettings> serverSettings;

    public static void main(String[] args){
        Config cfg;
        try{
            cfg = new Config();
        }catch (IOException e){
            LoggerService.log(null,"Can't find file " + e.getMessage(),LoggerService.ERROR);
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
            LoggerService.log(null,"Client is null! Maybe you didn't add the token to settings.ini?",LoggerService.ERROR);
        }
    }
    public static void initialiseGreetings(IGuild guild) {
        LoggerService.log(guild,"Initializing Greetings.",LoggerService.INFO);
        try {
            greetings.put(guild.getLongID(), new Greetings(guild));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void initialiseServerSettings(IGuild guild) {
        LoggerService.log(guild,"Initializing settings.",LoggerService.INFO);
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
