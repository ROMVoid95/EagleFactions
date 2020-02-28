package io.github.aquerr.eaglefactions.common.managers;

import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.common.EagleFactionsPlugin;
import io.github.aquerr.eaglefactions.common.PluginInfo;
import io.github.aquerr.eaglefactions.common.commands.AbstractCommand;
import io.github.aquerr.eaglefactions.common.commands.EagleFactionsCommand;
import io.github.aquerr.eaglefactions.common.messaging.Messages;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;

public class CommandManager
{
    private static final EagleFactions plugin = EagleFactionsPlugin.getPlugin();

    public static boolean testPermission(final AbstractCommand command, final CommandSource source) throws CommandException
    {
        if (!(source instanceof Player))
            return true;

        final Player player = (Player)source;
        final Class<? extends AbstractCommand> clazz = command.getClass();
        final EagleFactionsCommand annotation = clazz.getAnnotation(EagleFactionsCommand.class);

        if (annotation == null)
            return true;

        if (annotation.requireAdminMode() && !plugin.getPlayerManager().hasAdminMode(player))
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.DARK_RED, Messages.YOU_DONT_HAVE_ACCESS_TO_DO_THIS));

        if (annotation.requiredRank() == FactionMemberType.NONE)
            return true;

        final Optional<Faction> optionalFaction = plugin.getFactionLogic().getFactionByPlayerUUID(player.getUniqueId());
        if (!optionalFaction.isPresent())
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.DARK_RED, Messages.YOU_MUST_BE_IN_FACTION_IN_ORDER_TO_USE_THIS_COMMAND));

        final Faction faction = optionalFaction.get();
        final FactionMemberType playerMemberType = faction.getPlayerMemberType(player.getUniqueId());

        if (!canUseCommand(annotation.requiredRank(), playerMemberType))
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.DARK_RED, "You don't have required rank to use this command!"));
        else return true;
    }

    private static boolean canUseCommand(final FactionMemberType requiredRank, final FactionMemberType playerRank)
    {
        switch (requiredRank)
        {
            case LEADER:
                return playerRank == FactionMemberType.LEADER;
            case OFFICER:
                return playerRank == FactionMemberType.LEADER || playerRank == FactionMemberType.OFFICER;
            case MEMBER:
                return playerRank == FactionMemberType.LEADER || playerRank == FactionMemberType.OFFICER || playerRank == FactionMemberType.MEMBER;
            case RECRUIT:
                return playerRank == FactionMemberType.LEADER || playerRank == FactionMemberType.OFFICER || playerRank == FactionMemberType.MEMBER || playerRank == FactionMemberType.RECRUIT;
            default:
                return false;
        }
    }
}
