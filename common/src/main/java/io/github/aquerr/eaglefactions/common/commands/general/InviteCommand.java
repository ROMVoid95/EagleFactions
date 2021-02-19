package io.github.aquerr.eaglefactions.common.commands.general;

import com.google.common.collect.ImmutableMap;
import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.config.FactionsConfig;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.api.entities.Invite;
import io.github.aquerr.eaglefactions.common.EagleFactionsPlugin;
import io.github.aquerr.eaglefactions.common.PluginInfo;
import io.github.aquerr.eaglefactions.common.PluginPermissions;
import io.github.aquerr.eaglefactions.common.commands.CommandBase;
import io.github.aquerr.eaglefactions.common.commands.EagleFactionsCommand;
import io.github.aquerr.eaglefactions.common.events.EventRunner;
import io.github.aquerr.eaglefactions.common.messaging.MessageLoader;
import io.github.aquerr.eaglefactions.common.messaging.Messages;
import io.github.aquerr.eaglefactions.common.messaging.Placeholders;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@EagleFactionsCommand(
        permission = PluginPermissions.INVITE_COMMAND,
        canBeUsedFromConsole = false,
        mustBeInFaction = true,
        minimumRank = FactionMemberType.RECRUIT
)
public class InviteCommand extends CommandBase
{
    private final FactionsConfig factionsConfig;

    public InviteCommand(final EagleFactions plugin)
    {
        super(plugin);
        this.factionsConfig = plugin.getConfiguration().getFactionsConfig();
    }

    @Override
    protected CommandElement[] getDefinedCommandArgs()
    {
        return new CommandElement[] {
                GenericArguments.onlyOne(GenericArguments.player(Text.of("player")))
        };
    }

    @Override
    protected Text getDescription()
    {
        return Text.of(Messages.COMMAND_INVITE_DESC);
    }

    @Override
    protected CommandResult execute(CommandSource source, CommandContext context, boolean hasAdminMode) throws CommandException
    {
        final Player invitedPlayer = context.requireOne("player");

        final Player senderPlayer = (Player)source;
        final Optional<Faction> optionalSenderFaction = getPlugin().getFactionLogic().getFactionByPlayerUUID(senderPlayer.getUniqueId());
        final Faction senderFaction = optionalSenderFaction.get();

        if (!super.getPlugin().getPermsManager().canInvite(senderPlayer.getUniqueId(), senderFaction))
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.PLAYERS_WITH_YOUR_RANK_CANT_INVITE_PLAYERS_TO_FACTION));

        if(this.factionsConfig.isPlayerLimit())
        {
            int playerCount = senderFaction.getPlayers().size();
            if(playerCount >= this.factionsConfig.getPlayerLimit())
            {
                throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_CANT_INVITE_MORE_PLAYERS_TO_YOUR_FACTION + " " + Messages.FACTIONS_PLAYER_LIMIT_HAS_BEEN_REACHED));
            }
        }

        if(super.getPlugin().getFactionLogic().getFactionByPlayerUUID(invitedPlayer.getUniqueId()).isPresent())
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.PLAYER_IS_ALREADY_IN_A_FACTION));

        final boolean isCancelled = EventRunner.runFactionInviteEventPre(senderPlayer, invitedPlayer, senderFaction);
        if (isCancelled)
            return CommandResult.success();

        final Invite invite = new Invite(senderFaction.getName(), invitedPlayer.getUniqueId());
        EagleFactionsPlugin.INVITE_LIST.add(invite);

        invitedPlayer.sendMessage(getInviteGetMessage(senderFaction));
        senderPlayer.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX,TextColors.GREEN, Messages.YOU_INVITED + " ", TextColors.GOLD, invitedPlayer.getName(), TextColors.GREEN, " " + Messages.TO_YOUR_FACTION));

        final Task.Builder taskBuilder = Sponge.getScheduler().createTaskBuilder();

        taskBuilder.execute(() -> EagleFactionsPlugin.INVITE_LIST.remove(invite)).delay(2, TimeUnit.MINUTES).name("EagleFaction - Remove Invite").submit(EagleFactionsPlugin.getPlugin());
        EventRunner.runFactionInviteEventPost(senderPlayer, invitedPlayer, senderFaction);
        return CommandResult.success();
    }

	private Text getInviteGetMessage(final Faction senderFaction)
    {
        final Text clickHereText = Text.builder()
                .append(Text.of(TextColors.AQUA, "[", TextColors.GOLD, Messages.CLICK_HERE, TextColors.AQUA, "]"))
                .onClick(TextActions.runCommand("/f join " + senderFaction.getName()))
                .onHover(TextActions.showText(Text.of(TextColors.GOLD, "/f join " + senderFaction.getName())))
                .build();

        return Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.FACTION_HAS_SENT_YOU_AN_INVITE, TextColors.GREEN, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, senderFaction.getName()))),
                Messages.YOU_HAVE_TWO_MINUTES_TO_ACCEPT_IT,
                "\n", clickHereText, TextColors.GREEN, " ", Messages.TO_ACCEPT_INVITATION_OR_TYPE, " ", TextColors.GOLD, "/f join " + senderFaction.getName());
    }
}
