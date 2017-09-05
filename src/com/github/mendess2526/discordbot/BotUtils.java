package com.github.mendess2526.discordbot;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.RequestBuffer;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class BotUtils {
    private static String ERROR_SMTH_WRONG = "Something went wrong, contact the owner of the bot";

    public static void autoDelete(IMessage msg, IDiscordClient client, int delay) {

        try {
            TimeUnit.SECONDS.sleep(delay);
            client.getDispatcher().waitFor(MessageReceivedEvent.class,30,TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LoggerService.log("Interrupted while waiting for a Message received event on sfxadd",LoggerService.ERROR);
        }
        msg.delete();
    }
    public static void contactOwner(Event event, String msg){
        if(event instanceof MessageReceivedEvent){
            MessageReceivedEvent e = (MessageReceivedEvent) event;
            String guild = e.getGuild().getName();
            String channel = e.getChannel().getName();
            RequestBuffer.request(() -> e.getChannel().sendMessage(ERROR_SMTH_WRONG));
            RequestBuffer.request(() -> event.getClient().getApplicationOwner().getOrCreatePMChannel().
                    sendMessage(msg+"\n```" +
                                "[Guild  : "+guild+"]\n" +
                                "[Channel: "+channel+"]```"));
        }
    }
}
