package io.github.aquerr.eaglefactions.common.commands;

import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EagleFactionsCommand
{
    FactionMemberType requiredRank();

    boolean requireAdminMode();
}
