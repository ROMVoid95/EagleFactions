package io.github.aquerr.eaglefactions.common.commands;

import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.common.PluginInfo;
import io.github.aquerr.eaglefactions.common.PluginPermissions;
import io.github.aquerr.eaglefactions.common.messaging.Messages;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.net.MalformedURLException;
import java.net.URL;

@EagleFactionsCommand(
        permission = PluginPermissions.VERSION_COMMAND,
        canBeUsedFromConsole = true,
        mustBeInFaction = false,
        minimumRank = FactionMemberType.NONE
)
public class VersionCommand extends CommandBase
{
    public VersionCommand(final EagleFactions plugin)
    {
        super(plugin);
    }

    @Override
    protected CommandElement[] getDefinedCommandArgs()
    {
        return new CommandElement[0];
    }

    @Override
    protected Text getDescription()
    {
        return Text.of(Messages.COMMAND_VERSION_DESC);
    }

    @Override
    protected CommandResult execute(CommandSource source, CommandContext context, boolean hasAdminMode) throws CommandException
    {
        try
        {
            source.sendMessage(Text.of(
                    TextActions.showText(Text.of(TextColors.BLUE, "Click to view Github")),
                    TextActions.openUrl(new URL(PluginInfo.URL)),
                    PluginInfo.PLUGIN_PREFIX, TextColors.AQUA, PluginInfo.NAME, TextColors.WHITE, " - ", TextColors.GOLD, Messages.VERSION + " ", PluginInfo.VERSION, TextColors.WHITE, " made by ", TextColors.GOLD, PluginInfo.AUTHOR));
        }
        catch(final MalformedURLException e)
        {
            e.printStackTrace();
        }
        return CommandResult.success();
    }
}
