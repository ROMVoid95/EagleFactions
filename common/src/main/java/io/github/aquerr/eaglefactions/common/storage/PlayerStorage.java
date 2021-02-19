package io.github.aquerr.eaglefactions.common.storage;

import io.github.aquerr.eaglefactions.api.entities.FactionPlayer;
import io.github.aquerr.eaglefactions.common.entities.FactionPlayerState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PlayerStorage
{
    @Nullable FactionPlayer getPlayer(UUID playerUUID);

    boolean savePlayer(FactionPlayerState factionPlayerState);

    boolean savePlayers(List<FactionPlayer> players);

    Set<String> getServerPlayerNames();

    Set<FactionPlayer> getServerPlayers();

    void deletePlayers();
}
