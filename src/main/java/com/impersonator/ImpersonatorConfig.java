package com.impersonator;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("impersonator-plugin")
public interface ImpersonatorConfig extends Config
{
    @ConfigItem(
            keyName = "showImpersonate",
            name = "Show Impersonate option",
            description = "Adds the Impersonate option to the player right-click menu.",
            position = 0
    )
    default boolean showImpersonate()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showReset",
            name = "Show reset option",
            description = "Adds a reset option to the Impersonate submenu.",
            position = 1
    )
    default boolean showReset()
    {
        return true;
    }

    @ConfigItem(
            keyName = "npc1Id",
            name = "NPC 1 ID",
            description = "NPC ID used by the first impersonation option.",
            position = 2
    )
    default int npc1Id()
    {
        return 1158;
    }

    @ConfigItem(
            keyName = "npc2Id",
            name = "NPC 2 ID",
            description = "NPC ID used by the second impersonation option.",
            position = 3
    )
    default int npc2Id()
    {
        return 1160;
    }

    @ConfigItem(
            keyName = "npc3Id",
            name = "NPC 3 ID",
            description = "NPC ID used by the third impersonation option.",
            position = 4
    )
    default int npc3Id()
    {
        return 1157;
    }
}
