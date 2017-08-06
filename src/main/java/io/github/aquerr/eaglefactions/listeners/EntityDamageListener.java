package io.github.aquerr.eaglefactions.listeners;

import io.github.aquerr.eaglefactions.EagleFactions;
import io.github.aquerr.eaglefactions.logic.FactionLogic;
import io.github.aquerr.eaglefactions.logic.MainLogic;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;

public class EntityDamageListener
{
    @Listener
    public void onEntityDamage(DamageEntityEvent event)
    {
        EagleFactions.getEagleFactions().getLogger().info("event.getcause().root()");
        EagleFactions.getEagleFactions().getLogger().info(event.getCause().root().toString());

        if(event.getCause().root() instanceof EntityDamageSource)
        {
            EntityDamageSource source = (EntityDamageSource)event.getCause().root();

             if(source.getSource() instanceof Player)
             {
                 Player player = (Player) source.getSource();

                 player.sendMessage(Text.of("YOU ATTACKED SOMEONE!"));

                 EagleFactions.getEagleFactions().getLogger().info("Player attacked someone!");

                 if(event.getTargetEntity().getType() == EntityTypes.PLAYER)
                 {
                     Player attackedPlayer = (Player) event.getTargetEntity();

                     EagleFactions.getEagleFactions().getLogger().info("Checking if players are in the same faction...");

                     if(FactionLogic.getFactionName(player.getUniqueId()) == FactionLogic.getFactionName(attackedPlayer.getUniqueId()))
                     {
                         if(!FactionLogic.getFactionFriendlyFire(FactionLogic.getFactionName(player.getUniqueId())))
                         {
                             event.setBaseDamage(0);
                             event.setCancelled(true);
                         }
                         else return;
                     }
                     else if(FactionLogic.getAlliances(FactionLogic.getFactionName(player.getUniqueId())).contains(FactionLogic.getFactionName(attackedPlayer.getUniqueId())) && !MainLogic.getAllianceFriendlyFire())
                     {
                         event.setBaseDamage(0);
                         event.setCancelled(true);
                     }
                     else return;

                 }
             }
        }
        return;
    }
}
