package io.github.aquerr.eaglefactions.common.commands.management;

import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.common.EagleFactionsPlugin;
import io.github.aquerr.eaglefactions.common.PluginInfo;
import io.github.aquerr.eaglefactions.common.PluginPermissions;
import io.github.aquerr.eaglefactions.common.commands.CommandBase;
import io.github.aquerr.eaglefactions.common.commands.EagleFactionsCommand;
import io.github.aquerr.eaglefactions.common.commands.args.FactionArgument;
import io.github.aquerr.eaglefactions.common.events.EventRunner;
import io.github.aquerr.eaglefactions.common.messaging.Messages;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;

@EagleFactionsCommand(
        permission = PluginPermissions.DISBAND_COMMAND,
        canBeUsedFromConsole = false,
        mustBeInFaction = false,
        minimumRank = FactionMemberType.LEADER
)
public class DisbandCommand extends CommandBase
{
    public DisbandCommand(EagleFactions plugin)
    {
        super(plugin);
    }

    @Override
    protected CommandElement[] getDefinedCommandArgs()
    {
        return new CommandElement[] {
                GenericArguments.optional(new FactionArgument(super.getPlugin(), Text.of("faction")))
        };
    }

    @Override
    protected Text getDescription()
    {
        return Text.of(Messages.COMMAND_DISBAND_DESC);
    }

    @Override
    public CommandResult execute(CommandSource source, CommandContext context, boolean hasAdminMode) throws CommandException
    {
        final Player player = (Player) source;
        final Optional<Faction> optionalFaction = context.getOne("faction");
        final Faction faction = optionalFaction.orElse(super.getPlugin().getFactionLogic().getFactionByPlayerUUID(player.getUniqueId())
                        .orElseThrow(() -> new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_MUST_BE_IN_FACTION_IN_ORDER_TO_USE_THIS_COMMAND))));

        //Even admins should not be able to disband SafeZone nor WarZone
        if(faction.isSafeZone() || faction.isWarZone())
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.THIS_FACTION_CANNOT_BE_DISBANDED));

        if (isLeader(player, faction))
        {
            runDisbandEventAndDisband(player, faction, false);
        }
        else
        {
            if (!hasAdminMode)
                throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_NEED_TO_TOGGLE_FACTION_ADMIN_MODE_TO_DO_THIS));
            runDisbandEventAndDisband(player, faction, true);
        }
        return CommandResult.success();
    }

    private boolean isLeader(final Player player, final Faction faction)
    {
        return player.getUniqueId().equals(faction.getLeader());
    }

    private void runDisbandEventAndDisband(final Player player, final Faction faction, final boolean forceRemovedByAdmin)
    {
        final boolean isCancelled = EventRunner.runFactionDisbandEventPre(player, faction, forceRemovedByAdmin, false);
        if(!isCancelled)
            playerDisband(player, faction, forceRemovedByAdmin);
    }

    private void playerDisband(final Player player, final Faction faction, final boolean forceRemovedByAdmin)
    {
        super.getPlugin().getFactionLogic().disbandFaction(faction.getName());
        player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, TextColors.GREEN, Messages.FACTION_HAS_BEEN_DISBANDED));
        EagleFactionsPlugin.AUTO_CLAIM_LIST.remove(player.getUniqueId());
        EagleFactionsPlugin.CHAT_LIST.remove(player.getUniqueId());
        EventRunner.runFactionDisbandEventPost(player, faction, forceRemovedByAdmin, false);
    }
}
