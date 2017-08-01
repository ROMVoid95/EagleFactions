package io.github.aquerr.eaglefactions.config;

import com.google.gson.JsonObject;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.aquerr.eaglefactions.EagleFactions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Aquerr on 2017-07-12.
 */
public class FactionsConfig implements Configurable
{
    private static FactionsConfig config = new FactionsConfig();

    private FactionsConfig()
    {

    }

    public static FactionsConfig getConfig()
    {
        return config;
    }

   private Path configFile = Paths.get(EagleFactions.getEagleFactions().getConfigDir().resolve("data") + "/factions.config");
   private ConfigurationLoader<CommentedConfigurationNode> configLoader = HoconConfigurationLoader.builder().setPath(configFile).build();
   private CommentedConfigurationNode configNode;

   // @Override
    public void setup()
    {
        if (!Files.exists(configFile))
        {
            try
            {
              Files.createFile(configFile);
              load();
              populate();
              save();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            load();
        }
    }

    @Override
    public void load()
    {
        try
        {
            configNode = configLoader.load();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void save()
    {
        try
        {
            configLoader.save(configNode);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void populate()
    {
        get().getNode("factions").setComment("This stores all the data of the factions.");
    }

    @Override
    public CommentedConfigurationNode get()
    {
        return configNode;
    }
}
