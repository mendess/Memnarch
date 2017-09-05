package com.github.mendess2526.discordbot;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

@SuppressWarnings("WeakerAccess")
public class BotUtils {
    public static void autoDelete(IMessage msg, IDiscordClient client) {

        try {
            client.getDispatcher().waitFor(MessageReceivedEvent.class);
        } catch (InterruptedException e) {
            LoggerService.log("Interrupted while waiting for a Message received event on sfxadd",LoggerService.ERROR);
        }
        msg.delete();
    }
}
