package com.github.mendess2526.discordbot;

import sx.blah.discord.api.events.Event;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.util.RequestBuffer;

public class Events implements IListener {
    private final static String BOT_PREFIX = "!";
    public void handle(Event event) {
        if(event instanceof MessageReceivedEvent){
            handleMessageReceived((MessageReceivedEvent) event);
        }
        /*if(event instanceof ReactionEvent){
            handleReactionEvent((ReactionEvent) event);
        }*/
    }

    /*private void handleReactionEvent(ReactionEvent event) {

    }*/

    private void handleMessageReceived(MessageReceivedEvent event) {
        String command[] = event.getMessage().getContent().toUpperCase().split("\\s");

        if(command[0].equals(BOT_PREFIX + "HI")){
            RequestBuffer.request(() -> event.getChannel().sendMessage("Hello, minion!"));
        }
        if(command[0].equals(BOT_PREFIX + "ROLECHANNEL")){
            RoleChannels.handle(command, event);
        }
        if(command[0].equals(BOT_PREFIX + "JOIN")){
            RoleChannels.join(event,0);
        }
    }


}
