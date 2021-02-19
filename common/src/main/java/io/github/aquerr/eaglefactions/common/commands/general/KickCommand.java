package io.github.aquerr.eaglefactions.common.commands.general;

import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.api.entities.FactionPlayer;
import io.github.aquerr.eaglefactions.common.EagleFactionsPlugin;
import io.github.aquerr.eaglefactions.common.PluginInfo;
import io.github.aquerr.eaglefactions.common.PluginPermissions;
import io.github.aquerr.eaglefactions.common.commands.CommandBase;
import io.github.aquerr.eaglefactions.common.commands.EagleFactionsCommand;
import io.github.aquerr.eaglefactions.common.commands.args.FactionPlayerArgument;
import io.github.aquerr.eaglefactions.common.events.EventRunner;
import io.github.aquerr.eaglefactions.common.messaging.MessageLoader;
import io.github.aquerr.eaglefactions.common.messaging.Messages;
import io.github.aquerr.eaglefactions.common.messaging.Placeholders;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Collections;
import java.util.Optional;

@EagleFactionsCommand(
        permission = PluginPermissions.KICK_COMMAND,
        canBeUsedFromConsole = false,
        mustBeInFaction = true,
        minimumRank = FactionMemberType.OFFICER
)
public class KickCommand extends CommandBase
{
    public KickCommand(final EagleFactions plugin)
    {
        super(plugin);
    }

    @Override
    protected CommandElement[] getDefinedCommandArgs()
    {
        return new CommandElement[] {
                GenericArguments.onlyOne(new FactionPlayerArgument(super.getPlugin(), Text.of("player")))
        };
    }

    @Override
    protected Text getDescription()
    {
        return Text.of(Messages.COMMAND_KICK_DESC);
    }

    @Override
    protected CommandResult execute(CommandSource source, CommandContext context, boolean hasAdminMode) throws CommandException
    {
        final FactionPlayer selectedPlayer = context.<FactionPlayer>requireOne(Text.of("player"));

        final Player player = (Player)source;
        final Optional<Faction> optionalPlayerFaction = super.getPlugin().getFactionLogic().getFactionByPlayerUUID(player.getUniqueId());
        final Faction playerFaction = optionalPlayerFaction.get();
        final Optional<Faction> optionalSelectedPlayerFaction = super.getPlugin().getFactionLogic().getFactionByPlayerUUID(selectedPlayer.getUniqueId());
        if(!optionalSelectedPlayerFaction.isPresent())
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.THIS_PLAYER_IS_NOT_IN_YOUR_FACTION));

        if(!optionalSelectedPlayerFaction.get().getName().equals(playerFaction.getName()))
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.THIS_PLAYER_IS_NOT_IN_YOUR_FACTION));

        if(playerFaction.getLeader().equals(selectedPlayer.getUniqueId()) || (playerFaction.getOfficers().contains(player.getUniqueId()) && playerFaction.getOfficers().contains(selectedPlayer.getUniqueId())))
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_CANT_KICK_THIS_PLAYER));

        final boolean isCancelled = EventRunner.runFactionKickEventPre(selectedPlayer, player, playerFaction);
        if(!isCancelled)
        {
            super.getPlugin().getFactionLogic().kickPlayer(selectedPlayer.getUniqueId(), playerFaction.getName());
            source.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.YOU_KICKED_PLAYER_FROM_THE_FACTION, TextColors.GREEN, Collections.singletonMap(Placeholders.PLAYER, Text.of(TextColors.GOLD, selectedPlayer.getName())))));

            if(super.getPlugin().getPlayerManager().isPlayerOnline(selectedPlayer.getUniqueId()))
            {
                super.getPlugin().getPlayerManager().getPlayer(selectedPlayer.getUniqueId()).get().sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, Messages.YOU_WERE_KICKED_FROM_THE_FACTION));
            }

            EagleFactionsPlugin.AUTO_CLAIM_LIST.remove(selectedPlayer.getUniqueId());
            EagleFactionsPlugin.CHAT_LIST.remove(selectedPlayer.getUniqueId());
            EventRunner.runFactionKickEventPost(selectedPlayer, player, playerFaction);
        }

        return CommandResult.success();
    }
}
