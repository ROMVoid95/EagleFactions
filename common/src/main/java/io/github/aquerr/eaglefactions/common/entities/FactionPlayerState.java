package io.github.aquerr.eaglefactions.common.entities;

import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.entities.FactionPlayer;

import java.util.UUID;

/**
 * Represents immutable dto class used for saving faction player state in the storage.
 */
public class FactionPlayerState
{
    private final String name;
    private final UUID uniqueId;
    private final String factionName;
    private final boolean diedInWarZone;

    private final float power;
    private final float maxpower;

    public FactionPlayerState(final FactionPlayer factionPlayer)
    {
        this.name = factionPlayer.getName();
        this.uniqueId = factionPlayer.getUniqueId();
        this.factionName = factionPlayer.getFaction().map(Faction::getName).orElse("");
        this.diedInWarZone = factionPlayer.diedInWarZone();
        this.power = factionPlayer.getPower();
        this.maxpower = factionPlayer.getMaxPower();
    }

    public String getName()
    {
        return name;
    }

    public UUID getUniqueId()
    {
        return uniqueId;
    }

    public String getFactionName()
    {
        return factionName;
    }

    public boolean diedInWarZone()
    {
        return diedInWarZone;
    }

    public float getPower()
    {
        return power;
    }

    public float getMaxpower()
    {
        return maxpower;
    }
}
