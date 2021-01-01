package me.shawlaf.varlight.spigot.api;

import me.shawlaf.varlight.exception.LightUpdateFailedException;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.persistence.WorldLightPersistence;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static me.shawlaf.varlight.spigot.util.IntPositionExtension.toIntPosition;

// TODO document
public class VarLightAPI {

    public static VarLightAPI getAPI() {
        Plugin varLightPlugin = Bukkit.getPluginManager().getPlugin("VarLight");

        if (varLightPlugin == null) {
            throw new IllegalStateException("VarLight not present");
        }

        return ((VarLightPlugin) varLightPlugin).getApi();
    }

    private final VarLightPlugin plugin;
    private final Map<UUID, WorldLightPersistence> persistenceManagers = new HashMap<>();

    public VarLightAPI(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    public WorldLightPersistence requireVarLightEnabled(@NotNull World world) throws VarLightNotActiveException {
        requireNonNull(world, "World may not be null");

        WorldLightPersistence wlp = persistenceManagers.get(world.getUID());

        if (wlp == null) {
            if (plugin.getVarLightConfig().getVarLightEnabledWorldNames().contains(world.getName())) {
                wlp = new WorldLightPersistence(world, plugin);
                persistenceManagers.put(world.getUID(), wlp);
            } else {
                throw new VarLightNotActiveException(world);
            }
        }

        return wlp;
    }

    public int getCustomLuminance(@NotNull Location location) {
        requireNonNull(location, "Location may not be null");
        requireNonNull(location.getWorld(), "Location must have an associated world");

        try {
            return requireVarLightEnabled(location.getWorld()).getCustomLuminance(toIntPosition(location), 0);
        } catch (VarLightNotActiveException exception) {
            return 0;
        }
    }

    public void setCustomLuminance(@Nullable CommandSender source, @NotNull Location location, int customLuminance) {
        setCustomLuminance(location, customLuminance).thenAccept((result) -> {
            if (source != null) {
                result.displayMessage(source);
            }
        });
    }

    @NotNull
    public CompletableFuture<LightUpdateResult> setCustomLuminance(@NotNull Location location, int customLuminance) {
        requireNonNull(location, "Location may not be null");
        requireNonNull(location.getWorld(), "Location must have an associated world");

        int fromLight = location.getBlock().getLightFromBlocks();

        if (customLuminance < 0) {
            return completedFuture(LightUpdateResult.zeroReached(fromLight, customLuminance));
        }

        if (customLuminance > 15) {
            return completedFuture(LightUpdateResult.fifteenReached(fromLight, customLuminance));
        }

        if (plugin.getNmsAdapter().isIllegalBlock(location.getBlock())) {
            return completedFuture(LightUpdateResult.invalidBlock(fromLight, customLuminance));
        }

        IntPosition position = toIntPosition(location);
        World world = location.getWorld();

        WorldLightPersistence wlp;

        try {
            wlp = requireVarLightEnabled(world);
        } catch (VarLightNotActiveException e) {
            return completedFuture(LightUpdateResult.notActive(fromLight, customLuminance, e));
        }

        int finalFromLight = wlp.getCustomLuminance(position, 0);

        // TODO call Light update event

        wlp.setCustomLuminance(position, customLuminance);

        return CompletableFuture.supplyAsync(() -> { // TODO use Bukkit executor
            try {
                plugin.getLightUpdater().updateLightServer(world, position).join();
                plugin.getLightUpdater().updateLightClient(world, position.toChunkCoords());

                return LightUpdateResult.updated(finalFromLight, customLuminance);
            } catch (VarLightNotActiveException exception) {
                throw new LightUpdateFailedException(exception);
            }
        });
    }
}