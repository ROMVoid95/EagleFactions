package io.github.aquerr.eaglefactions.common.entities;

import io.github.aquerr.eaglefactions.api.entities.Faction;
import io.github.aquerr.eaglefactions.api.managers.PermsManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FactionImplTest
{
    private Faction faction;

    @BeforeEach
    void prepareFactionObject()
    {
        faction = FactionImpl.builder("Test", Text.of(TextColors.BLUE, "TE"), UUID.randomUUID())
                .build();
    }

    @Test
    void builderShouldSetFactionName()
    {
        Assertions.assertEquals("Test", faction.getName());
    }

    @Test
    void builderShouldSetFactionTag()
    {
        Assertions.assertEquals(Text.of(TextColors.BLUE, "TE"), faction.getTag());
    }

//    @Test
//    void builderBuildShouldReturnNewFactionInstance()
//    {
//        final Faction newFaction = faction.toBuilder().build();
//        Assertions.assertNotSame(faction, newFaction);
//    }

    @Test
    void builderShouldSetDefaultFlagsIfNotSpecified()
    {
        Assertions.assertEquals(PermsManager.getDefaultFactionPerms(), faction.getPerms());
    }

    @Test
    void buildingAFactionWithoutNameShouldThrowException()
    {
        //given
        //when
        //then
        assertThrows(IllegalStateException.class, () -> FactionImpl.builder(null, Text.EMPTY, UUID.randomUUID()).build());
    }

    @Test
    void buildingAFactionWithoutTagShouldThrowException()
    {
        //given
        //when
        //then
        assertThrows(IllegalStateException.class, () -> FactionImpl.builder("", null, UUID.randomUUID()).build());
    }

    @Test
    void buildingAFactionWithoutLeaderShouldThrowException()
    {
        //given
        //when
        //then
        assertThrows(IllegalStateException.class, () -> FactionImpl.builder("", Text.EMPTY, null).build());
    }
}