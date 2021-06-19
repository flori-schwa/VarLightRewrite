package me.shawlaf.varlight.spigot.nms.v1_16_R3;

import lombok.experimental.ExtensionMethod;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.exceptions.LightUpdateFailedException;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.nms.IMinecraftLightUpdater;
import me.shawlaf.varlight.spigot.util.RegionIterator;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.joor.Reflect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@ExtensionMethod({
        Util.class
})
public class LightUpdater implements IMinecraftLightUpdater, Listener {

    private final VarLightPlugin plugin;

    public LightUpdater(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        for (World world : plugin.getVarLightConfig().getVarLightEnabledWorlds()) {
            injectCustomILightAccess(world);
        }
    }

    @Override
    public CompletableFuture<Void> fullLightUpdate(World bukkitWorld, IntPosition position) throws VarLightNotActiveException {
        WorldServer nmsWorld = bukkitWorld.toNmsWorld();
        plugin.getApi().requireVarLightEnabled(bukkitWorld);

        LightEngineThreaded let = ((LightEngineThreaded) nmsWorld.getLightProvider());

        CompletableFuture<Void> future = new CompletableFuture<>();

        nmsWorld.runLightEngineSync(() -> ((LightEngineBlock) let.getLightingView(EnumSkyBlock.BLOCK)).checkBlock(position)).thenRunAsync(() -> {
            List<Future<IChunkAccess>> futures = new ArrayList<>();

            Iterator<ChunkCoords> chunks = RegionIterator.squareChunkArea(position.toChunkCoords(), 1);

            while (chunks.hasNext()) {
                ChunkCoords next = chunks.next();
                IChunkAccess chunk = nmsWorld.getChunkProvider().a(next.x, next.z);

                if (chunk == null) {
                    throw new LightUpdateFailedException(String.format("Could not get IChunkAccess for Chunk %s in world %s", next.toShortString(), bukkitWorld.getName()));
                }

                futures.add(let.lightChunk(chunk, true));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            plugin.getApi().getSyncExecutor().submit(() -> {
                try {
                    updateLightClient(bukkitWorld, position.toChunkCoords());
                } catch (VarLightNotActiveException e) {
                    e.printStackTrace();
                }

                future.complete(null);
            });
        }, plugin.getApi().getAsyncExecutor());

        return future;
    }

    @Override
    public CompletableFuture<Void> updateLightServer(World bukkitWorld, IntPosition position) throws VarLightNotActiveException {
        WorldServer nmsWorld = bukkitWorld.toNmsWorld();
        plugin.getApi().requireVarLightEnabled(bukkitWorld);

        LightEngineBlock leb = (LightEngineBlock) nmsWorld.getLightProvider().getLightingView(EnumSkyBlock.BLOCK);

        return nmsWorld.runLightEngineSync(() -> leb.checkBlock(position));
    }

    @Override
    public CompletableFuture<Void> updateLightServer(World bukkitWorld, Collection<IntPosition> positions) throws VarLightNotActiveException {
        WorldServer nmsWorld = bukkitWorld.toNmsWorld();
        plugin.getApi().requireVarLightEnabled(bukkitWorld);

        LightEngineBlock leb = (LightEngineBlock) nmsWorld.getLightProvider().getLightingView(EnumSkyBlock.BLOCK);

        return nmsWorld.runLightEngineSync(() -> {
            for (IntPosition position : positions) {
                leb.checkBlock(position);
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateLightServer(World bukkitWorld, ChunkCoords chunk) throws VarLightNotActiveException {
        WorldServer nmsWorld = bukkitWorld.toNmsWorld();
        plugin.getApi().requireVarLightEnabled(bukkitWorld);

        LightEngineThreaded let = ((LightEngineThreaded) nmsWorld.getLightProvider());
        IChunkAccess iChunkAccess = nmsWorld.getChunkProvider().a(chunk.x, chunk.z);

        if (iChunkAccess == null) {
            throw new LightUpdateFailedException(String.format("Could not get IChunkAccess for Chunk %s in world %s", chunk.toShortString(), bukkitWorld.getName()));
        }

        CompletableFuture<Void> future = new CompletableFuture<>();

        let.lightChunk(iChunkAccess, true).whenComplete((result, ex) -> {
            if (ex != null) {
                future.completeExceptionally(ex);
            } else {
                future.complete(null);
            }
        });

        return future;
    }

    @Override
    public void updateLightClient(World bukkitWorld, ChunkCoords centerChunk) throws VarLightNotActiveException {
        WorldServer nmsWorld = bukkitWorld.toNmsWorld();
        plugin.getApi().requireVarLightEnabled(bukkitWorld);

        LightEngine let = nmsWorld.e();

        Iterator<ChunkCoords> it = RegionIterator.squareChunkArea(centerChunk, 1);

        while (it.hasNext()) {
            ChunkCoords toSendClientUpdate = it.next();
            ChunkCoordIntPair chunkCoordIntPair = new ChunkCoordIntPair(toSendClientUpdate.x, toSendClientUpdate.z);
            PacketPlayOutLightUpdate ppolu = new PacketPlayOutLightUpdate(chunkCoordIntPair, let, true);

            nmsWorld.getChunkProvider().playerChunkMap.a(chunkCoordIntPair, false).forEach(e -> e.playerConnection.sendPacket(ppolu));
        }
    }

    private void injectCustomILightAccess(World bukkitWorld) {
        WorldServer nmsWorld = bukkitWorld.toNmsWorld();

        Reflect.on(
                nmsWorld.getChunkProvider().getLightEngine().getLightingView(EnumSkyBlock.BLOCK)
        ).set("a", new WrappedLightAccess(plugin, nmsWorld));
    }

    // region Events

    @EventHandler
    private void onWorldLoadEvent(WorldLoadEvent e) {
        if (plugin.getVarLightConfig().getVarLightEnabledWorldNames().contains(e.getWorld().getName())) {
            injectCustomILightAccess(e.getWorld());
        }
    }

    // endregion
}
