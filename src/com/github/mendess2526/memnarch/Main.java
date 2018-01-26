package com.github.mendess2526.memnarch;

import com.github.mendess2526.memnarch.serversettings.ServerSettings;
import com.github.mendess2526.memnarch.sounds.Greetings;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.DiscordException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import static com.github.mendess2526.memnarch.BotUtils.SERVERS_PATH;
import static com.github.mendess2526.memnarch.BotUtils.USERS_PATH;
import static com.github.mendess2526.memnarch.LoggerService.*;


public class Main {

    static volatile Map<Long,ScheduledFuture<?>> leaveVoice;
    public static Map<Long,Greetings> greetings;
    public static Map<Long,ServerSettings> serverSettings;

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

    private static void initialiseFiles() throws IOException{
        if(!BotUtils.mkFolder(null,BotUtils.DEFAULT_FILE_PATH)) throw new IOException("Couldn't create files folder");
        File servers = new File(SERVERS_PATH);
        File users = new File(USERS_PATH);
        if(!servers.exists() && !servers.createNewFile()) throw new IOException("Couldn't create: "+servers.getAbsolutePath());
        if(!users.exists() && !users.createNewFile()) throw new IOException("Couldn't create: "+users.getAbsolutePath());
    }

    static void initialiseGreetings(IGuild guild) {
        log(guild,"Initializing Greetings.", INFO);
        try {
            greetings.put(guild.getLongID(), new Greetings(guild));
            log(guild,"Greetings initialized",SUCC);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void initialiseServerSettings(IGuild guild) {
        log(guild,"Initializing settings.", INFO);
        try {
            serverSettings.put(guild.getLongID(), new ServerSettings(guild));
            log(guild,"Server settings initialized",SUCC);
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