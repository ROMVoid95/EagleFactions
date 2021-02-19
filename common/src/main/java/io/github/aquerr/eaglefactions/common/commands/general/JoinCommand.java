package io.github.aquerr.eaglefactions.common.commands.general;

import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.config.FactionsConfig;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.api.entities.Invite;
import io.github.aquerr.eaglefactions.common.EagleFactionsPlugin;
import io.github.aquerr.eaglefactions.common.PluginInfo;
import io.github.aquerr.eaglefactions.common.PluginPermissions;
import io.github.aquerr.eaglefactions.common.commands.AbstractCommand;
import io.github.aquerr.eaglefactions.common.commands.CommandBase;
import io.github.aquerr.eaglefactions.common.commands.EagleFactionsCommand;
import io.github.aquerr.eaglefactions.common.commands.args.FactionArgument;
import io.github.aquerr.eaglefactions.common.events.EventRunner;
import io.github.aquerr.eaglefactions.common.messaging.MessageLoader;
import io.github.aquerr.eaglefactions.common.messaging.Messages;
import io.github.aquerr.eaglefactions.common.messaging.Placeholders;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Collections;

@EagleFactionsCommand(
        permission = PluginPermissions.JOIN_COMMAND,
        canBeUsedFromConsole = false,
        mustBeInFaction = false,
        minimumRank = FactionMemberType.NONE
)
public class JoinCommand extends CommandBase
{
    private final FactionsConfig factionsConfig;

    public JoinCommand(final EagleFactions plugin)
    {
        super(plugin);
        this.factionsConfig = plugin.getConfiguration().getFactionsConfig();
    }

    @Override
    protected CommandElement[] getDefinedCommandArgs()
    {
        return new CommandElement[] {
                new FactionArgument(super.getPlugin(), Text.of("faction"))
        };
    }

    @Override
    protected Text getDescription()
    {
        return Text.of(Messages.COMMAND_JOIN_DESC);
    }

    @Override
    protected CommandResult execute(CommandSource source, CommandContext context, boolean hasAdminMode) throws CommandException
    {
        final Faction faction = context.requireOne("faction");
        final Player player = (Player)source;
        if (super.getPlugin().getFactionLogic().getFactionByPlayerUUID(player.getUniqueId()).isPresent())
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_ARE_ALREADY_IN_A_FACTION));

        //If player has admin mode then force join.
        if (hasAdminMode)
            return joinFactionAndNotify(player, faction);

        if(!faction.isPublic())
        {
            boolean hasInvite = false;
            for (final Invite invite: EagleFactionsPlugin.INVITE_LIST)
            {
                if(invite.getPlayerUUID().equals(player.getUniqueId()) && invite.getFactionName().equals(faction.getName()))
                {
                    hasInvite = true;
                }
            }
            if(!hasInvite)
                throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_HAVENT_BEEN_INVITED_TO_THIS_FACTION));
        }

        //TODO: Should public factions bypass this restriction?
        if(this.factionsConfig.isPlayerLimit())
        {
            int playerCount = faction.getPlayers().size();
            if(playerCount >= this.factionsConfig.getPlayerLimit())
                throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_CANT_JOIN_THIS_FACTION_BECAUSE_IT_REACHED_ITS_PLAYER_LIMIT));
        }

        return joinFactionAndNotify(player, faction);
    }

    private CommandResult joinFactionAndNotify(final Player player, final Faction faction)
    {
        final boolean isCancelled = EventRunner.runFactionJoinEventPre(player, faction);
        if (isCancelled)
            return CommandResult.success();

        super.getPlugin().getFactionLogic().joinFaction(player.getUniqueId(), faction.getName());
        EagleFactionsPlugin.INVITE_LIST.remove(new Invite(faction.getName(), player.getUniqueId()));
        player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.SUCCESSFULLY_JOINED_FACTION, TextColors.GREEN, Collections.singletonMap(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, faction.getName())))));
        EventRunner.runFactionJoinEventPost(player, faction);
        return CommandResult.success();
    }
}
