package com.github.mendess2526.discordbot;

import com.vdurmont.emoji.EmojiParser;
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
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class RoleChannels {

    private static final int JOIN = 0;
    private static final int LEAVE = 1;
    private static final String[] NUMBERS = {":one:",":two:",":three:",":four:",":five:",":six:"};
    private static final Set<Permissions> permissions = EnumSet.of(Permissions.MANAGE_CHANNELS);
    public static Map<String,Command> commandMap = new HashMap<>();
    private static String PRIVATE_MARKER = ">";

    static {
        commandMap.put("NEW", RoleChannels::newChannel);

        commandMap.put("DELETE", RoleChannels::deleteChannel);

        commandMap.put("SETALL", RoleChannels::setAll);

        commandMap.put("SET", RoleChannels::set);

        commandMap.put("UNSET", RoleChannels::unSet);

        commandMap.put("CONVERT", RoleChannels::convert);
    }


    // Managing Channels
    public static void handle(MessageReceivedEvent event, List<String> args){
        if(BotUtils.hasPermission(event,permissions)) {
            if (args.size() == 0 || !commandMap.containsKey(args.get(0))) {
                if (args.size() != 0) LoggerService.log(event.getGuild(), "Invalid Argument: " + args.get(0), LoggerService.INFO);
                HashMap<String, Set<String>> cmds = new HashMap<>();
                cmds.put("Rolechannels", commandMap.keySet());
                BotUtils.help(event.getAuthor(), event.getChannel(), cmds);
            } else {
                commandMap.get(args.get(0)).runCommand(event, args.subList(1, args.size()));
            }
        }
    }
    private static void newChannel(MessageReceivedEvent event, List<String> args) {
        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        IGuild guild = event.getGuild();
        IChannel ch;
        IRole everyone = guild.getEveryoneRole();
        IUser ourUser = guild.getClient().getOurUser();
        String name = String.join("-",args);

        // Attempt to create channel
        ch = RequestBuffer.request(() -> {
            try{
                return guild.createChannel(name);
            }catch (MissingPermissionsException e){
                return null;
            }
        }).get();
        if(ch==null){
            LoggerService.log(event.getGuild(),"Couldn't create channel",LoggerService.ERROR);
            BotUtils.sendMessage(event.getChannel(),"Couldn't create channel, maybe I'm missing permissions?",120,false);
            return;
        }

        // Attempt to change permissions
        boolean success;
        RequestBuffer.request(() -> ch.overrideUserPermissions(ourUser,readMessages,noPermits));
        RequestBuffer.request(() -> ch.overrideRolePermissions(everyone,noPermits,readMessages));
        try {
            guild.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) ->
                            !ch.getModifiedPermissions(everyone).contains(Permissions.READ_MESSAGES)
                            && ch.getModifiedPermissions(ourUser).contains(Permissions.READ_MESSAGES),10,TimeUnit.SECONDS);
        }catch (InterruptedException e) {
            LoggerService.log(event.getGuild(),"Interrupted Exception thrown when waiting for "+ch.getName()+"'s permissions to be changed.",LoggerService.ERROR);
            e.printStackTrace();
        }
        success = !ch.getModifiedPermissions(everyone).contains(Permissions.READ_MESSAGES);
        if(!success){
            LoggerService.log(event.getGuild(),"Couldn't change permissions for "+ch.getName()+".",LoggerService.ERROR);
            BotUtils.sendMessage(event.getChannel(),
                    "Couldn't change permissions for channel"+ch.getName()+". Deleting channel",120,false);
            ch.delete();
            return;
        }
        LoggerService.log(event.getGuild(),"Changed permissions for "+ch.getName()+".",LoggerService.INFO);

        // Attempt to change topic
        String topic = PRIVATE_MARKER+ch.getName();
        RequestBuffer.request(() -> ch.changeTopic(topic));
        try{
            guild.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) -> topic.equals(ch.getTopic()),10,TimeUnit.SECONDS);
        }catch (InterruptedException e){
            LoggerService.log(event.getGuild(),"Interrupted Exception thrown when waiting for "+ch.getName()+"'s topic to be changed.",LoggerService.ERROR);
            e.printStackTrace();
        }
        success = topic.equals(ch.getTopic());
        if(!success){
            LoggerService.log(event.getGuild(),"Couldn't change topic for channel "+ch.getName()+". Deleting channel",LoggerService.ERROR);
            BotUtils.sendMessage(event.getChannel(),
                    "Couldn't change topic for channel "+ch.getName()+". Deleting channel",120,false);
            ch.delete();
        }
        LoggerService.log(event.getGuild(),"Changed topic of channel "+ch.getName(),LoggerService.INFO);
        LoggerService.log(event.getGuild(),"Channel "+ch.getName()+" created successfully!",LoggerService.SUCC);
        BotUtils.sendMessage(event.getChannel(),
                "Channel "+ch.mention()+" created successfully!",-1,false);
    }

    private static void deleteChannel(MessageReceivedEvent event, List<String> args) {
        IChannel ch;
        long id;
        try {
            id = Long.parseLong(args.get(0).replaceAll("<", "").replaceAll("#", "").replaceAll(">", ""));
        }catch (NumberFormatException e){
            BotUtils.sendMessage(event.getChannel(),"Use `#` to specify what channel you want to delete.",120,false);
            return;
        }
        ch = event.getGuild().getChannelByID(id);
        String topic = EmojiParser.parseToAliases(ch.getTopic());
        if(topic.startsWith(PRIVATE_MARKER)){
            LoggerService.log(event.getGuild(),"Name of the channel to delete: "+ch.getName(),LoggerService.INFO);
            RequestBuffer.request(ch::delete);
            try {
                event.getClient().getDispatcher().waitFor((ChannelDeleteEvent e) -> event.getGuild().getChannelByID(id)==null,10,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LoggerService.log(event.getGuild(),"Interrupted Exception thrown when waiting for channel to be deleted.",LoggerService.ERROR);
                e.printStackTrace();
            }
            BotUtils.sendMessage(event.getChannel(),
                    event.getGuild().getChannelByID(id)==null ? "Channel deleted successfully" : "Couldn't delete channel",
                    120,false);
        }else {
            LoggerService.log(event.getGuild(),ch.getName()+" is not a Role Channel",LoggerService.INFO);
            BotUtils.sendMessage(event.getChannel(),ch.getName()+" is not a Private Channel, I can't delete it",120,false);
        }
    }

    private static void setAll(MessageReceivedEvent event, List<String> args) {
        List<IChannel> chList = event.getGuild().getChannels();
        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        chList.forEach(c -> {
            RequestBuffer.request(() -> c.changeTopic(PRIVATE_MARKER+c.getTopic())).get();
            RequestBuffer.request(() -> c.overrideUserPermissions(event.getGuild().getClient().getOurUser(),readMessages,noPermits)).get();
            BotUtils.sendMessage(event.getChannel(),"Changed topic of "+c.getName(),120,false);
        });
    }

    private static void set(MessageReceivedEvent event, List<String> name){
        long id;
        try {
            id = Long.parseLong(name.get(0).replaceAll("<", "").replaceAll("#", "").replaceAll(">", ""));
        }catch (NumberFormatException e){
            BotUtils.sendMessage(event.getChannel(),"Use `#` to specify what channel you want to delete.",120,false);
            return;
        }
        IChannel ch = event.getGuild().getChannelByID(id);
        LoggerService.log(event.getGuild(),"Channel to set: "+ch.getName(),LoggerService.INFO);

        if(ch.getTopic()!=null && ch.getTopic().startsWith(PRIVATE_MARKER)){
            BotUtils.sendMessage(event.getChannel(),"Already a private channel",120,false);
            return;
        }
        String newTopic = PRIVATE_MARKER + (ch.getTopic()!=null ? ch.getTopic() : "");
        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        IRole everyone = event.getGuild().getEveryoneRole();
        IUser ourUser = event.getGuild().getClient().getOurUser();
        LoggerService.log(event.getGuild(),String.format("Changing topic from: "+ch.getTopic()+" to: "+newTopic+"\n%27sAnd overriding permissions."," "),LoggerService.INFO);
        RequestBuffer.request(() -> ch.changeTopic(newTopic)).get();
        RequestBuffer.request(() -> ch.overrideUserPermissions(ourUser,readMessages,noPermits)).get();
        RequestBuffer.request(() -> ch.overrideRolePermissions(everyone,noPermits,readMessages));
        BotUtils.sendMessage(event.getChannel(),ch.getName()+" is now private!",-1,false);
    }

    private static void unSet(MessageReceivedEvent event, List<String> name){
        long id;
        try {
            id = Long.parseLong(name.get(0).replaceAll("<", "").replaceAll("#", "").replaceAll(">", ""));
        }catch (NumberFormatException e){
            BotUtils.sendMessage(event.getChannel(),"Use `#` to specify what channel you want to delete.",120,false);
            return;
        }
        IChannel ch = event.getGuild().getChannelByID(id);
        LoggerService.log(event.getGuild(),"Channel to un-set: "+ch.getName(),LoggerService.INFO);

        if(ch.getTopic()==null || !ch.getTopic().startsWith(PRIVATE_MARKER)){
            BotUtils.sendMessage(event.getChannel(),"Not a private channel",120,false);
            return;
        }
        String newTopic = ch.getTopic().replaceFirst(PRIVATE_MARKER,"");
        EnumSet<Permissions> readMessages = EnumSet.of(Permissions.READ_MESSAGES);
        EnumSet<Permissions> noPermits = EnumSet.noneOf(Permissions.class);
        IRole everyone = event.getGuild().getEveryoneRole();

        LoggerService.log(event.getGuild(),String.format("Changing topic from: "+ch.getTopic()+" to: "+newTopic+"\n%27sAnd overriding permissions."," "),LoggerService.INFO);
        RequestBuffer.request(() -> ch.changeTopic(newTopic)).get();
        RequestBuffer.request(() -> ch.overrideRolePermissions(everyone,readMessages,noPermits));
        event.getGuild().getUsers().stream()
                                   .filter(user -> ch.getModifiedPermissions(user).contains(Permissions.READ_MESSAGES))
                                   .forEach(user -> RequestBuffer.request(() -> ch.removePermissionsOverride(user)));
        BotUtils.sendMessage(event.getChannel(),ch.getName()+" is now public!",-1,false);
    }

    private static void convert(MessageReceivedEvent event, List<String> args){
        args.remove(0);
        String oldMarker = String.join(" ", args);
        LoggerService.log(event.getGuild(),"Converting "+oldMarker+" to "+PRIVATE_MARKER,LoggerService.INFO);
        List<IChannel> chList = event.getGuild().getChannels();
        chList.stream()
                .filter(c -> c.getTopic()!=null && c.getTopic().startsWith(oldMarker))
                .forEach(c -> {
                    LoggerService.log(event.getGuild(),"Converting "+c.getName(),LoggerService.INFO);
                    String nTopic = c.getTopic().replace(oldMarker,PRIVATE_MARKER);
                    RequestBuffer.request(() -> c.changeTopic(nTopic));
                    try {
                        c.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) -> c.getTopic().startsWith(PRIVATE_MARKER),5,TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        LoggerService.log(event.getGuild(),"Interrupted when converting channel "+c.getName(),LoggerService.ERROR);
                        e.printStackTrace();
                    }
                    if(!c.getTopic().startsWith(PRIVATE_MARKER)){
                        BotUtils.sendMessage(event.getChannel(),"Couldn't convert "+c.getName()+". Try to rerun the command",120,false);
                    }
                });
    }

    // Join
    private static List<IChannel> joinableChannels(IGuild guild, IUser user){
        List<IChannel> chList = guild.getChannels();
        // Filter out channels that can't be joined
        return chList.stream().filter(c -> c.getTopic()!=null
                                        && c.getTopic().startsWith(PRIVATE_MARKER)
                                        && !c.getModifiedPermissions(user).contains(Permissions.READ_MESSAGES))
                              .collect(Collectors.toList());
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

    public static void showJoinableChannels(MessageReceivedEvent event, List<String> args) {
        IGuild guild = event.getGuild();
        IChannel channel = event.getChannel();
        IUser user = event.getAuthor();
        List<IChannel> chList = joinableChannels(guild,user);
        EmbedObject e = channelListEmbed(chList,0, JOIN);
        IMessage msg = BotUtils.sendMessage(channel,user.mention(),e,-1,false);
        channelListReact(msg,chList.size(),0);
    }

    private static void addUser(IChannel ch, IUser user) {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            LoggerService.log(ch.getGuild(),"Interrupted on RoleChannels.addUser",LoggerService.ERROR);
            e.printStackTrace();
        }
        RequestBuffer.request(() -> ch.overrideUserPermissions(user,EnumSet.of(Permissions.READ_MESSAGES),EnumSet.noneOf(Permissions.class)));
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
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    int chNumber = literal2Int(opt);

                    LoggerService.log(event.getGuild(),"Channel list before adding user: "+ Arrays.toString(joinableChannels(event.getGuild(),event.getUser()).stream().map(IChannel::getName).toArray()),LoggerService.INFO);
                    RequestBuffer.request(() -> event.getMessage().removeAllReactions());
                    IChannel ch  = joinableChannels(event.getGuild(), event.getUser()).get((chNumber - 1) + page * 6);
                    addUser(ch, event.getUser());
                    try {
                        event.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) ->
                                e.getNewChannel().getModifiedPermissions(event.getUser()).contains(Permissions.READ_MESSAGES)
                        ,5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        LoggerService.log(event.getGuild(),"Interrupted Exception thrown when waiting for "+event.getUser().getName()+"to be added to "+ch.getName()+".",LoggerService.ERROR);
                        e.printStackTrace();
                    }
                    if(ch.getModifiedPermissions(event.getUser()).contains(Permissions.READ_MESSAGES)){
                        RequestBuffer.request(() -> event.getMessage().edit(event.getUser().mention(),
                                new EmbedBuilder().withTitle(":white_check_mark: Channel joined!").build())).get();
                    }else{
                        RequestBuffer.request(() -> event.getMessage().edit(event.getUser().mention(),
                                new EmbedBuilder().withTitle(":x: Couldn't join channel").build())).get();
                    }
                    LoggerService.log(event.getGuild(),"Channel list after adding user: "+ Arrays.toString(joinableChannels(event.getGuild(),event.getUser()).stream().map(IChannel::getName).toArray()),LoggerService.INFO);
                    showJoinableChannels(event.getMessage(), event.getUser(), page);
                    break;
                }
            }
        }
    }


    // Leave
    private static List<IChannel> leavableChannels(IGuild guild, IUser user){
        List<IChannel> chList = guild.getChannels();
        // Filter out channels that can't be left
        return chList.stream().filter(c -> c.getTopic()!=null
                                        && c.getTopic().startsWith(PRIVATE_MARKER)
                                        && c.getModifiedPermissions(user).contains(Permissions.READ_MESSAGES))
                              .collect(Collectors.toList());
    }

    public static void showLeavableChannels(MessageReceivedEvent event, List<String> args) {
        IGuild guild = event.getGuild();
        IChannel channel = event.getChannel();
        IUser user = event.getAuthor();
        List<IChannel> chList = leavableChannels(guild,user);
        EmbedObject e = channelListEmbed(chList,0,LEAVE);
        IMessage msg = BotUtils.sendMessage(channel,user.mention(),e,-1,false);
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
        RequestBuffer.request(() ->ch.removePermissionsOverride(user));
    }

    public static void leave(ReactionEvent event) {
        // If it was the mentioned user that reacted to a reaction added by the bot
        if(event.getMessage().getMentions().get(0).equals(event.getUser())){
            String opt = event.getReaction().getUnicodeEmoji().getAliases().get(0);
            LoggerService.log(event.getGuild(),"Emoji alias:  "+ opt,LoggerService.INFO);
            switch (opt) {
                case "arrow_backward": {
                    LoggerService.log(event.getGuild(),event.getMessage().getEmbeds().get(0).getFooter().getText(), LoggerService.INFO);
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    showLeavableChannels(event.getMessage(), event.getUser(), page - 1);
                    break;
                }
                case "arrow_forward": {
                    LoggerService.log(event.getGuild(),event.getMessage().getEmbeds().get(0).getFooter().getText(), LoggerService.INFO);
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
                    LoggerService.log(event.getGuild(),opt, LoggerService.INFO);
                    String pageStr = event.getMessage().getEmbeds().get(0).getFooter().getText().split("\\s")[1].split("/")[0];
                    int page = Integer.parseInt(pageStr) - 1;
                    int chNumber = literal2Int(opt);

                    LoggerService.log(event.getGuild(),"Channel list before removing user: "+ Arrays.toString(joinableChannels(event.getGuild(),event.getUser()).stream().map(IChannel::getName).toArray()),LoggerService.INFO);
                    RequestBuffer.request(() -> event.getMessage().removeAllReactions());
                    IChannel ch  = leavableChannels(event.getGuild(), event.getUser()).get((chNumber - 1) + page * 6);
                    removeUser(ch, event.getUser());
                    try {
                        event.getClient().getDispatcher().waitFor((ChannelUpdateEvent e) ->
                                !e.getNewChannel().getModifiedPermissions(event.getUser()).contains(Permissions.READ_MESSAGES)
                        ,5,TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        LoggerService.log(event.getGuild(),"Interrupted Exception thrown when waiting for "+event.getUser().getName()+"to be removed from "+ch.getName()+".",LoggerService.ERROR);
                        e.printStackTrace();
                    }
                    if(!ch.getModifiedPermissions(event.getUser()).contains(Permissions.READ_MESSAGES)){
                        RequestBuffer.request(() -> event.getMessage().edit(event.getUser().mention(),
                                new EmbedBuilder().withTitle(":white_check_mark: Channel Left!").build())).get();
                    }else{
                        RequestBuffer.request(() -> event.getMessage().edit(event.getUser().mention(),
                                new EmbedBuilder().withTitle(":x: Couldn't leave channel").build())).get();
                    }
                    LoggerService.log(event.getGuild(),"Channel list after removing user: "+ Arrays.toString(joinableChannels(event.getGuild(),event.getUser()).stream().map(IChannel::getName).toArray()),LoggerService.INFO);
                    showLeavableChannels(event.getMessage(), event.getUser(), page);
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
            LoggerService.log(chList.get(0).getGuild(),"List size: "+chList.size(),LoggerService.INFO);
            LoggerService.log(chList.get(0).getGuild(),"Channel list: "+ Arrays.toString(chList.stream().map(IChannel::getName).toArray()),LoggerService.INFO);
            if(currentPage*6 >= chList.size()){currentPage--;}
            int from = currentPage*6;
            int to = from+6 > chList.size() ? chList.size() : from+6;
            LoggerService.log(chList.get(0).getGuild(),"Making sub list. From: "+from+" To: "+to,LoggerService.INFO);
            List<IChannel> page = chList.subList(from,to);

            e.withTitle(Title2[mode]);
            // Print the list to the Embed
            for(IChannel c : page){
                e.appendDesc("**"+count+":** "+ c.getName()+"\n");
                count++;
            }
            // Print the page count
            int numPages = (chList.size()-1)/6;
            e.withFooterText("Page "+(currentPage+1)+"/"+(numPages+1));

            return e.build();
        }
    }

    private static void channelListReact(IMessage msg, int size, int page){
        if(page!=0){
            RequestBuffer.request(() -> msg.addReaction(":arrow_backward:")).get();
        }
        LoggerService.log(msg.getGuild(),"page: "+page+" size: "+size+" (size-1)/6: "+(size-1)/6,LoggerService.INFO);
        boolean isLastPage = page>=(size-1)/6;
        int count = size==0 ? 0 : (isLastPage ? ((size-1)%6)+1 : 6);
        for(int i=0;i<count;i++){
            int finalI = i;
            RequestBuffer.request(() -> msg.addReaction(NUMBERS[finalI])).get();
        }
        if(!isLastPage){
            RequestBuffer.request(() -> msg.addReaction(":arrow_forward:")).get();
        }
        BotUtils.closeButton(msg);
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
