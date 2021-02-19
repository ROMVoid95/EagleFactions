package io.github.aquerr.eaglefactions.common.commands.general;

import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.api.entities.FactionPlayer;
import io.github.aquerr.eaglefactions.common.PluginInfo;
import io.github.aquerr.eaglefactions.common.PluginPermissions;
import io.github.aquerr.eaglefactions.common.commands.CommandBase;
import io.github.aquerr.eaglefactions.common.commands.EagleFactionsCommand;
import io.github.aquerr.eaglefactions.common.commands.args.FactionArgument;
import io.github.aquerr.eaglefactions.common.commands.args.FactionPlayerArgument;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

@EagleFactionsCommand(
        permission = PluginPermissions.SET_FACTION_COMMAND,
        canBeUsedFromConsole = true,
        mustBeInFaction = false,
        minimumRank = FactionMemberType.RECRUIT
)
public class SetFactionCommand extends CommandBase
{
    public SetFactionCommand(EagleFactions plugin)
    {
        super(plugin);
    }

    @Override
    public CommandResult execute(CommandSource source, CommandContext context, boolean hasAdminMode) throws CommandException
    {
        final FactionPlayer factionPlayer = context.requireOne(Text.of("factionPlayer"));
        final Faction faction = context.requireOne(Text.of("faction"));
        final FactionMemberType factionMemberType = context.requireOne(Text.of("rank"));

        if (factionMemberType == FactionMemberType.LEADER || factionMemberType == FactionMemberType.NONE)
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, "You can't use the specified rank!"));

        super.getPlugin().getFactionLogic().setFaction(factionPlayer.getUniqueId(), faction.getName(), factionMemberType);
        source.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX.concat(Text.of("Player " + factionPlayer.getName() + " has been assigned to " + faction.getName()))));
        return CommandResult.success();
    }

    @Override
    public CommandElement[] getDefinedCommandArgs()
    {
        return new CommandElement[]
        {
                GenericArguments.onlyOne(new FactionPlayerArgument(super.getPlugin(), Text.of("factionPlayer"))),
                GenericArguments.onlyOne(new FactionArgument(super.getPlugin(), Text.of("faction"))),
                GenericArguments.onlyOne(GenericArguments.enumValue(Text.of("rank"), FactionMemberType.class))
        };
    }

    @Override
    public Text getDescription()
    {
        return Text.of("Force set player's faction");
    }
}
