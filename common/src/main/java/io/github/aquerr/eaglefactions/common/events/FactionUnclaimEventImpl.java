package io.github.aquerr.eaglefactions.common.events;

import com.flowpowered.math.vector.Vector3i;
import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.events.FactionClaimEvent;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.World;

public class FactionUnclaimEventImpl extends FactionAbstractEvent implements FactionClaimEvent.Unclaim
{
    private final World world;
    private final Vector3i chunkPosition;

    FactionUnclaimEventImpl(final Player creator, final Faction faction, final World world, final Vector3i chunkPosition, final Cause cause)
    {
        super(creator, faction, cause);
        this.world = world;
        this.chunkPosition = chunkPosition;
    }

    public World getWorld()
    {
        return this.world;
    }

    public Vector3i getChunkPosition()
    {
        return this.chunkPosition;
    }

    static class Pre extends FactionUnclaimEventImpl implements FactionClaimEvent.Unclaim.Pre
    {
        Pre(Player creator, Faction faction, World world, Vector3i chunkPosition, Cause cause)
        {
            super(creator, faction, world, chunkPosition, cause);
        }
    }

    static class Post extends FactionUnclaimEventImpl implements FactionClaimEvent.Unclaim.Post
    {
        Post(Player creator, Faction faction, World world, Vector3i chunkPosition, Cause cause)
        {
            super(creator, faction, world, chunkPosition, cause);
        }
    }
}
