package io.github.aquerr.eaglefactions.common.commands;

import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EagleFactionsCommand
{
    String permission();

    boolean canBeUsedFromConsole();

    boolean mustBeInFaction();

    FactionMemberType minimumRank();
}
