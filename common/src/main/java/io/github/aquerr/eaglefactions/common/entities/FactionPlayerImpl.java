package io.github.aquerr.eaglefactions.common.entities;

import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.api.entities.FactionPlayer;
import io.github.aquerr.eaglefactions.common.EagleFactionsPlugin;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;

import java.util.Optional;
import java.util.UUID;

public class FactionPlayerImpl implements FactionPlayer
{
    private final UUID uniqueId;
    private final String name;

    private String factionName;

    private boolean diedInWarZone;

    private float power;
    private float maxpower;

    public FactionPlayerImpl(final String playerName, final UUID uniqueId, final String factionName, final float power, final float maxpower, final boolean diedInWarZone)
    {
        this.name = playerName;
        this.uniqueId = uniqueId;

        if (StringUtils.isBlank(factionName))
            this.factionName = null;
        else this.factionName = factionName;

        this.diedInWarZone = diedInWarZone;

        this.power = power;
        this.maxpower = maxpower;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Optional<String> getFactionName()
    {
        return Optional.ofNullable(this.factionName);
    }

    @Override
    public UUID getUniqueId()
    {
        return this.uniqueId;
    }

    @Override
    public Optional<Faction> getFaction()
    {
        return getFactionName()
                .filter(StringUtils::isNotBlank)
                .map(EagleFactionsPlugin.getPlugin().getFactionLogic()::getFactionByName);
    }

    @Override
    public FactionMemberType getFactionRole()
    {
        return this.getFaction().map(faction -> faction.getPlayerMemberType(this.uniqueId)).orElse(FactionMemberType.NONE);
    }

    @Override
    public boolean isOnline()
    {
        final Optional<User> optionalUser = getUser();
        return optionalUser.map(User::isOnline).orElse(false);
    }

    @Override
    public float getPower()
    {
        return this.power;
    }

    @Override
    public float getMaxPower()
    {
        return this.maxpower;
    }

    @Override
    public Optional<User> getUser()
    {
        final Optional<UserStorageService> userStorageService = Sponge.getServiceManager().provide(UserStorageService.class);
        return userStorageService.flatMap(storageService -> storageService.get(this.uniqueId));
    }

    @Override
    public boolean diedInWarZone()
    {
        return this.diedInWarZone;
    }
}
