package io.github.aquerr.eaglefactions.common.commands.relation;

import com.google.common.collect.ImmutableMap;
import io.github.aquerr.eaglefactions.api.EagleFactions;
import io.github.aquerr.eaglefactions.api.entities.ArmisticeRequest;
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
        permission = PluginPermissions.ENEMY_COMMAND,
        canBeUsedFromConsole = false,
        mustBeInFaction = true,
        minimumRank = FactionMemberType.OFFICER
)
public class EnemyCommand extends CommandBase
{
    public EnemyCommand(final EagleFactions plugin)
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
        return Text.of(Messages.COMMAND_ENEMY_DESC);
    }

    @Override
    public CommandResult execute(final CommandSource source, final CommandContext context, boolean hasAdminMode) throws CommandException
    {
        final Faction enemyFaction = context.requireOne(Text.of("faction"));
        final Player player = (Player) source;
        final Optional<Faction> optionalPlayerFaction = super.getPlugin().getFactionLogic().getFactionByPlayerUUID(player.getUniqueId());
        final Faction playerFaction = optionalPlayerFaction.get();
        if(playerFaction.getName().equals(enemyFaction.getName()))
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.YOU_CANNOT_BE_IN_WAR_WITH_YOURSELF));

        if(hasAdminMode)
        {
            if(playerFaction.getAlliances().contains(enemyFaction.getName()))
                throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.THIS_FACTION_IS_YOUR_ALLY + " " + Messages.DISBAND_ALLIANCE_FIRST_TO_DECLARE_A_WAR));
            if(playerFaction.getTruces().contains(enemyFaction.getName()))
                throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.THIS_FACTION_IS_IN_TRUCE_WITH_YOU + " " + Messages.DISBAND_TRUCE_FIRST_TO_DECLARE_A_WAR));

            if(!playerFaction.getEnemies().contains(enemyFaction.getName()))
            {
                super.getPlugin().getFactionLogic().addEnemy(playerFaction.getName(), enemyFaction.getName());
                player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, TextColors.GREEN, Messages.FACTION_HAS_BEEN_ADDED_TO_THE_ENEMIES));
            }
            else
            {
                super.getPlugin().getFactionLogic().removeEnemy(playerFaction.getName(), enemyFaction.getName());
                player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.YOU_REMOVED_WAR_STATE_WITH_FACTION, TextColors.GREEN, Collections.singletonMap(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, enemyFaction.getName())))));
            }
            return CommandResult.success();
        }

        if(playerFaction.getAlliances().contains(enemyFaction.getName()) || playerFaction.getTruces().contains(enemyFaction.getName()))
            throw new CommandException(Text.of(PluginInfo.ERROR_PREFIX, TextColors.RED, Messages.THIS_FACTION_IS_YOUR_ALLY + " " + Messages.DISBAND_ALLIANCE_FIRST_TO_DECLARE_A_WAR));

        if(!playerFaction.getEnemies().contains(enemyFaction.getName()))
        {
            super.getPlugin().getFactionLogic().addEnemy(playerFaction.getName(), enemyFaction.getName());
            player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.YOUR_FACTION_IS_NOW_ENEMIES_WITH_FACTION, TextColors.RESET, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, enemyFaction.getName())))));

            //Send message to enemy leader.
            super.getPlugin().getPlayerManager().getPlayer(enemyFaction.getLeader()).ifPresent(x->x.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.FACTION_HAS_HAS_DECLARED_YOU_A_WAR, TextColors.RED, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, playerFaction.getName()))))));

            //Send message to enemy officers.
            enemyFaction.getOfficers().forEach(x-> super.getPlugin().getPlayerManager().getPlayer(x).ifPresent(y-> y.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.FACTION_HAS_HAS_DECLARED_YOU_A_WAR, TextColors.RED, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, playerFaction.getName())))))));
            return CommandResult.success();
        }
        else
        {
            final ArmisticeRequest checkRemove = new ArmisticeRequest(enemyFaction.getName(), playerFaction.getName());
            if(EagleFactionsPlugin.ARMISTICE_REQUEST_LIST.contains(checkRemove))
            {
                super.getPlugin().getFactionLogic().removeEnemy(enemyFaction.getName(), playerFaction.getName());
                player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.YOU_HAVE_ACCEPTED_ARMISTICE_REQUEST_FROM_FACTION, TextColors.GREEN, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, enemyFaction.getName())))));

                final Optional<Player> enemyFactionLeader = super.getPlugin().getPlayerManager().getPlayer(enemyFaction.getLeader());
                enemyFactionLeader.ifPresent(x->x.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.FACTION_ACCEPTED_YOUR_ARMISTICE_REQUEST, TextColors.GREEN, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, playerFaction.getName()))))));
                enemyFaction.getOfficers().forEach(x-> super.getPlugin().getPlayerManager().getPlayer(x).ifPresent(y->y.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.FACTION_ACCEPTED_YOUR_ARMISTICE_REQUEST, TextColors.GREEN, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, playerFaction.getName())))))));

                EagleFactionsPlugin.ARMISTICE_REQUEST_LIST.remove(checkRemove);
            }
            else if(!EagleFactionsPlugin.ARMISTICE_REQUEST_LIST.contains(checkRemove))
            {

                final ArmisticeRequest armisticeRequest = new ArmisticeRequest(playerFaction.getName(), enemyFaction.getName());
                if(EagleFactionsPlugin.ARMISTICE_REQUEST_LIST.contains(armisticeRequest))
                {
                    player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, TextColors.RED, Messages.YOU_HAVE_ALREADY_SENT_ARMISTICE_REQUEST));
                    return CommandResult.success();
                }
                EagleFactionsPlugin.ARMISTICE_REQUEST_LIST.add(armisticeRequest);

                final Optional<Player> enemyFactionLeader = super.getPlugin().getPlayerManager().getPlayer(enemyFaction.getLeader());
                enemyFactionLeader.ifPresent(x->x.sendMessage(getArmisticeRequestMessage(playerFaction)));
                enemyFaction.getOfficers().forEach(x-> super.getPlugin().getPlayerManager().getPlayer(x).ifPresent(y->y.sendMessage(getArmisticeRequestMessage(playerFaction))));
                player.sendMessage(Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.YOU_REQUESTED_ARMISTICE_WITH_FACTION, TextColors.GREEN, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, enemyFaction.getName())))));

                final Task.Builder taskBuilder = Sponge.getScheduler().createTaskBuilder();
                taskBuilder.execute(() -> EagleFactionsPlugin.ARMISTICE_REQUEST_LIST.remove(armisticeRequest)).delay(2, TimeUnit.MINUTES).name("EagleFaction - Remove Enemy").submit(super.getPlugin());
                return CommandResult.success();
            }
        }

        return CommandResult.success();
    }

    private Text getArmisticeRequestMessage(final Faction senderFaction)
    {
        final Text clickHereText = Text.builder()
                .append(Text.of(TextColors.AQUA, "[", TextColors.GOLD, Messages.CLICK_HERE, TextColors.AQUA, "]"))
                .onClick(TextActions.runCommand("/f enemy " + senderFaction.getName()))
                .onHover(TextActions.showText(Text.of(TextColors.GOLD, "/f enemy " + senderFaction.getName()))).build();

        return Text.of(PluginInfo.PLUGIN_PREFIX, MessageLoader.parseMessage(Messages.FACTION_HAS_SENT_YOU_AN_ARMISTICE_REQUEST, TextColors.YELLOW, ImmutableMap.of(Placeholders.FACTION_NAME, Text.of(TextColors.GOLD, senderFaction.getName(),
                "\n", Messages.YOU_HAVE_TWO_MINUTES_TO_ACCEPT_IT,
                "\n", clickHereText, Messages.TO_ACCEPT_IT_OR_TYPE, " ", TextColors.GOLD, "/f enemy " + senderFaction.getName()))));
    }
}
