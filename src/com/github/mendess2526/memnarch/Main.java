package com.github.mendess2526.memnarch;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;

import java.io.File;
import java.io.IOException;

import static com.github.mendess2526.memnarch.BotUtils.SERVERS_PATH;
import static com.github.mendess2526.memnarch.BotUtils.USERS_PATH;
import static com.github.mendess2526.memnarch.LoggerService.ERROR;
import static com.github.mendess2526.memnarch.LoggerService.log;


public class Main {

    public static void main(String[] args){
        try{
            initialiseFiles();
        }catch(IOException e){
            e.printStackTrace();
            return;
        }
        Config cfg;
        try{
            cfg = new Config();
        }catch (IOException e){
            log(null,"Can't find file " + e.getMessage(), ERROR);
            return;
        }

        String token = cfg.getToken();

        IDiscordClient client = createClient (token);

        if(client!=null){
            client.getDispatcher().registerListener(new Events());
        }else{
            log(null,"Client is null! Maybe you didn't add the token to settings.ini?", ERROR);
        }
    }

    private static void initialiseFiles() throws IOException{
        if(!BotUtils.mkFolder(null,BotUtils.DEFAULT_FILE_PATH)) throw new IOException("Couldn't create files folder");
        File servers = new File(SERVERS_PATH);
        File users = new File(USERS_PATH);
        if(!servers.exists() && !servers.createNewFile()) throw new IOException("Couldn't create: "+servers.getAbsolutePath());
        if(!users.exists() && !users.createNewFile()) throw new IOException("Couldn't create: "+users.getAbsolutePath());
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