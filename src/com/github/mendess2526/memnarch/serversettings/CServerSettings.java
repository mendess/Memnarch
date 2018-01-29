package com.github.mendess2526.memnarch.serversettings;

import com.github.mendess2526.memnarch.Command;
import sx.blah.discord.handle.obj.Permissions;

import java.util.Set;

public abstract class CServerSettings implements Command {
    //TODO implement
    @Override
    public String getCommandGroup(){
        return "Server Settings";
    }

    @Override
    public Set<Permissions> getPermissions(){
        return null;
    }

}
