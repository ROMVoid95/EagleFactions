package io.github.aquerr.eaglefactions.common.storage.task;

import io.github.aquerr.eaglefactions.common.entities.FactionPlayerState;

public class SavePlayerTask implements IStorageTask
{
    private final FactionPlayerState factionPlayerState;
    private final Runnable runnable;

    public SavePlayerTask(FactionPlayerState factionPlayerState, Runnable runnable)
    {
        this.factionPlayerState = factionPlayerState;
        this.runnable = runnable;
    }

    @Override
    public void run()
    {
        this.runnable.run();
    }
}
