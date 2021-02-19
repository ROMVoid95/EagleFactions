package io.github.aquerr.eaglefactions.common.caching;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.aquerr.eaglefactions.api.entities.Claim;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.entities.FactionPlayer;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Singleton
public class FactionsCache
{
    private static final FactionsCache INSTANCE = new FactionsCache();


    //TODO: Consider converting factions and players cache into Guava Cache.
    private final Map<String, Faction> factionsCache = new HashMap<>();
    private final Map<UUID, FactionPlayer> factionsPlayerCache = new HashMap<>();

    // TODO: Add cache time to configuration?
    private final Cache<Claim, Optional<Faction>> claimsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build();

    public static FactionsCache getInstance()
    {
        return INSTANCE;
    }

    private FactionsCache()
    {

    }

    public Optional<Faction> getClaimFaction(Claim claim) {
        return claimsCache.getIfPresent(claim);
    }

    public void updateClaimFaction(Claim claim, Optional<Faction> faction) {
        claimsCache.put(claim, faction);
    }

    public Map<UUID, FactionPlayer> getPlayersMap()
    {
        return factionsPlayerCache;
    }

    public void savePlayer(final FactionPlayer factionPlayer)
    {
        synchronized (factionsPlayerCache)
        {
            factionsPlayerCache.put(factionPlayer.getUniqueId(), factionPlayer);
        }
    }

    public @Nullable FactionPlayer getPlayer(final UUID playerUniqueId)
    {
        synchronized (factionsPlayerCache)
        {
            return factionsPlayerCache.get(playerUniqueId);
        }
    }

    public void removePlayer(final UUID playerUniqueId)
    {
        synchronized (factionsPlayerCache)
        {
            factionsPlayerCache.remove(playerUniqueId);
        }
    }

    public Map<String, Faction> getFactionsMap()
    {
        return Collections.unmodifiableMap(factionsCache);
    }

    public void saveFaction(final Faction faction)
    {
        synchronized (factionsCache)
        {
            Faction factionToUpdate = factionsCache.get(faction.getName().toLowerCase());

            if (factionToUpdate != null)
            {
                factionsCache.replace(factionToUpdate.getName().toLowerCase(), faction);
                factionToUpdate.getClaims().forEach(claim -> claimsCache.put(claim, Optional.empty()));
            }
            else
            {
                factionsCache.put(faction.getName().toLowerCase(), faction);
            }

            if(!faction.getClaims().isEmpty())
            {
                faction.getClaims().forEach(claim -> claimsCache.put(claim, Optional.of(faction)));
            }
        }
    }

    public void removeFaction(final String factionName)
    {
        synchronized (factionsCache)
        {
            Faction faction = factionsCache.remove(factionName.toLowerCase());
            faction.getClaims().forEach(claim -> claimsCache.put(claim, Optional.empty()));
        }
    }

    @Nullable
    public Faction getFaction(final String factionName)
    {
        return factionsCache.get(factionName.toLowerCase());
    }

    public Map<Claim, Optional<Faction>> getClaims()
    {
        return claimsCache.asMap();
    }

    public void removeClaim(final Claim claim)
    {
        claimsCache.put(claim, Optional.empty());
    }

    public void clear()
    {
        claimsCache.invalidateAll();
        factionsCache.clear();
        factionsPlayerCache.clear();
    }
}
