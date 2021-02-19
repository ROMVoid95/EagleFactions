package io.github.aquerr.eaglefactions.common.commands;

import com.google.common.collect.ImmutableList;
import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.common.PluginInfo;
import io.github.aquerr.eaglefactions.common.messaging.Messages;
import org.spongepowered.api.command.*;
import org.spongepowered.api.command.args.*;
import org.spongepowered.api.command.args.parsing.InputTokenizer;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spongepowered.api.util.SpongeApiTranslationHelper.t;

public abstract class CommandBase implements CommandCallable
{
    private static final InputTokenizer argumentParser = InputTokenizer.quotedStrings(false);
    private final EagleFactions plugin;
    private final CommandElement commandElements;

    public CommandBase(final EagleFactions plugin)
    {
        this.plugin = plugin;
        this.commandElements = GenericArguments.seq(getDefinedCommandArgs());
    }

    protected abstract CommandElement[] getDefinedCommandArgs();

    protected abstract Text getDescription();

    protected abstract CommandResult execute(CommandSource source, CommandContext context, boolean hasAdminMode) throws CommandException;

    public EagleFactions getPlugin()
    {
        return this.plugin;
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) throws CommandException
    {
        CommandArgs args = new CommandArgs(arguments, argumentParser.tokenize(arguments, true));
        CommandContext ctx = new CommandContext();
        if (targetPosition != null) {
            ctx.putArg(CommandContext.TARGET_BLOCK_ARG, targetPosition);
        }
        ctx.putArg(CommandContext.TAB_COMPLETION, true);
        return complete(source, args, ctx);
    }

    public List<String> complete(CommandSource source, CommandArgs args, CommandContext context) {
        checkNotNull(source, "source");
        List<String> ret = this.commandElements.complete(source, args, context);
        return ImmutableList.copyOf(ret);
    }

    @Override
    public boolean testPermission(CommandSource source)
    {
        String requiredPermission = getPermission();
        return requiredPermission == null || source.hasPermission(requiredPermission);
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource source)
    {
        return Optional.of(getDescription());
    }

    @Override
    public Optional<Text> getHelp(CommandSource source)
    {
        checkNotNull(source, "source");
        Text.Builder builder = Text.builder();
        this.getShortDescription(source).ifPresent((a) -> builder.append(a, Text.NEW_LINE));
        builder.append(getUsage(source));
        return Optional.of(builder.build());
    }

    @Override
    public Text getUsage(CommandSource source)
    {
        checkNotNull(source, "source");
        return this.commandElements.getUsage(source);
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException
    {
        final EagleFactionsCommand eagleFactionsCommand = this.getClass().getAnnotation(EagleFactionsCommand.class);

        if(eagleFactionsCommand == null)
            throw new IllegalStateException("Command class is not annotated with EagleFactionsCommand annotation! Class: " + this.getClass());

        checkPermission(source);
        final CommandArgs args = new CommandArgs(arguments, argumentParser.tokenize(arguments, false));
        final CommandContext context = new CommandContext();
        populateContext(source, args, context);

        final boolean hasAdminMode = source instanceof User && this.plugin.getPlayerManager().hasAdminMode((User) source);
        checkEagleFactionsAccess(eagleFactionsCommand, source, arguments);

        return execute(source, context, hasAdminMode);
    }

    private void populateContext(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException
    {
        this.commandElements.parse(source, args, context);
        if (args.hasNext()) {
            args.next();
            throw args.createError(t("Too many arguments!"));
        }
    }

    private void checkPermission(CommandSource source) throws CommandException {
        checkNotNull(source, "source");
        if (!testPermission(source)) {
            throw new CommandPermissionException();
        }
    }

    private void checkEagleFactionsAccess(EagleFactionsCommand eagleFactionsCommand, CommandSource source, String arguments) throws CommandException
    {
        final boolean canBeUsedFromConsole = eagleFactionsCommand.canBeUsedFromConsole();
        final boolean mustBeInFaction = eagleFactionsCommand.mustBeInFaction();
        final FactionMemberType factionMemberType = eagleFactionsCommand.minimumRank();

        if (canBeUsedFromConsole && !(source instanceof Player))
            return;

        if (!(source instanceof Player))
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.DARK_RED, Messages.ONLY_IN_GAME_PLAYERS_CAN_USE_THIS_COMMAND));

        final Player player = (Player)source;
        final Optional<Faction> optionalFaction = this.plugin.getFactionLogic().getFactionByPlayerUUID(player.getUniqueId());

        if (!mustBeInFaction)
            return;

        if (!optionalFaction.isPresent())
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.DARK_RED, Messages.YOU_DONT_HAVE_ACCESS_TO_DO_THIS));

        final Faction faction = optionalFaction.get();
        final FactionMemberType playerMemberType = faction.getPlayerMemberType(player.getUniqueId());

        //Check if player has access for this command.
        if (!canUseCommand(factionMemberType, playerMemberType))
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.DARK_RED, Messages.YOU_DONT_HAVE_ACCESS_TO_DO_THIS));
    }

    private boolean canUseCommand(final FactionMemberType requiredRank, final FactionMemberType playerRank)
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
            case NONE:
                return true;
            default:
                return false;
        }
    }

    private String getPermission()
    {
        final EagleFactionsCommand eagleFactionsCommand = this.getClass().getAnnotation(EagleFactionsCommand.class);
        return eagleFactionsCommand.permission();
    }
}
