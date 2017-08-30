package com.github.mendess2526.discordbot;

import sx.blah.discord.api.events.Event;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.util.RequestBuffer;

public class Events implements IListener {

    public void handle(Event event) {
        if(event instanceof MessageReceivedEvent){
            handleMessageReceived((MessageReceivedEvent) event);
        }
    }
    private void handleMessageReceived(MessageReceivedEvent event) {
        final String message = event.getMessage().getContent();

        if(message.toUpperCase().equals("!HI")){
            RequestBuffer.request(() -> event.getChannel().sendMessage(message));
        }
    }


}
