package com.github.mendess2526.discordbot;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelUpdateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class RoleChannels {

    static final int JOIN = 0;
    static final int LEAVE = 1;

    static final String[] NUMBERS = {":one:",":two:",":three:",":four:",":five:",":six:"};

    private static Map<String,Command> commandMap = new HashMap<>();

    static {
        commandMap.put("NEW",(event, args) -> {
            IChannel ch = newChannel(args.get(1), event.getGuild());
            if(ch!=null){
                LoggerService.log("Channel "+ch.getName()+" created successfully!",LoggerService.SUCC);
                RequestBuffer.request(() -> event.getChannel().sendMessage("Channel "+ch.getName()+" created successfully!"));
            }else{
                LoggerService.log("Couldn't create channel",LoggerService.ERROR);
                RequestBuffer.request(() -> event.getChannel().sendMessage("Couldn't create channel"));
            }
        });

        commandMap.put("DELETE", (event, args) -> deleteChannel(args.get(1), event));

        commandMap.put("SETALL", (event, args) -> setAll(event));
    }


    // Making Channels
    public static void handle(List<String> args, MessageReceivedEvent event){

        if(!event.getAuthor().getPermissionsForGuild(event.getGuild()).contains(Permissions.MANAGE_CHANNELS)){
            RequestBuffer.request(() -> event.getChannel().sendMessage("You don't have permission to use that command :("));
        }else{
            commandMap.get(args.get(0)).runCommand(event,args);
        }
    }

    private static IChannel newChannel(String name, IGuild guild) {
        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        IRole everyone = guild.getEveryoneRole();
        IUser ourUser = guild.getClient().getOurUser();
        IChannel ch;

        // Attempt to create channel
        ch = RequestBuffer.request(() -> {
            try{
                return guild.createChannel(name);
            }catch (MissingPermissionsException e){
                return null;
            }
        }).get();
        if(ch==null){
            return null;
        }

        // Attempt to change permissions
        boolean success;
        RequestBuffer.request(() -> ch.overrideUserPermissions(ourUser,readMessages,noPermits));
        RequestBuffer.request(() -> ch.overrideRolePermissions(everyone,noPermits,readMessages));
        try {
            guild.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) ->
                    !ch.getModifiedPermissions(everyone).contains(Permissions.READ_MESSAGES)
                            && ch.getModifiedPermissions(ourUser).contains(Permissions.READ_MESSAGES),2,TimeUnit.SECONDS);
        }catch (InterruptedException e) {
            LoggerService.log("Interrupted Exception thrown when waiting for "+ch.getName()+"'s permissions to be changed.",LoggerService.ERROR);
            e.printStackTrace();
        }
        success = !ch.getModifiedPermissions(everyone).contains(Permissions.READ_MESSAGES);
        LoggerService.log(success ? "Changed permissions for "+ch.getName() : "Couldn't change permissions for "+ch.getName()+".",
                success ? LoggerService.SUCC                      : LoggerService.ERROR);
        if(!success){ch.delete(); return ch;}

        // Attempt to change topic
        String topic = "PRIVATE CHANNEL: "+name;
        RequestBuffer.request(() -> ch.changeTopic(topic));
        try{
            guild.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) -> topic.equals(ch.getTopic()));
        }catch (InterruptedException e){
            LoggerService.log("Interrupted Exception thrown when waiting for "+ch.getName()+"'s topic to be changed.",LoggerService.ERROR);
            e.printStackTrace();
        }
        success = topic.equals(ch.getTopic());
        LoggerService.log(success ? "Changed topic of channel "+ch.getName() : "Couldn't change topic for channel "+ch.getName()+".",
                success ? LoggerService.SUCC                       : LoggerService.ERROR);
        if(!success){ch.delete();}
        return ch;
    }

    private static void deleteChannel(String name, MessageReceivedEvent event) {
        IChannel ch;
        long id;
        try {
            id = Long.parseLong(name.replaceAll("<", "").replaceAll("#", "").replaceAll(">", ""));
        }catch (NumberFormatException e){
            RequestBuffer.request(() -> event.getChannel().sendMessage("Use `#` to specify what channel you want to delete."));
            return;
        }
        ch = event.getGuild().getChannelByID(id);
        String[] topic;
        try{
            topic = ch.getTopic().split(":");
        }catch (NullPointerException e){
            topic = new String[]{"NOT"};
        }
        if(topic[0].equals("PRIVATE CHANNEL")){
            LoggerService.log("Name of the channel to delete: "+ch.getName(),LoggerService.INFO);
            RequestBuffer.request(ch::delete);
            try {
                event.getClient().getDispatcher().waitFor((ChannelDeleteEvent e) -> event.getGuild().getChannelByID(id)==null,2,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LoggerService.log("Interrupted Exception thrown when waiting for channel to be deleted.",LoggerService.ERROR);
                e.printStackTrace();
            }
            RequestBuffer.request(() -> event.getChannel().sendMessage(event.getGuild().getChannelByID(id)==null ? "Channel deleted successfully" : "Couldn't delete channel"));
        }else {
            LoggerService.log(ch.getName()+" is not a Role Channel",LoggerService.INFO);
            RequestBuffer.request(() -> event.getChannel().sendMessage(ch.getName()+" is not a Private Channel, I can't delete it"));
        }
    }

    private static void setAll(MessageReceivedEvent event) {
        List<IChannel> chList = event.getGuild().getChannels();
        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        chList.forEach(c -> {
            RequestBuffer.request(() -> c.changeTopic("PRIVATE CHANNEL: "+c.getName())).get();
            RequestBuffer.request(() -> c.overrideUserPermissions(event.getGuild().getClient().getOurUser(),readMessages,noPermits)).get();
            RequestBuffer.request(() -> event.getChannel().sendMessage("changed topic of "+c.getName()));
        });
    }

    // Join

    private static List<IChannel> joinableChannels(IGuild guild, IUser user){
        List<IChannel> chList = guild.getChannels();
        Iterator<IChannel> it = chList.iterator();

        // Filter out channels that can't be joined
        while (it.hasNext()){
            IChannel c = it.next();
            String[] topic;

            try{
                topic = c.getTopic().split(":");
            }catch (NullPointerException e){
                topic = new String[]{"NOT"};
            }
            if(c.getModifiedPermissions(user).contains(Permissions.READ_MESSAGES)
                    || !topic[0].equals("PRIVATE CHANNEL")){
                it.remove();
            }
        }
        return chList;
    }

    private static void showJoinableChannels(IMessage message, IUser user, int page) {
        List<IChannel> chList = joinableChannels(message.getGuild(),user);
        EmbedObject e = channelListEmbed(chList,page,JOIN);

        IMessage msg;
        msg = RequestBuffer.request(() -> {
            return message.edit(user.mention(),e);
        }).get();
        RequestBuffer.request(message::removeAllReactions).get();
        channelListReact(msg,chList.size(),page);
    }

    public static void showJoinableChannels(IGuild guild, IChannel channel, IUser user) {
        List<IChannel> chList = joinableChannels(guild,user);
        EmbedObject e = channelListEmbed(chList,0, JOIN);
        IMessage msg;
        msg = RequestBuffer.request(() -> {
            return channel.sendMessage(user.mention(),e);
        }).get();
        channelListReact(msg,chList.size(),0);
    }

    private static void addUser(IChannel ch, IUser user) {
        RequestBuffer.request(() -> ch.overrideUserPermissions(user,EnumSet.of(Permissions.READ_MESSAGES),EnumSet.noneOf(Permissions.class))).get();
    }

    public static void join(ReactionEvent event) {
        // If it was the mentioned user that reacted to a reaction added by the bot
        if(event.getMessage().getMentions().get(0).equals(event.getUser())
                && event.getReaction().getUserReacted(event.getClient().getOurUser())){

            String opt = event.getReaction().getUnicodeEmoji().getAliases().get(0);
            switch (opt) {
                case "arrow_backward": {
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    showJoinableChannels(event.getMessage(), event.getUser(), page - 1);
                    break;
                }
                case "arrow_forward": {
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    showJoinableChannels(event.getMessage(), event.getUser(), page + 1);
                    break;
                }
                case "one":
                case "two":
                case "three":
                case "four":
                case "five":
                case "six": {
                    LoggerService.log(event.getMessage().getEmbeds().get(0).getFooter().getText(), LoggerService.INFO);
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    int chNumber = literal2Int(opt);

                    LoggerService.log("Channel list before adding user: "+ Arrays.toString(joinableChannels(event.getGuild(),event.getUser()).stream().map(IChannel::getName).toArray()),LoggerService.INFO);
                    RequestBuffer.request(() -> event.getMessage().removeAllReactions());
                    IChannel ch  = joinableChannels(event.getGuild(), event.getUser()).get((chNumber - 1) + page * 6);
                    addUser(ch, event.getUser());
                    RequestBuffer.request(() -> event.getMessage().edit(event.getUser().mention(),
                            new EmbedBuilder().withTitle(":white_check_mark: Channel Joined!").build()));
                    try {
                        event.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) ->
                                !ch.getModifiedPermissions(event.getUser()).contains(Permissions.READ_MESSAGES)
                        ,2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        LoggerService.log("Interrupted Exception thrown when waiting for "+event.getUser().getName()+"to be added to "+ch.getName()+".",LoggerService.ERROR);
                        e.printStackTrace();
                    }
                    LoggerService.log("Channel list after adding user: "+ Arrays.toString(joinableChannels(event.getGuild(),event.getUser()).stream().map(IChannel::getName).toArray()),LoggerService.INFO);
                    showJoinableChannels(event.getMessage(), event.getUser(), page);
                    /*}else{
                        RequestBuffer.request(() -> event.getMessage().edit(event.getUser().mention(),
                                                                            new EmbedBuilder().withTitle(":x: Couldn't join channel").build()));
                    }*/
                    break;
                }
            }
        }
    }

    // Leave

    private static List<IChannel> leavableChannels(IGuild guild, IUser user){
        List<IChannel> chList = guild.getChannels();
        Iterator<IChannel> it = chList.iterator();

        // Filter out channels that can't be left
        while (it.hasNext()){
            IChannel c = it.next();
            String[] topic;

            try{
                topic = c.getTopic().split(":");
            }catch (NullPointerException e){
                topic = new String[]{"NOT"};
            }
            if(!c.getModifiedPermissions(user).contains(Permissions.READ_MESSAGES)
                    || !topic[0].equals("PRIVATE CHANNEL")){
                it.remove();
            }
        }
        return chList;
    }

    public static void showLeavableChannels(IGuild guild, IChannel channel, IUser user) {
        List<IChannel> chList = leavableChannels(guild,user);
        EmbedObject e = channelListEmbed(chList,0,LEAVE);
        IMessage msg;
        msg = RequestBuffer.request(() -> {
            return channel.sendMessage(user.mention(),e);
        }).get();
        channelListReact(msg,chList.size(),0);
    }

    private static void showLeavableChannels(IMessage message, IUser user, int page) {
        List<IChannel> chList = leavableChannels(message.getGuild(),user);
        EmbedObject e = channelListEmbed(chList,page,LEAVE);

        IMessage msg;
        msg = RequestBuffer.request(() -> {
            return message.edit(user.mention(),e);
        }).get();
        RequestBuffer.request(message::removeAllReactions).get();
        channelListReact(msg,chList.size(),page);
    }

    private static void removeUser(IChannel ch, IUser user) {
        RequestBuffer.request(() ->ch.overrideUserPermissions(user,EnumSet.noneOf(Permissions.class),EnumSet.of(Permissions.READ_MESSAGES))).get();
    }

    public static void leave(ReactionEvent event) {
        // If it was the mentioned user that reacted to a reaction added by the bot
        if(event.getMessage().getMentions().get(0).equals(event.getUser())){
            String opt = event.getReaction().getUnicodeEmoji().getAliases().get(0);
            LoggerService.log("Emoji alias:  "+ opt,LoggerService.INFO);
            switch (opt) {
                case "arrow_backward": {
                    LoggerService.log(event.getMessage().getEmbeds().get(0).getFooter().getText(), LoggerService.INFO);
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    showLeavableChannels(event.getMessage(), event.getUser(), page - 1);
                    break;
                }
                case "arrow_forward": {
                    LoggerService.log(event.getMessage().getEmbeds().get(0).getFooter().getText(), LoggerService.INFO);
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    showLeavableChannels(event.getMessage(), event.getUser(), page + 1);
                    break;
                }
                case "one":
                case "two":
                case "three":
                case "four":
                case "five":
                case "six": {
                    LoggerService.log(opt, LoggerService.INFO);
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    int chNumber = literal2Int(opt);

                    LoggerService.log("Channel list before removing user: "+ Arrays.toString(joinableChannels(event.getGuild(),event.getUser()).stream().map(IChannel::getName).toArray()),LoggerService.INFO);
                    RequestBuffer.request(() -> event.getMessage().removeAllReactions());
                    IChannel ch  = leavableChannels(event.getGuild(), event.getUser()).get((chNumber - 1) + page * 6);
                    removeUser(ch, event.getUser());
                    RequestBuffer.request(() -> event.getMessage().edit(event.getUser().mention(),
                            new EmbedBuilder().withTitle(":white_check_mark: Channel Left!").build()));
                    try {
                        event.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) ->
                                !ch.getModifiedPermissions(event.getUser()).contains(Permissions.READ_MESSAGES)
                        ,2,TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        LoggerService.log("Interrupted Exception thrown when waiting for "+event.getUser().getName()+"to be removed from "+ch.getName()+".",LoggerService.ERROR);
                        e.printStackTrace();
                    }
                    LoggerService.log("Channel list after removing user: "+ Arrays.toString(joinableChannels(event.getGuild(),event.getUser()).stream().map(IChannel::getName).toArray()),LoggerService.INFO);
                    showLeavableChannels(event.getMessage(), event.getUser(), page);
                    /*RequestBuffer.request(() -> event.getMessage().edit(event.getUser().mention(),
                                                                            new EmbedBuilder().withTitle(":white_check_mark: Channel Left!").build()));*/
                    /*}else{
                        RequestBuffer.request(() -> event.getMessage().edit(event.getUser().mention(),
                                                                            new EmbedBuilder().withTitle(":x: Couldn't leave channel").build()));
                    }*/


                    break;
                }
            }
        }
    }

    // Misc

    private static EmbedObject channelListEmbed(List<IChannel> chList, int currentPage, int mode) {
        String[] Title1 = {"No more channels to join. You're EVERYWHERE!", "No more channels to leave."};
        String[] Title2 = {"Select the channel you want to join!","Select the channel you want to leave!"};
        EmbedBuilder e = new EmbedBuilder();
        int count = 1;
        if(chList.size()==0){
            e.withTitle(Title1[mode]);
            return e.build();
        }else{
            // Make a sub list of channels, i.e. a page.
            LoggerService.log("List size: "+chList.size(),LoggerService.INFO);
            LoggerService.log("Channel list: "+ Arrays.toString(chList.stream().map(IChannel::getName).toArray()),LoggerService.INFO);
            if(currentPage*6 > chList.size()){currentPage--;}
            int from = currentPage*6;
            int to = from+6 > chList.size() ? chList.size() : from+6;
            LoggerService.log("Making sub list. From: "+from+" To: "+to,LoggerService.INFO);
            List<IChannel> page = chList.subList(from,to);

            e.withTitle(Title2[mode]);
            // Print the list to the Embed
            for(IChannel c : page){
                e.appendDesc("**"+count+":** "+ c.getName()+"\n");
                count++;
            }
            // Print the page count
            int numPages = chList.size()/6;
            e.withFooterText("Page "+(currentPage+1)+"/"+(numPages+1));

            return e.build();
        }
    }

    private static void channelListReact(IMessage msg, int size, int page){
        if(page!=0){
            RequestBuffer.request(() -> msg.addReaction(":arrow_backward:")).get();
        }
        boolean isLastPage = page>=size/6;
        int count = isLastPage ? size%6 : 6;

        for(int i=0;i<count;i++){
            int finalI = i;
            RequestBuffer.request(() -> msg.addReaction(NUMBERS[finalI])).get();
        }
        if(!isLastPage){
            RequestBuffer.request(() -> msg.addReaction(":arrow_forward:")).get();
        }
        RequestBuffer.request(() -> msg.addReaction(":x:")).get();
    }

    private static int literal2Int(String literal){
        int number = -1;
        switch (literal){
            case "zero":
                number = 0; break;
            case "one":
                number = 1; break;
            case "two":
                number = 2; break;
            case "three":
                number = 3; break;
            case "four":
                number = 4; break;
            case "five":
                number = 5; break;
            case "six":
                number = 6; break;
            case "seven":
                number = 7; break;
            case "eight":
                number = 8; break;
            case "nine":
                number = 9; break;
        }
        return number;
    }
}
