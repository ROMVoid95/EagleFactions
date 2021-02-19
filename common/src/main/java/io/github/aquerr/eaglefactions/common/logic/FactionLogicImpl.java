package io.github.aquerr.eaglefactions.common.logic;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.github.aquerr.eaglefactions.api.config.FactionsConfig;
import io.github.aquerr.eaglefactions.api.entities.*;
import io.github.aquerr.eaglefactions.api.logic.FactionLogic;
import io.github.aquerr.eaglefactions.api.managers.PlayerManager;
import io.github.aquerr.eaglefactions.api.storage.StorageManager;
import io.github.aquerr.eaglefactions.common.PluginInfo;
import io.github.aquerr.eaglefactions.common.caching.FactionsCache;
import io.github.aquerr.eaglefactions.common.entities.FactionPlayerImpl;
import io.github.aquerr.eaglefactions.common.events.EventRunner;
import io.github.aquerr.eaglefactions.common.exception.RequiredItemsNotFoundException;
import io.github.aquerr.eaglefactions.common.messaging.MessageLoader;
import io.github.aquerr.eaglefactions.common.messaging.Messages;
import io.github.aquerr.eaglefactions.common.messaging.Placeholders;
import io.github.aquerr.eaglefactions.common.scheduling.ClaimDelayTask;
import io.github.aquerr.eaglefactions.common.scheduling.EagleFactionsScheduler;
import io.github.aquerr.eaglefactions.common.util.ItemUtil;
import io.github.aquerr.eaglefactions.common.util.ParticlesUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Aquerr on 2017-07-12.
 */
public class FactionLogicImpl implements FactionLogic
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FactionLogicImpl.class);

    private final FactionsCache factionsCache = FactionsCache.getInstance();
    private final StorageManager storageManager;
    private final FactionsConfig factionsConfig;
    private final PlayerManager playerManager;

    public FactionLogicImpl(final PlayerManager playerManager, final StorageManager storageManager, final FactionsConfig factionsConfig)
    {
        this.storageManager = storageManager;
        this.playerManager = playerManager;
        this.factionsConfig = factionsConfig;
    }

    @Override
    public Optional<Faction> getFactionByPlayerUUID(UUID playerUUID)
    {
        checkNotNull(playerUUID);

        //TODO: Theoretically, we could get faction directly from the player... but... let's test it before...
        return this.playerManager.getFactionPlayer(playerUUID)
                .flatMap(FactionPlayer::getFactionName)
                .map(this::getFactionByName);
    }

    @Override
    public Optional<Faction> getFactionByChunk(final UUID worldUUID, final Vector3i chunk)
    {
        checkNotNull(worldUUID);
        checkNotNull(chunk);

        Claim claim = new Claim(worldUUID, chunk);

        Optional<Faction> cachedOptional = factionsCache.getClaimFaction(claim);
        //noinspection OptionalAssignedToNull
        if (cachedOptional != null) return cachedOptional;

        for(Faction faction : getFactions().values())
        {
            if(faction.getClaims().contains(claim))
            {
                factionsCache.updateClaimFaction(claim, Optional.of(faction));
                return Optional.of(faction);
            }
        }

        factionsCache.updateClaimFaction(claim, Optional.empty());
        return Optional.empty();
    }

    @Override
    public @Nullable Faction getFactionByName(String factionName)
    {
        Validate.notBlank(factionName);
        return storageManager.getFaction(factionName);
    }

    @Override
    public List<Player> getOnlinePlayers(final Faction faction)
    {
        checkNotNull(faction);

        final List<Player> factionPlayers = new ArrayList<>();
        final UUID factionLeader = faction.getLeader();
        if(!faction.getLeader().equals(new UUID(0, 0)) && this.playerManager.isPlayerOnline(factionLeader))
        {
            factionPlayers.add(playerManager.getPlayer(factionLeader).get());
        }

        for(final UUID uuid : faction.getOfficers())
        {
            if(playerManager.isPlayerOnline(uuid))
            {
                factionPlayers.add(playerManager.getPlayer(uuid).get());
            }
        }

        for(final UUID uuid : faction.getMembers())
        {
            if(playerManager.isPlayerOnline(uuid))
            {
                factionPlayers.add(playerManager.getPlayer(uuid).get());
            }
        }

        for(final UUID uuid : faction.getRecruits())
        {
            if(playerManager.isPlayerOnline(uuid))
            {
                factionPlayers.add(playerManager.getPlayer(uuid).get());
            }
        }

        return factionPlayers;
    }

    @Override
    public Set<String> getFactionsNames()
    {
        return getFactions().keySet();
    }

    @Override
    public Map<String, Faction> getFactions()
    {
        return new HashMap<>(factionsCache.getFactionsMap());
    }

    @Override
    public Map<Claim, Optional<Faction>> getAllClaims()
    {
        return new HashMap<>(factionsCache.getClaims());
    }

    @Override
    public void addFaction(final Faction faction)
    {
        checkNotNull(faction);
        storageManager.saveFaction(faction);
    }

    @Override
    public boolean disbandFaction(final String factionName)
    {
        checkNotNull(factionName);

        final Faction factionToDisband = getFactionByName(factionName);

        Preconditions.checkNotNull(factionToDisband, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), factionName));

        //Update players...
        CompletableFuture.runAsync(() -> {
            final Set<UUID> playerUUIDs = factionToDisband.getPlayers();
            for (final UUID playerUUID : playerUUIDs)
            {
                //Faction Player should always exists so we do not need to check if it is present.
                this.playerManager.getFactionPlayer(playerUUID).ifPresent(factionPlayer -> {
                    factionPlayer.setFaction(null);
                    this.storageManager.savePlayer(factionPlayer);
                });
            }
        });

        // Update other factions
        CompletableFuture.runAsync(() -> {
            final Set<String> alliances = factionToDisband.getAlliances();
            final Set<String> truces = factionToDisband.getTruces();
            final Set<String> enemies = factionToDisband.getEnemies();
            for (final String alliance : alliances)
            {
                removeAlly(alliance, factionToDisband.getName());
            }
            for (final String truce : truces)
            {
                removeTruce(truce, factionToDisband.getName());
            }
            for (final String enemy : enemies)
            {
                removeEnemy(enemy, factionToDisband.getName());
            }
        });
        return this.storageManager.deleteFaction(factionName);
    }

    @Override
    public void joinFaction(final UUID playerUUID, final String factionName)
    {
        checkNotNull(playerUUID);
        checkNotNull(factionName);

        Faction faction = getFactionByName(factionName);
        checkNotNull(faction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), factionName));

        faction.getRecruits().add(playerUUID);

        this.storageManager.saveFaction(faction);

        //Save player...
        this.playerManager.getFactionPlayer(playerUUID).ifPresent(factionPlayer -> {
            factionPlayer.setFaction(faction);
            this.storageManager.savePlayer(factionPlayer);
        });
    }

    @Override
    public void leaveFaction(final UUID playerUUID, final String factionName)
    {
        checkNotNull(playerUUID);
        checkNotNull(factionName);

        final Faction faction = getFactionByName(factionName);
        checkNotNull(faction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), factionName));

        final Set<UUID> recruits = faction.getRecruits();
        final Set<UUID> members = faction.getMembers();
        final Set<UUID> officers = faction.getOfficers();

        if(faction.getRecruits().contains(playerUUID))
        {
            recruits.remove(playerUUID);
        }
        else if(faction.getMembers().contains(playerUUID))
        {
            members.remove(playerUUID);
        }
        else
        {
            officers.remove(playerUUID);
        }

        //Remove player from claim owners
        for (final Claim claim : faction.getClaims()) {
            final Set<UUID> owners = claim.getOwners();
            owners.remove(playerUUID);
        }

        this.storageManager.saveFaction(faction);

        //Save player...
        this.playerManager.getFactionPlayer(playerUUID).ifPresent(factionPlayer -> {
            factionPlayer.setFaction(null);
            this.storageManager.savePlayer(factionPlayer);
        });
    }

    @Override
    public void addTruce(final String playerFactionName, final String invitedFactionName)
    {
        Validate.notBlank(playerFactionName);
        Validate.notBlank(invitedFactionName);

        final Faction playerFaction = getFactionByName(playerFactionName);
        final Faction invitedFaction = getFactionByName(invitedFactionName);

        checkNotNull(playerFaction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), playerFactionName));
        checkNotNull(invitedFaction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), invitedFactionName));

        final Set<String> playerFactionTruces = playerFaction.getTruces();
        final Set<String> invitedFactionTruces = invitedFaction.getTruces();

        playerFactionTruces.add(invitedFactionName);
        invitedFactionTruces.add(playerFactionName);

        this.storageManager.saveFaction(playerFaction);
        this.storageManager.saveFaction(invitedFaction);
    }

    @Override
    public void removeTruce(final String playerFactionName, final String removedFactionName)
    {
        Validate.notBlank(playerFactionName);
        Validate.notBlank(removedFactionName);

        final Faction playerFaction = getFactionByName(playerFactionName);
        final Faction removedFaction = getFactionByName(removedFactionName);

        checkNotNull(playerFaction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), playerFactionName));
        checkNotNull(removedFaction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), removedFactionName));

        final Set<String> playerFactionTruces = playerFaction.getTruces();
        final Set<String> removedFactionTruces = removedFaction.getTruces();

        playerFactionTruces.remove(removedFactionName);
        removedFactionTruces.remove(playerFactionName);

        this.storageManager.saveFaction(playerFaction);
        this.storageManager.saveFaction(removedFaction);
    }

    @Override
    public void addAlly(final String playerFactionName, final String invitedFactionName)
    {
        Validate.notBlank(playerFactionName);
        Validate.notBlank(invitedFactionName);

        final Faction playerFaction = getFactionByName(playerFactionName);
        final Faction invitedFaction = getFactionByName(invitedFactionName);

        checkNotNull(playerFaction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), playerFactionName));
        checkNotNull(invitedFaction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), invitedFactionName));

        final Set<String> playerFactionAlliances = playerFaction.getAlliances();
        final Set<String> invitedFactionAlliances = invitedFaction.getAlliances();

        playerFactionAlliances.add(invitedFactionName);
        invitedFactionAlliances.add(playerFactionName);

        this.storageManager.saveFaction(playerFaction);
        this.storageManager.saveFaction(invitedFaction);
    }

    @Override
    public void removeAlly(final String playerFactionName, final String removedFactionName)
    {
        Validate.notBlank(playerFactionName);
        Validate.notBlank(removedFactionName);

        final Faction playerFaction = getFactionByName(playerFactionName);
        final Faction removedFaction = getFactionByName(removedFactionName);

        checkNotNull(playerFaction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), playerFactionName));
        checkNotNull(removedFaction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), removedFactionName));


        final Set<String> playerFactionAlliances = playerFaction.getAlliances();
        final Set<String> removedFactionAlliances = removedFaction.getAlliances();

        playerFactionAlliances.remove(removedFactionName);
        removedFactionAlliances.remove(playerFactionName);

        this.storageManager.saveFaction(playerFaction);
        this.storageManager.saveFaction(removedFaction);
    }

    @Override
    public void addEnemy(final String playerFactionName, final String enemyFactionName)
    {
        Validate.notBlank(playerFactionName);
        Validate.notBlank(enemyFactionName);

        final Faction playerFaction = getFactionByName(playerFactionName);
        final Faction enemyFaction = getFactionByName(enemyFactionName);

        checkNotNull(playerFaction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), playerFactionName));
        checkNotNull(enemyFaction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), enemyFactionName));

        final Set<String> playerFactionEnemies = playerFaction.getEnemies();
        final Set<String> enemyFactionEnemies = enemyFaction.getEnemies();

        playerFactionEnemies.add(enemyFactionName);
        enemyFactionEnemies.add(playerFactionName);

        this.storageManager.saveFaction(playerFaction);
        this.storageManager.saveFaction(enemyFaction);
    }

    @Override
    public void removeEnemy(final String playerFactionName, final String enemyFactionName)
    {
        Validate.notBlank(playerFactionName);
        Validate.notBlank(enemyFactionName);

        final Faction playerFaction = getFactionByName(playerFactionName);
        final Faction enemyFaction = getFactionByName(enemyFactionName);

        checkNotNull(playerFaction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), playerFactionName));
        checkNotNull(enemyFaction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), enemyFactionName));

        final Set<String> playerFactionEnemies = playerFaction.getEnemies();
        final Set<String> enemyFactionEnemies = enemyFaction.getEnemies();

        playerFactionEnemies.remove(enemyFactionName);
        enemyFactionEnemies.remove(playerFactionName);

        storageManager.saveFaction(playerFaction);
        storageManager.saveFaction(enemyFaction);
    }

    @Override
    public void setLeader(final UUID newLeaderUUID, final String playerFactionName)
    {
        checkNotNull(newLeaderUUID);
        Validate.notBlank(playerFactionName);

        final Faction faction = getFactionByName(playerFactionName);
        checkNotNull(faction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), playerFactionName));

        final Set<UUID> officers = faction.getOfficers();
        final Set<UUID> members = faction.getMembers();
        final Set<UUID> recruits = faction.getRecruits();

        if(!faction.getLeader().equals(new UUID(0, 0)))
        {
            officers.add(faction.getLeader());
        }

        if(faction.getOfficers().contains(newLeaderUUID))
        {
            officers.remove(newLeaderUUID);
        }
        else if(faction.getMembers().contains(newLeaderUUID))
        {
            members.remove(newLeaderUUID);
        }
        else if(faction.getRecruits().contains(newLeaderUUID))
        {
            recruits.remove(newLeaderUUID);
        }

        this.storageManager.saveFaction(faction);
    }

    @Override
    public void addClaims(final Faction faction, final List<Claim> claims)
    {
        checkNotNull(faction);
        checkNotNull(claims);

        final Set<Claim> factionClaims = faction.getClaims();

        for(final Claim claim : claims)
        {
            factionClaims.add(claim);
            ParticlesUtil.spawnClaimParticles(claim);
        }

        this.storageManager.saveFaction(faction);
    }

    @Override
    public void addClaim(final Faction faction, final Claim claim)
    {
        checkNotNull(faction);
        checkNotNull(claim);

        final Set<Claim> claims = faction.getClaims();
        claims.add(claim);
        this.storageManager.saveFaction(faction);

		ParticlesUtil.spawnClaimParticles(claim);
    }

    @Override
    public void removeClaim(final Faction faction, final Claim claim)
    {
        checkNotNull(faction);
        checkNotNull(claim);

        removeClaimInternal(faction, claim);
		ParticlesUtil.spawnUnclaimParticles(claim);
    }

    @Override
    public void destroyClaim(final Faction faction, final Claim claim)
    {
        checkNotNull(faction);
        checkNotNull(claim);

        removeClaimInternal(faction, claim);
        ParticlesUtil.spawnDestroyClaimParticles(claim);
    }

    @Override
    public boolean isClaimed(final UUID worldUUID, final Vector3i chunk)
    {
        checkNotNull(worldUUID);
        checkNotNull(chunk);
        return getFactionByChunk(worldUUID, chunk).isPresent();
    }

    @Override
    public boolean isClaimConnected(final Faction faction, final Claim claimToCheck)
    {
        checkNotNull(faction);
        checkNotNull(claimToCheck);

        if (faction.getClaims().size() == 0)
            return true;

        for(final Claim claim : faction.getClaims())
        {
            if(!claimToCheck.getWorldUUID().equals(claim.getWorldUUID()))
                continue;

            final Vector3i chunkToCheck = claimToCheck.getChunkPosition();
            final Vector3i claimChunk = claim.getChunkPosition();

            if((claimChunk.getX() == chunkToCheck.getX()) && ((claimChunk.getZ() + 1 == chunkToCheck.getZ()) || (claimChunk.getZ() - 1 == chunkToCheck.getZ())))
            {
                return true;
            }
            else if((claimChunk.getZ() == chunkToCheck.getZ()) && ((claimChunk.getX() + 1 == chunkToCheck.getX()) || (claimChunk.getX() - 1 == chunkToCheck.getX())))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean addClaimOwner(final Faction faction, final Claim claim, final UUID owner)
    {
        checkNotNull(faction);
        checkNotNull(claim);
        checkNotNull(owner);
        faction.getClaimAt(claim.getWorldUUID(), claim.getChunkPosition()).ifPresent(claim1 -> claim1.addOwner(owner));
        this.storageManager.saveFaction(faction);
        return true;
    }

    @Override
    public boolean removeClaimOwner(final Faction faction, final Claim claim, final UUID owner)
    {
        checkNotNull(faction);
        checkNotNull(claim);
        checkNotNull(owner);
        faction.getClaimAt(claim.getWorldUUID(), claim.getChunkPosition()).ifPresent(claim1 -> claim1.removeOwner(owner));
        this.storageManager.saveFaction(faction);
        return true;
    }

    @Override
    public void setClaimAccessibleByFaction(final Faction faction, final Claim claim, final boolean isAccessibleByFaction)
    {
        checkNotNull(faction);
        checkNotNull(claim);

        faction.getClaimAt(claim.getWorldUUID(), claim.getChunkPosition()).ifPresent(claim1 -> claim1.setAccessibleByFaction(isAccessibleByFaction));
        this.storageManager.saveFaction(faction);
    }

    @Override
    public void setHome(final Faction faction, final @Nullable FactionHome home)
    {
        checkNotNull(faction);
        faction.setHome(home);
        storageManager.saveFaction(faction);
    }

    @Override
    public List<String> getFactionsTags()
    {
        final Collection<Faction> factionsList = getFactions().values();
        final List<String> factionsTags = new ArrayList<>();

        for(final Faction faction : factionsList)
        {
            factionsTags.add(faction.getTag().toPlain());
        }

        return factionsTags;
    }

    @Override
    public boolean hasOnlinePlayers(final Faction faction)
    {
        checkNotNull(faction);

        if(faction.getLeader() != null && !faction.getLeader().toString().equals(""))
        {
            if(playerManager.isPlayerOnline(faction.getLeader()))
            {
                return true;
            }
        }

        for(final UUID playerUUID : faction.getOfficers())
        {
            if(playerManager.isPlayerOnline(playerUUID))
            {
                return true;
            }
        }

        for(final UUID playerUUID : faction.getMembers())
        {
            if(playerManager.isPlayerOnline(playerUUID))
            {
                return true;
            }
        }

        for(final UUID playerUUID : faction.getRecruits())
        {
            if(playerManager.isPlayerOnline(playerUUID))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void removeAllClaims(final Faction faction)
    {
        checkNotNull(faction);
        faction.getClaims().clear();
        storageManager.saveFaction(faction);
    }

    @Override
    public void kickPlayer(final UUID playerUUID, final String factionName)
    {
        checkNotNull(playerUUID);
        Validate.notBlank(factionName);

        final Faction faction = getFactionByName(factionName);
        checkNotNull(faction, Messages.THERE_IS_NO_FACTION_CALLED_FACTION_NAME.replace(Placeholders.FACTION_NAME.getPlaceholder(), factionName));

        final Set<UUID> officers = faction.getOfficers();
        final Set<UUID> members = faction.getMembers();
        final Set<UUID> recruits = faction.getRecruits();

        if(faction.getRecruits().contains(playerUUID))
        {
            recruits.remove(playerUUID);
        }
        else if(faction.getMembers().contains(playerUUID))
        {
            members.remove(playerUUID);
        }
        else
        {
            officers.remove(playerUUID);
        }

        //Remove player from claim owners
        for (final Claim claim : faction.getClaims()) {
            claim.removeOwner(playerUUID);
        }

        this.storageManager.saveFaction(faction);

        //Update player...
        this.playerManager.getFactionPlayer(playerUUID).ifPresent(factionPlayer -> {
            factionPlayer.setFaction(null);
            this.playerManager.savePlayer(factionPlayer);
        });
    }

    @Override
    public void startClaiming(final Player player, final Faction faction, final UUID worldUUID, final Vector3i chunkPosition)
    {
        checkNotNull(player);
        checkNotNull(faction);
        checkNotNull(worldUUID);
        checkNotNull(chunkPosition);

        if(this.factionsConfig.shouldDelayClaim())
        {
            player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.STAY_IN_THE_CHUNK_FOR_NUMBER_SECONDS_TO_CLAIM_IT, TextColors.GREEN, Collections.singletonMap(Placeholders.NUMBER, Text.of(TextColors.GOLD, this.factionsConfig.getClaimDelay())))));
            EagleFactionsScheduler.getInstance().scheduleWithDelayedInterval(new ClaimDelayTask(player, chunkPosition), 1, TimeUnit.SECONDS, 1, TimeUnit.SECONDS);
        }
        else
        {
            if(this.factionsConfig.shouldClaimByItems())
            {
                boolean didSucceed = addClaimByItems(player, faction, worldUUID, chunkPosition);
                if(didSucceed)
                    player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, Messages.LAND + " ", TextColors.GOLD, chunkPosition.toString(), TextColors.WHITE, " " + Messages.HAS_BEEN_SUCCESSFULLY + " ", TextColors.GOLD, Messages.CLAIMED, TextColors.WHITE, "!"));
                else
                    player.sendMessage(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_DONT_HAVE_ENOUGH_RESOURCES_TO_CLAIM_A_TERRITORY));
            }
            else
            {
                player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, Messages.LAND + " ", TextColors.GOLD, chunkPosition.toString(), TextColors.WHITE, " " + Messages.HAS_BEEN_SUCCESSFULLY + " ", TextColors.GOLD, Messages.CLAIMED, TextColors.WHITE, "!"));
                addClaim(faction, new Claim(worldUUID, chunkPosition));
            }
            EventRunner.runFactionClaimEventPost(player, faction, player.getWorld(), chunkPosition);
        }
    }

    @Override
    public boolean addClaimByItems(final Player player, final Faction faction, final UUID worldUUID, final Vector3i chunkPosition)
    {
        checkNotNull(player);
        checkNotNull(faction);
        checkNotNull(worldUUID);
        checkNotNull(chunkPosition);

        try
        {
            ItemUtil.pollItemsNeededForClaimFromPlayer(player);
            addClaim(faction, new Claim(worldUUID, chunkPosition));
            return true;
        }
        catch (RequiredItemsNotFoundException e)
        {
            return false;
        }
    }

    @Override
    public void togglePerm(final Faction faction, final FactionMemberType factionMemberType, final FactionPermType factionPermType, final Boolean flagValue)
    {
        checkNotNull(faction);
        checkNotNull(factionMemberType);
        checkNotNull(factionPermType);
        checkNotNull(flagValue);

        final Map<FactionMemberType, Map<FactionPermType, Boolean>> perms = faction.getPerms();
        perms.get(factionMemberType).replace(factionPermType, flagValue);
        this.storageManager.saveFaction(faction);
    }

    @Override
    public void changeTagColor(final Faction faction, final TextColor textColor)
    {
        checkNotNull(faction);
        checkNotNull(textColor);

        final Text text = Text.of(textColor, faction.getTag().toPlainSingle());
        faction.setTag(text);
        storageManager.saveFaction(faction);
    }

    @Override
    public FactionMemberType promotePlayer(final Faction faction, final UUID playerToPromote)
    {
        checkNotNull(faction);
        checkNotNull(playerToPromote);

        FactionMemberType promotedTo = FactionMemberType.RECRUIT;

        final Set<UUID> recruits = faction.getRecruits();
        final Set<UUID> members = faction.getMembers();
        final Set<UUID> officers = faction.getOfficers();

        if(recruits.contains(playerToPromote))
        {
            members.add(playerToPromote);
            recruits.remove(playerToPromote);
            promotedTo = FactionMemberType.MEMBER;
        }
        else if (members.contains(playerToPromote))
        {
            officers.add(playerToPromote);
            members.remove(playerToPromote);
            promotedTo = FactionMemberType.OFFICER;
        }

        this.storageManager.saveFaction(faction);
        return promotedTo;
    }

    @Override
    public FactionMemberType demotePlayer(final Faction faction, final UUID playerToDemote)
    {
        checkNotNull(faction);
        checkNotNull(playerToDemote);

        FactionMemberType demotedTo = FactionMemberType.RECRUIT;
        final Set<UUID> recruits = faction.getRecruits();
        final Set<UUID> members = faction.getMembers();
        final Set<UUID> officers = faction.getOfficers();

        if(members.contains(playerToDemote))
        {
            recruits.add(playerToDemote);
            members.remove(playerToDemote);
            demotedTo = FactionMemberType.RECRUIT;
        }
        else if (officers.contains(playerToDemote))
        {
            members.add(playerToDemote);
            officers.remove(playerToDemote);
            demotedTo = FactionMemberType.MEMBER;
        }

        this.storageManager.saveFaction(faction);
        return demotedTo;
    }

    @Override
    public void setLastOnline(final Faction faction, final Instant instantTime)
    {
        checkNotNull(faction);
        checkNotNull(instantTime);

        faction.setLastOnline(instantTime);
        this.storageManager.saveFaction(faction);
    }

    @Override
    public void renameFaction(final Faction faction, final String newFactionName)
    {
        checkNotNull(faction);
        Validate.notBlank(newFactionName);

        this.storageManager.deleteFaction(faction.getName());
        faction.setName(newFactionName);
        this.storageManager.saveFaction(faction);

        // Update other factions
        CompletableFuture.runAsync(() -> {
            final Set<String> alliances = faction.getAlliances();
            final Set<String> truces = faction.getTruces();
            final Set<String> enemies = faction.getEnemies();
            for (final String alliance : alliances)
            {
                removeAlly(alliance, faction.getName());
                addAlly(alliance, newFactionName);
            }
            for (final String truce : truces)
            {
                removeTruce(truce, faction.getName());
                addTruce(truce, newFactionName);
            }
            for (final String enemy : enemies)
            {
                removeEnemy(enemy, faction.getName());
                addEnemy(enemy, newFactionName);
            }
        });

        //Update players...
        CompletableFuture.runAsync(() -> {
           final Set<UUID> playerUUIDs = faction.getPlayers();
           for (final UUID playerUUID : playerUUIDs)
           {
               this.playerManager.getFactionPlayer(playerUUID).ifPresent(factionPlayer -> {
                   factionPlayer.setFaction(faction);
                   this.storageManager.savePlayer(factionPlayer);
               });
           }
        });
    }

    @Override
    public void changeTag(final Faction faction, final String newTag)
    {
        checkNotNull(faction);
        Validate.notBlank(newTag);
        faction.setTag(Text.of(faction.getTag().getColor(), newTag));
        this.storageManager.saveFaction(faction);
    }

    @Override
    public void setChest(final Faction faction, final FactionChest inventory)
    {
        checkNotNull(faction);
        checkNotNull(inventory);
        faction.setChest(inventory);
        this.storageManager.saveFaction(faction);
    }

    @Override
    public void setDescription(final Faction faction, final String description)
    {
        checkNotNull(faction);
        checkNotNull(description);
        faction.setDescription(description);
        this.storageManager.saveFaction(faction);
    }

    @Override
    public void setMessageOfTheDay(final Faction faction, final String motd)
    {
        checkNotNull(faction);
        checkNotNull(motd);
        faction.setMessageOfTheDay(motd);
        this.storageManager.saveFaction(faction);
    }

    @Override
    public void setIsPublic(final Faction faction, final boolean isPublic)
    {
        checkNotNull(faction);
        faction.setIsPublic(isPublic);
        this.storageManager.saveFaction(faction);
    }

    private void removeClaimInternal(final Faction faction, final Claim claim)
    {
        faction.getClaims().remove(claim);
        factionsCache.removeClaim(claim);
        this.storageManager.saveFaction(faction);
    }
}
