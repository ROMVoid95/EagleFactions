package io.github.aquerr.eaglefactions.common.commands.relation;

import com.google.common.collect.ImmutableMap;
import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.entities.AllyRequest;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.entities.FactionMemberType;
import io.github.aquerr.eaglefactions.common.EagleFactionsPlugin;
import io.github.aquerr.eaglefactions.common.PluginInfo;
import io.github.aquerr.eaglefactions.common.PluginPermissions;
import io.github.aquerr.eaglefactions.common.commands.AbstractCommand;
import io.github.aquerr.eaglefactions.common.commands.CommandBase;
import io.github.aquerr.eaglefactions.common.commands.EagleFactionsCommand;
import io.github.aquerr.eaglefactions.common.commands.args.FactionArgument;
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

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@EagleFactionsCommand(
		permission = PluginPermissions.ALLY_COMMAND,
		canBeUsedFromConsole = false,
		mustBeInFaction = true,
		minimumRank = FactionMemberType.OFFICER
)
public class AllyCommand extends CommandBase
{
    public AllyCommand(final EagleFactions plugin)
    {
        super(plugin);
    }

	@Override
	protected CommandElement[] getDefinedCommandArgs()
	{
		return new CommandElement[] {
				GenericArguments.onlyOne(new FactionArgument(super.getPlugin(), Text.of("faction")))
		};
	}

	@Override
	protected Text getDescription()
	{
		return Text.of(Messages.COMMAND_ALLY_DESC);
	}

	@Override
    public CommandResult execute(final CommandSource source, final CommandContext context, boolean hasAdminMode) throws CommandException
    {
        final Faction selectedFaction = context.requireOne(Text.of("faction"));
        final Player player = (Player) source;
        final Optional<Faction> optionalPlayerFaction = getPlugin().getFactionLogic().getFactionByPlayerUUID(player.getUniqueId());
        final Faction playerFaction = optionalPlayerFaction.get();

        if(playerFaction.getName().equals(selectedFaction.getName()))
        	throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_CANNOT_INVITE_YOURSELF_TO_THE_ALLIANCE));

        if(super.getPlugin().getPlayerManager().hasAdminMode(player))
        {
            if(playerFaction.getEnemies().contains(selectedFaction.getName()))
                throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_ARE_IN_WAR_WITH_THIS_FACTION + " " + Messages.SEND_THIS_FACTION_A_PEACE_REQUEST_FIRST_BEFORE_INVITING_THEM_TO_ALLIES));

            if(playerFaction.getTruces().contains(selectedFaction.getName()))
			{
				super.getPlugin().getFactionLogic().removeTruce(playerFaction.getName(), selectedFaction.getName());
				//Add ally
				super.getPlugin().getFactionLogic().addAlly(playerFaction.getName(), selectedFaction.getName());
				player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, TextColors.GREEN, Messages.FACTION_HAS_BEEN_ADDED_TO_THE_ALLIANCE));
			}

			if(playerFaction.getAlliances().contains(selectedFaction.getName()))
			{
				//Remove ally
				super.getPlugin().getFactionLogic().removeAlly(playerFaction.getName(), selectedFaction.getName());
				player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.YOU_DISBANDED_YOUR_ALLIANCE_WITH_FACTION, TextColors.GREEN, Collections.singletonMap(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, selectedFaction.getName())))));
			}
			else
			{
				//Add ally
				super.getPlugin().getFactionLogic().addAlly(playerFaction.getName(), selectedFaction.getName());
				player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, TextColors.GREEN, Messages.FACTION_HAS_BEEN_ADDED_TO_THE_ALLIANCE));
			}
			return CommandResult.success();
        }

        if(!playerFaction.getLeader().equals(player.getUniqueId()) && !playerFaction.getOfficers().contains(player.getUniqueId()))
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_MUST_BE_THE_FACTIONS_LEADER_OR_OFFICER_TO_DO_THIS));

        if(playerFaction.getEnemies().contains(selectedFaction.getName()))
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_ARE_IN_WAR_WITH_THIS_FACTION + " " + Messages.SEND_THIS_FACTION_A_PEACE_REQUEST_FIRST_BEFORE_INVITING_THEM_TO_ALLIES));

		if(playerFaction.getTruces().contains(selectedFaction.getName()))
			throw new CommandException(Text.of(PluginInfo.PLUGIN_PREFIX, TextColors.RED, Messages.DISBAND_TRUCE_FIRST_TO_INVITE_FACTION_TO_THE_ALLIANCE));

        if(playerFaction.getAlliances().contains(selectedFaction.getName()))
        {
            //Remove ally
            super.getPlugin().getFactionLogic().removeAlly(playerFaction.getName(), selectedFaction.getName());
            player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.YOU_DISBANDED_YOUR_ALLIANCE_WITH_FACTION, TextColors.GREEN, Collections.singletonMap(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, selectedFaction.getName())))));
        }
        else
        {
            AllyRequest checkInvite = new AllyRequest(selectedFaction.getName(), playerFaction.getName());

            if(EagleFactionsPlugin.ALLY_INVITE_LIST.contains(checkInvite))
            {
            	acceptInvite(player, playerFaction, selectedFaction);
                EagleFactionsPlugin.ALLY_INVITE_LIST.remove(checkInvite);
            }
            else if(!EagleFactionsPlugin.ALLY_INVITE_LIST.contains(checkInvite))
            {
				sendInvite(player, playerFaction, selectedFaction);
            }
        }
        return CommandResult.success();
    }

    private void acceptInvite(final Player player, final Faction playerFaction, final Faction senderFaction)
	{
		super.getPlugin().getFactionLogic().addAlly(playerFaction.getName(), senderFaction.getName());
		player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.YOU_HAVE_ACCEPTED_AN_INVITATION_FROM_FACTION, TextColors.GREEN, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, senderFaction.getName())))));

		final Optional<Player> optionalSenderFactionLeader = super.getPlugin().getPlayerManager().getPlayer(senderFaction.getLeader());
		optionalSenderFactionLeader.ifPresent(x-> optionalSenderFactionLeader.get().sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.FACTION_ACCEPTED_YOUR_INVITE_TO_THE_ALLIANCE, TextColors.GREEN, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, playerFaction.getName()))))));
		senderFaction.getOfficers().forEach(x-> super.getPlugin().getPlayerManager().getPlayer(x).ifPresent(y-> Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.FACTION_ACCEPTED_YOUR_INVITE_TO_THE_ALLIANCE, TextColors.GREEN, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, playerFaction.getName()))))));
	}

	private void sendInvite(final Player player, final Faction playerFaction, final Faction targetFaction) throws CommandException
	{
		final AllyRequest invite = new AllyRequest(playerFaction.getName(), targetFaction.getName());
		if(EagleFactionsPlugin.ALLY_INVITE_LIST.contains(invite))
			throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, Messages.YOU_HAVE_ALREADY_INVITED_THIS_FACTION_TO_THE_ALLIANCE));

		EagleFactionsPlugin.ALLY_INVITE_LIST.add(invite);

		final Optional<Player> optionalInvitedFactionLeader = super.getPlugin().getPlayerManager().getPlayer(targetFaction.getLeader());

		optionalInvitedFactionLeader.ifPresent(x-> optionalInvitedFactionLeader.get().sendMessage(getInviteGetMessage(playerFaction)));
		targetFaction.getOfficers().forEach(x-> super.getPlugin().getPlayerManager().getPlayer(x).ifPresent(y-> getInviteGetMessage(playerFaction)));

		player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.YOU_HAVE_INVITED_FACTION_TO_THE_ALLIANCE, TextColors.GREEN, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, targetFaction.getName())))));

		final Task.Builder taskBuilder = Sponge.getScheduler().createTaskBuilder();
		taskBuilder.execute(() -> EagleFactionsPlugin.ALLY_INVITE_LIST.remove(invite)).delay(2, TimeUnit.MINUTES).name("EagleFaction - Remove Invite").submit(super.getPlugin());
	}

	private Text getInviteGetMessage(final Faction senderFaction)
	{
		final Text clickHereText = Text.builder()
				.append(Text.of(TextColors.AQUA, "[", TextColors.GOLD, Messages.CLICK_HERE, TextColors.AQUA, "]"))
				.onClick(TextActions.runCommand("/f ally " + senderFaction.getName()))
				.onHover(TextActions.showText(Text.of(TextColors.GOLD, "/f ally " + senderFaction.getName()))).build();

		return Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.FACTION_HAS_SENT_YOU_AN_INVITE_TO_THE_ALLIANCE, TextColors.GREEN, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, senderFaction.getName()))),
                "\n", Messages.YOU_HAVE_TWO_MINUTES_TO_ACCEPT_IT,
                "\n", clickHereText, Messages.TO_ACCEPT_INVITATION_OR_TYPE, " ", TextColors.GOLD, "/f ally ", senderFaction.getName());
	}
}
