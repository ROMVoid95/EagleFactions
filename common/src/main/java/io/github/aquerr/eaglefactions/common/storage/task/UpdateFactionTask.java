package io.github.aquerr.eaglefactions.common.storage.task;

import io.github.aquerr.eaglefactions.common.entities.FactionState;

public class UpdateFactionTask implements IStorageTask
{
    private final FactionState factionState;
    private final Runnable runnable;

    public UpdateFactionTask(final FactionState factionState, final Runnable runnable)
    {
        this.factionState = factionState;
        this.runnable = runnable;
    }

    public FactionState getFactionState()
    {
        return this.factionState;
    }

    @Override
    public void run()
    {
        this.runnable.run();
    }
}
