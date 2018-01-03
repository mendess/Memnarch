package com.github.mendess2526.memnarch.sounds;

import com.github.mendess2526.memnarch.Command;
import sx.blah.discord.handle.obj.Permissions;

import java.util.Set;

public abstract class CSounds implements Command{
    //TODO implement
    @Override
    public String getCommandGroup(){
        return "Sfx";
    }

    @Override
    public Set<Permissions> getPermissions(){
        return null;
    }
}
