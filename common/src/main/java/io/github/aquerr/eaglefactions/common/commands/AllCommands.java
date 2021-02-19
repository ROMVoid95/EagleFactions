package io.github.aquerr.eaglefactions.common.commands;

import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.common.EagleFactionsPlugin;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ChildCommandElementExecutor;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Map;

@EagleFactionsCommand(
        permission = "",
        canBeUsedFromConsole = true,
        mustBeInFaction = false,
        minimumRank = FactionMemberType.NONE
)
public class AllCommands extends CommandBase
{
    private ChildCommandElementExecutor commandExecutor;

    public AllCommands(EagleFactions plugin)
    {
        super(plugin);
    }

    @Override
    protected CommandElement[] getDefinedCommandArgs()
    {
        if (this.commandExecutor == null)
            this.commandExecutor = registerInDispatcher(new ChildCommandElementExecutor(null, null, false), EagleFactionsPlugin.SUBCOMMANDS);

        return new CommandElement[] {
                GenericArguments.seq(this.commandExecutor)
        };
    }

    @Override
    protected Text getDescription()
    {
        return null;
    }

    @Override
    protected CommandResult execute(CommandSource source, CommandContext context, boolean hasAdminMode) throws CommandException
    {
        return this.commandExecutor.execute(source, context);
    }

    private ChildCommandElementExecutor registerInDispatcher(ChildCommandElementExecutor childDispatcher, Map<List<String>, ? extends CommandCallable> childCommandsMap) {
        for (Map.Entry<List<String>, ? extends CommandCallable> spec : childCommandsMap.entrySet()) {
            childDispatcher.register(spec.getValue(), spec.getKey());
        }

        return childDispatcher;
    }
}
