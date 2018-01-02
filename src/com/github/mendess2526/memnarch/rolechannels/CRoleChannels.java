package com.github.mendess2526.memnarch.rolechannels;

import com.github.mendess2526.memnarch.Command;
import sx.blah.discord.handle.obj.Permissions;

import java.util.Set;

public abstract class CRoleChannels implements Command {
    //TODO implement
    @Override
    public String getCommandGroup(){
        return "Role Channels";
    }

    @Override
    public Set<Permissions> getPermissions(){
        return null;
    }
}
