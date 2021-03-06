package io.github.aquerr.eaglefactions.common.commands.general;

import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.common.commands.AbstractCommand;
import io.github.aquerr.eaglefactions.common.messaging.Messages;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ListCommand extends AbstractCommand
{
    public ListCommand(final EagleFactions plugin)
    {
        super(plugin);
    }

    @Override
    public CommandResult execute(final CommandSource source, final CommandContext context) throws CommandException
    {
        CompletableFuture.runAsync(() ->{
            Collection<Faction> factionsList = super.getPlugin().getFactionLogic().getFactions().values();
            List<Text> helpList = new ArrayList<>();

            Text tagPrefix = getPlugin().getConfiguration().getChatConfig().getFactionStartPrefix();
            Text tagSuffix = getPlugin().getConfiguration().getChatConfig().getFactionEndPrefix();

            for(final Faction faction : factionsList)
            {
                Text tag = Text.builder().append(tagPrefix).append(faction.getTag()).append(tagSuffix, Text.of(" ")).build();

                Text factionHelp = Text.builder()
                        .append(Text.builder()
                                .append(Text.of(TextColors.AQUA, "- ")).append(tag).append(Text.of(faction.getName(), " (", getPlugin().getPowerManager().getFactionPower(faction), "/", getPlugin().getPowerManager().getFactionMaxPower(faction), ")"))
                                .build())
                        .onClick(TextActions.runCommand("/f info " + faction.getName()))
                        .onHover(TextActions.showText(Text.of(TextStyles.ITALIC, TextColors.BLUE, "Click", TextColors.RESET, " for more info...")))
                        .build();

                helpList.add(factionHelp);
            }

            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            PaginationList.Builder paginationBuilder = paginationService.builder().title(Text.of(TextColors.GREEN, Messages.FACTIONS_LIST)).padding(Text.of("-")).contents(helpList);
            paginationBuilder.sendTo(source);
        });
        return CommandResult.success();
    }
}
