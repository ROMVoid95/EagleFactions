package io.github.aquerr.eaglefactions.logic;

import io.github.aquerr.eaglefactions.config.ConfigAccess;
import io.github.aquerr.eaglefactions.config.IConfig;
import io.github.aquerr.eaglefactions.config.MainConfig;
import ninja.leaping.configurate.ConfigurationNode;

public class MainLogic
{
    private static IConfig mainConfig = MainConfig.getConfig();

    public static boolean getAllianceFriendlyFire()
    {
        ConfigurationNode friendlyFireNode = ConfigAccess.getConfig(mainConfig).getNode("eaglefactions", "friendlyFire", "alliance");

        Boolean friendlyFire = friendlyFireNode.getBoolean();

        return friendlyFire;
    }
}