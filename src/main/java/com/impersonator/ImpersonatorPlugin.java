package com.impersonator;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.PlayerChanged;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
        name = "Impersonator",
        description = "Locally replaces other players with configurable NPC appearances."
)
public class ImpersonatorPlugin extends Plugin
{
    private static final int NO_TRANSFORM = -1;
    private static final String CONFIG_GROUP = "impersonator-plugin";
    private static final String CONFIG_PREFIX = "player.";

    private final Map<Player, Integer> originalTransforms = new HashMap<>();
    private final Map<String, Integer> activeImpersonations = new HashMap<>();

    @Inject
    private Client client;

    @Inject
    private ImpersonatorConfig config;

    @Inject
    private ConfigManager configManager;

    @Provides
    ImpersonatorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ImpersonatorConfig.class);
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        if (!config.showImpersonate())
        {
            return;
        }

        Menu menu = client.getMenu();
        Set<Player> playersAdded = new HashSet<>();

        for (MenuEntry menuEntry : event.getMenuEntries())
        {
            Player player = menuEntry.getPlayer();

            if (player == null || player == client.getLocalPlayer() || !playersAdded.add(player))
            {
                continue;
            }

            String playerName = player.getName();

            if (playerName == null || playerName.isEmpty())
            {
                continue;
            }

            MenuEntry root = menu.createMenuEntry(-1)
                    .setOption("Impersonate")
                    .setTarget(playerName)
                    .setType(MenuAction.RUNELITE_PLAYER)
                    .setIdentifier(player.getId());

            Menu subMenu = root.createSubMenu();

            addImpersonationEntry(subMenu, player, config.npc1Id());
            addImpersonationEntry(subMenu, player, config.npc2Id());
            addImpersonationEntry(subMenu, player, config.npc3Id());

            if (config.showReset())
            {
                subMenu.createMenuEntry(-1)
                        .setOption("Reset")
                        .setTarget(playerName)
                        .setType(MenuAction.RUNELITE_PLAYER)
                        .setIdentifier(player.getId())
                        .onClick(entry -> resetImpersonation(player));
            }
        }
    }

    private void addImpersonationEntry(Menu menu, Player player, int npcId)
    {
        menu.createMenuEntry(-1)
                .setOption("into a " + getNpcName(npcId))

                .setType(MenuAction.RUNELITE_PLAYER)
                .setIdentifier(player.getId())
                .onClick(entry -> applyImpersonation(player, npcId));
    }

    private String getNpcName(int npcId)
    {
        NPCComposition npc = client.getNpcDefinition(npcId);
        return npc == null || npc.getName() == null ? "NPC " + npcId : npc.getName();
    }

    private void applyImpersonation(Player player, int npcId)
    {
        if (!isValidTarget(player))
        {
            return;
        }

        PlayerComposition composition = player.getPlayerComposition();
        if (composition == null)
        {
            return;
        }

        originalTransforms.putIfAbsent(player, composition.getTransformedNpcId());
        activeImpersonations.put(player.getName(), npcId);
        saveImpersonation(player.getName(), npcId);
        setImpersonation(composition, npcId);
    }

    private void resetImpersonation(Player player)
    {
        if (player == null || player.getName() == null || player.getName().isEmpty())
        {
            return;
        }

        String playerName = player.getName();
        activeImpersonations.remove(playerName);
        removeSavedImpersonation(playerName);

        PlayerComposition composition = player.getPlayerComposition();
        if (composition == null)
        {
            originalTransforms.remove(player);
            return;
        }

        Integer original = originalTransforms.remove(player);
        composition.setTransformedNpcId(original == null ? NO_TRANSFORM : original);
        composition.setHash();
    }

    private boolean isValidTarget(Player player)
    {
        return player != null
                && player != client.getLocalPlayer()
                && player.getName() != null
                && !player.getName().isEmpty();
    }

    private void setImpersonation(PlayerComposition composition, int npcId)
    {
        composition.setTransformedNpcId(npcId);
        composition.setHash();
    }

    @Subscribe
    public void onPlayerChanged(PlayerChanged event)
    {
        Player player = event.getPlayer();

        if (!isValidTarget(player))
        {
            return;
        }

        String playerName = player.getName();
        Integer npcId = activeImpersonations.get(playerName);

        if (npcId == null)
        {
            npcId = loadImpersonation(playerName);
            if (npcId != null)
            {
                activeImpersonations.put(playerName, npcId);
            }
        }

        if (npcId == null)
        {
            return;
        }

        PlayerComposition composition = player.getPlayerComposition();
        if (composition == null)
        {
            return;
        }

        originalTransforms.putIfAbsent(player, composition.getTransformedNpcId());

        if (composition.getTransformedNpcId() != npcId)
        {
            setImpersonation(composition, npcId);
        }
    }

    @Subscribe
    public void onPlayerDespawned(PlayerDespawned event)
    {
        originalTransforms.remove(event.getPlayer());
    }

    private void saveImpersonation(String playerName, int npcId)
    {
        configManager.setConfiguration(CONFIG_GROUP, CONFIG_PREFIX + playerName, npcId);
    }

    private Integer loadImpersonation(String playerName)
    {
        String value = configManager.getConfiguration(CONFIG_GROUP, CONFIG_PREFIX + playerName);

        if (value == null || value.isEmpty())
        {
            return null;
        }

        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private void removeSavedImpersonation(String playerName)
    {
        configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_PREFIX + playerName);
    }

    @Override
    protected void shutDown()
    {
        for (Map.Entry<Player, Integer> entry : originalTransforms.entrySet())
        {
            PlayerComposition composition = entry.getKey().getPlayerComposition();
            if (composition != null)
            {
                composition.setTransformedNpcId(entry.getValue());
                composition.setHash();
            }
        }

        originalTransforms.clear();
        activeImpersonations.clear();
    }
}
