package io.github.aquerr.eaglefactions.common.commands.general;

import com.google.common.collect.Lists;
import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.common.EagleFactionsPlugin;
import io.github.aquerr.eaglefactions.common.PluginPermissions;
import io.github.aquerr.eaglefactions.common.commands.CommandBase;
import io.github.aquerr.eaglefactions.common.commands.EagleFactionsCommand;
import io.github.aquerr.eaglefactions.common.messaging.Messages;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.List;
import java.util.Map;

@EagleFactionsCommand(
        permission = PluginPermissions.HELP_COMMAND,
        canBeUsedFromConsole = true,
        mustBeInFaction = false,
        minimumRank = FactionMemberType.NONE
)
public class HelpCommand extends CommandBase
{
    public HelpCommand(EagleFactions plugin)
    {
        super(plugin);
    }

    @Override
    protected CommandElement[] getDefinedCommandArgs()
    {
        return new CommandElement[] {
                GenericArguments.optional(GenericArguments.integer(Text.of("page")))
        };
    }

    @Override
    protected Text getDescription()
    {
        return Text.of(Messages.COMMAND_HELP_DESC);
    }

    @Override
    public CommandResult execute(CommandSource source, CommandContext context, boolean hasAdminMode) throws CommandException
    {
        final int pageNumber = context.<Integer>getOne(Text.of("page")).orElse(1);
        final Map<List<String>, CommandCallable> commands = EagleFactionsPlugin.SUBCOMMANDS;
        final List<Text> helpList = Lists.newArrayList();

        for (final List<String> aliases: commands.keySet())
        {
            CommandCallable commandSpec = commands.get(aliases);

            if(source instanceof Player)
            {
                Player player = (Player)source;

                if(!commandSpec.testPermission(player))
                {
                    continue;
                }
            }

            final Text commandHelp = Text.builder()
                    .append(Text.builder()
                            .append(Text.of(TextColors.AQUA, "/f " + aliases.toString().replace("[","").replace("]","")))
                            .build())
                    .append(Text.builder()
                            .append(Text.of(TextColors.WHITE, " - " + commandSpec.getShortDescription(source).get().toPlain() + "\n"))
                            .build())
                    .append(Text.builder()
                            .append(Text.of(TextColors.GRAY, Messages.USAGE + " /f " + aliases.toString().replace("[","").replace("]","") + " " + commandSpec.getUsage(source).toPlain()))
                            .build())
                    .build();
            helpList.add(commandHelp);
        }

        //Sort commands alphabetically.
        helpList.sort(Text::compareTo);

        PaginationList.Builder paginationBuilder = PaginationList.builder().title(Text.of(TextColors.GREEN, Messages.EAGLEFACTIONS_COMMAND_LIST)).padding(Text.of("-")).contents(helpList).linesPerPage(14);
        paginationBuilder.build().sendTo(source, pageNumber);
        return CommandResult.success();
    }
}