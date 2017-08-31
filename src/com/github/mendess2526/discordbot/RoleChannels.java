package com.github.mendess2526.discordbot;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

import java.io.BufferedReader;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class RoleChannels {
    static final String NEW = "NEW";
    static final String DELETE = "DELETE";

    static final String[] NUMBERS = {":one:",":two:",":three:",":four:",":five:",":six:"};

    public static void handle(String[] command, MessageReceivedEvent event){
        if(!event.getAuthor().getPermissionsForGuild(event.getGuild()).contains(Permissions.MANAGE_CHANNELS)){
            RequestBuffer.request(() -> event.getChannel().sendMessage("You don't have permission to use that command :("));
        }else{
            switch (command[1]){
                case NEW:
                    newChannel(command[2], event.getGuild());
                    break;
                case DELETE:
                    deleteChannel(command[2], event);
                    break;
            }
        }
    }

    private static void deleteChannel(String name, MessageReceivedEvent event) {
        LoggerService.log("Getting channels to delete",LoggerService.INFO);
        IChannel ch;
        long id;
        try {
            id = Long.parseLong(name.replaceAll("<", "").replaceAll("#", "").replaceAll(">", ""));
        }catch (NumberFormatException e){
            RequestBuffer.request(() -> event.getChannel().sendMessage("Use `#` to specify what channel you want to delete."));
            return;
        }
        ch = event.getGuild().getChannelByID(id);
        LoggerService.log("Name of the channel to delete "+ch.getName(),LoggerService.INFO);
        ch.delete();
    }

    private static void newChannel(String name, IGuild guild) {

        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        IRole everyone = guild.getEveryoneRole();

        RequestBuffer.request(() -> {
            IChannel newChannel = guild.createChannel(name);
            newChannel.overrideRolePermissions(everyone,noPermits,readMessages);
            newChannel.changeTopic("PRIVATE CHANNEL: "+name);
        });
    }

    public static void join(MessageReceivedEvent event, int currentPage) {
        List<IChannel> chList = event.getGuild().getChannels();

        Iterator<IChannel> it = chList.iterator();

        IUser author = event.getAuthor();

        // Filter out channels that can't be joined
        while (it.hasNext()){
            IChannel c = it.next();

            String topic[] = c.getTopic().split(":");

            if(c.getModifiedPermissions(author).contains(Permissions.READ_MESSAGES)
               || !topic[0].equals("PRIVATE CHANNEL")) {

                it.remove();
            }
        }
        EmbedBuilder e = new EmbedBuilder();
        int count = 1;
        if(chList.size()==0){
            e.withTitle("No more channels to join. You're EVERYWHERE!");
            RequestBuffer.request(() -> event.getChannel().sendMessage(e.build()));
        }else{
            int numPages = (chList.size()/6) + 1;
            int from = currentPage*numPages;
            int to = from+6 > chList.size() ? chList.size() : from+6;
            List<IChannel> page = chList.subList(from,to);

            e.withTitle("Select the channel you want to join!");
            for(IChannel c : page){
                e.appendDesc("**"+count+":** "+ c.getName()+"\n");
                count++;
            }

            int finalCount = count;
            LoggerService.log("Starting RequestBuffer",LoggerService.INFO);
            RequestBuffer.request(() ->{
                IMessage msg = event.getChannel().sendMessage(e.build());
                LoggerService.log("Message sent: "+msg.toString(),LoggerService.INFO);

                msg.addReaction(":arrow_backward:");

                RequestBuffer.request(() ->{
                    for(int i=0;i<1;i++){
                        msg.addReaction(NUMBERS[i]);
                        LoggerService.log("Reaction added",LoggerService.INFO);
                    }
                });

                msg.addReaction(":arrow_forward:");
            });
        }
    }
}
