package com.github.mendess2526.memnarch;

import com.github.mendess2526.memnarch.misc.CMiscCommands;
import com.github.mendess2526.memnarch.misc.CustomCommands;
import com.github.mendess2526.memnarch.misc.MiscCommands;
import com.github.mendess2526.memnarch.misc.MiscTasks;
import com.github.mendess2526.memnarch.rolechannels.CRoleChannels;
import com.github.mendess2526.memnarch.rolechannels.RoleChannels;
import com.github.mendess2526.memnarch.serversettings.CServerSettings;
import com.github.mendess2526.memnarch.serversettings.ServerSettings;
import com.github.mendess2526.memnarch.sounds.CSounds;
import com.github.mendess2526.memnarch.sounds.SfxModule;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.RequestBuffer;

import java.util.*;

import static com.github.mendess2526.memnarch.LoggerService.*;

public class Events {

    public final static String BOT_PREFIX = "|";
    private static final String CHECK_MARK = "\u2705";
    private static final String RED_X = "\u274C";

    public static final Map<String, Command> commandMap = new HashMap<>();
    private static final SfxModule sfxModule = new SfxModule();
    static {
        commandMap.put("HELP",        new CMiscCommands() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                MiscCommands.help(event);
            }
        });
        commandMap.put("PING",        new CMiscCommands() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                MiscCommands.ping(event);
            }
        });
        commandMap.put("HI",          new CMiscCommands() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                MiscCommands.hi(event);
            }
        });
        commandMap.put("WHOAREYOU",   new CMiscCommands() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                MiscCommands.whoAreYou(event);
            }
        });
        commandMap.put("SHUTDOWN",    new CMiscCommands() {
            @Override
            public Set<Permissions> getPermissions(){
                Set<Permissions> s = super.getPermissions();
                s.add(Permissions.ADMINISTRATOR);
                return s;
            }

            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                MiscCommands.shutdown(event);
            }
        });
        commandMap.put("RRANK",       new CMiscCommands() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                MiscTasks.rRank(event);
            }
        });
        commandMap.put("CC",          new CMiscCommands() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                CustomCommands.handle(event,args);
            }
        });

        commandMap.put("ROLECHANNEL", new CRoleChannels() {
            @Override
            public Set<Permissions> getPermissions(){
                return EnumSet.of(Permissions.MANAGE_CHANNELS);
            }
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                RoleChannels.handle(event,args);
            }
        });
        commandMap.put("JOIN",        new CRoleChannels() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                RoleChannels.startJoinUI(event);
            }
        });
        commandMap.put("LEAVE",       new CRoleChannels() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                RoleChannels.startLeaveUI(event);
            }
        });

        commandMap.put("SFX",         new CSounds() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
//                if(args.get(0).startsWith("<")) jukebox.pause(event);
                sfxModule.sfx(event,args);
            }
        });
        commandMap.put("SFXLIST",     new CSounds() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                sfxModule.list(event);
            }
        });


        commandMap.put("SERVERSET",   new CServerSettings() {
            @Override
            public void runCommand(MessageReceivedEvent event, List<String> args){
                ServerSettings.serverSettings(event,args);
            }
        });
    }

    @EventSubscriber
    public void guildJoin(GuildCreateEvent event){
        ServerSettings.initialiseServerSettings(event.getGuild());
        CustomCommands.initializeGuild(event.getGuild());
    }
    @EventSubscriber
    public void reactionEvent(ReactionAddEvent event) {
        // If it wasn't the bot adding the reaction and it was a reaction added my the bot
        if(event.getReaction().getUserReacted(event.getClient().getOurUser()) &&
                !(event.getUser().equals(event.getClient().getOurUser()))){

            List<IUser> mentions = event.getMessage().getMentions();
            if(mentions.isEmpty() || mentions.contains(event.getUser())){
                if(event.getReaction().getEmoji().getName().equals(BotUtils.X)){
                    log(event.getGuild(),event.getUser().getName()+" clicked an :heavy_multiplication_x:", INFO);
                    event.getMessage().delete();
                }else{
                    int size = 0;
                    switch (event.getMessage().getEmbeds().get(0).getTitle()){
                        case RoleChannels.TITLE_INIT_QUERY_J:
                            RoleChannels.processInitialQuery(event,event.getUser(),true);
                            break;
                        case RoleChannels.TITLE_INIT_QUERY_L:
                            RoleChannels.processInitialQuery(event,event.getUser(),false);
                            break;
                        case RoleChannels.TITLE_CH_QUERY_J:
                            size = RoleChannels.processChannelQuery(event,true);
                            RequestBuffer.request(() ->event.getMessage().removeReaction(event.getUser(),event.getReaction()));
                            break;
                        case RoleChannels.TITLE_CH_QUERY_L:
                            size = RoleChannels.processChannelQuery(event,false);
                            RequestBuffer.request(() ->event.getMessage().removeReaction(event.getUser(),event.getReaction()));
                            break;
                        case RoleChannels.TITLE_CT_QUERY_J:
                            size = RoleChannels.processCategoryQuery(event,true);
                            RequestBuffer.request(() ->event.getMessage().removeReaction(event.getUser(),event.getReaction()));
                            break;
                        case RoleChannels.TITLE_CT_QUERY_L:
                            size = RoleChannels.processCategoryQuery(event,false);
                            RequestBuffer.request(() ->event.getMessage().removeReaction(event.getUser(),event.getReaction()));
                            break;
                    }
                    if(size>0 && size<7){RoleChannels.cutSuperfluousReactions(event.getMessage(),size);}
                }
            }
        }
        else if(!event.getReaction().getUserReacted(event.getClient().getOurUser())){
            if(event.getReaction().getEmoji().getName().equals(BotUtils.R)){
                log(event.getGuild(),"Found an R",INFO);
                MiscTasks.handleR(event);
            }
        }
    }

    @EventSubscriber
    public void userGuildJoin(UserJoinEvent event){
        if(ServerSettings.messagesNewUsers(event.getGuild())){
            String msg = ServerSettings.getNewUserMessage(event.getGuild());
            event.getUser().getOrCreatePMChannel().sendMessage(msg);
        }
    }
    @EventSubscriber
    public void userVoiceLeave(UserVoiceChannelLeaveEvent event){
        if(event.getVoiceChannel().getConnectedUsers().contains(event.getClient().getOurUser())
            && event.getVoiceChannel().getConnectedUsers().size()==1){
            event.getVoiceChannel().leave();
        }
    }

    @EventSubscriber
    public void messageReceived(MessageReceivedEvent event) {
        if(event.getMessage().getContent().contains(BotUtils.R)){
            log(event.getGuild(),"Found an R",INFO);
            MiscTasks.handleR(event);
        }

        // When @everyone is tagged with a ? in the same message. Doesn't trigger on links
        if(event.getMessage().mentionsEveryone() && event.getMessage().getContent().contains("?")
            && !event.getMessage().getContent().contains("https")){
            RequestBuffer.request(() -> event.getMessage().addReaction(ReactionEmoji.of(CHECK_MARK))).get();
            RequestBuffer.request(() -> event.getMessage().addReaction(ReactionEmoji.of(RED_X))).get();
            return;
        }

        String query[] = event.getMessage().getContent().split("\\s");

        if(query.length == 0){
            return;
        }
        if(!query[0].startsWith(BOT_PREFIX)){
            return;
        }

        String cmd = query[0].substring(1).toUpperCase();

        List<String> args = new ArrayList<>(Arrays.asList(query));
        log(event.getGuild(),"Command: "+ cmd, INFO);
        args.remove(0);

        if(commandMap.containsKey(cmd)){
            log(event.getGuild(),"Valid command: "+cmd, SUCC);
            Command command = commandMap.get(cmd);
            if(BotUtils.hasPermission(event,command.getPermissions(), true)) command.runCommand(event,args);
        }else{
            if(!CustomCommands.invoke(event,cmd)){
                log(event.getGuild(),"Invalid command: "+cmd, UERROR);
            }
        }
    }
}
