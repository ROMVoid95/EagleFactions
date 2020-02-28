package io.github.aquerr.eaglefactions.common.commands;

import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.common.PluginInfo;
import io.github.aquerr.eaglefactions.common.messaging.Messages;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;

public abstract class AbstractCommand implements CommandExecutor
{
    private EagleFactions plugin;

    public AbstractCommand(final EagleFactions plugin)
    {
        this.plugin = plugin;
    }

    public EagleFactions getPlugin()
    {
        return plugin;
    }

    @Override
    public abstract CommandResult execute(final CommandSource source, final CommandContext context) throws CommandException;

//    @Override
//    public CommandResult execute(final CommandSource source, final CommandContext context) throws CommandException
//    {
//        if (!(source instanceof Player))
//            return CommandResult.success();
//
//        final Player player = (Player)source;
//        final Class<? extends AbstractCommand> clazz = this.getClass();
//        final EagleFactionsCommand annotation = clazz.getAnnotation(EagleFactionsCommand.class);
//
//        if (annotation == null)
//            return CommandResult.success();
//
//        if (annotation.requireAdminMode() && !this.plugin.getPlayerManager().hasAdminMode(player))
//            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.DARK_RED, Messages.YOU_DONT_HAVE_ACCESS_TO_DO_THIS));
//
//        if (annotation.requiredRank() == FactionMemberType.NONE)
//            return CommandResult.success();
//
//        final Optional<Faction> optionalFaction = this.plugin.getFactionLogic().getFactionByPlayerUUID(player.getUniqueId());
//        if (!optionalFaction.isPresent())
//            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.DARK_RED, "You don't have access to use this command!"));
//
//        final Faction faction = optionalFaction.get();
//        final FactionMemberType playerMemberType = faction.getPlayerMemberType(player.getUniqueId());
//
//        if (!canUseCommand(annotation.requiredRank(), playerMemberType))
//            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.DARK_RED, "You don't have access to use this command!"));
//        else return CommandResult.success();
//    }
//
//    private boolean canUseCommand(final FactionMemberType requiredRank, final FactionMemberType playerRank)
//    {
//        switch (requiredRank)
//        {
//            case LEADER:
//                return playerRank == FactionMemberType.LEADER;
//            case OFFICER:
//                return playerRank == FactionMemberType.LEADER || playerRank == FactionMemberType.OFFICER;
//            case MEMBER:
//                return playerRank == FactionMemberType.LEADER || playerRank == FactionMemberType.OFFICER || playerRank == FactionMemberType.MEMBER;
//            case RECRUIT:
//                return playerRank == FactionMemberType.LEADER || playerRank == FactionMemberType.OFFICER || playerRank == FactionMemberType.MEMBER || playerRank == FactionMemberType.RECRUIT;
//            default:
//                return false;
//        }
//    }
}
