package io.github.aquerr.eaglefactions.common.entities;

import io.github.aquerr.eaglefactions.api.entities.*;
import org.spongepowered.api.text.Text;

import java.time.Instant;
import java.util.*;

/**
 * Represents immutable dto class used for saving faction state in the storage.
 */
public class FactionState
{
    private final String name;
    private final Text tag;
    private final String description;
    private final String messageOfTheDay;
    private final Set<UUID> recruits;
    private final Set<UUID> members;
    private final Set<String> truces;
    private final Set<String> alliances;
    private final Set<String> enemies;
    private final UUID leader;
    private final Set<UUID> officers;
    private final Set<Claim> claims;
    private final FactionHome home;
    private final Instant lastOnline;
    private final boolean isPublic;
    private final FactionChest chest;
    private final Map<FactionMemberType, Map<FactionPermType, Boolean>> perms;

    public FactionState(final Faction faction)
    {
        this.name = faction.getName();
        this.tag = faction.getTag();
        this.leader = faction.getLeader();
        this.description = faction.getDescription();
        this.messageOfTheDay = faction.getMessageOfTheDay();
        this.recruits = new HashSet<>(faction.getRecruits());
        this.members = new HashSet<>(faction.getMembers());
        this.officers = new HashSet<>(faction.getOfficers());
        this.truces = new HashSet<>(faction.getTruces());
        this.alliances = new HashSet<>(faction.getAlliances());
        this.enemies = new HashSet<>(faction.getEnemies());
        this.claims = new HashSet<>(faction.getClaims());
        this.home = faction.getHome();
        this.lastOnline = faction.getLastOnline();
        this.isPublic = faction.isPublic();
        this.chest = new FactionChestImpl(faction.getName(), faction.getChest().getInventory());
        this.perms = new HashMap<>(faction.getPerms());
    }

    public String getName()
    {
        return name;
    }

    public Text getTag()
    {
        return tag;
    }

    public String getDescription()
    {
        return description;
    }

    public String getMessageOfTheDay()
    {
        return messageOfTheDay;
    }

    public Set<UUID> getRecruits()
    {
        return recruits;
    }

    public Set<UUID> getMembers()
    {
        return members;
    }

    public Set<String> getTruces()
    {
        return truces;
    }

    public Set<String> getAlliances()
    {
        return alliances;
    }

    public Set<String> getEnemies()
    {
        return enemies;
    }

    public UUID getLeader()
    {
        return leader;
    }

    public Set<UUID> getOfficers()
    {
        return officers;
    }

    public Set<Claim> getClaims()
    {
        return claims;
    }

    public FactionHome getHome()
    {
        return home;
    }

    public Instant getLastOnline()
    {
        return lastOnline;
    }

    public boolean isPublic()
    {
        return isPublic;
    }

    public Map<FactionMemberType, Map<FactionPermType, Boolean>> getPerms()
    {
        return perms;
    }

    public FactionChest getChest()
    {
        return chest;
    }
}
