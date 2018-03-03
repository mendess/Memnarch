package com.github.mendess2526.memnarch.misc;

import com.github.mendess2526.memnarch.Command;
import sx.blah.discord.handle.obj.Permissions;

import java.util.EnumSet;
import java.util.Set;

public abstract class CMiscCommands implements Command {

    @Override
    public String getCommandGroup(){
        return "Misc Commands";
    }

    @Override
    public Set<Permissions> getPermissions(){
        return EnumSet.noneOf(Permissions.class);
    }
}
