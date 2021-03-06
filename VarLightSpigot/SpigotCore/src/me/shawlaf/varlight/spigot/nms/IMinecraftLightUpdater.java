package me.shawlaf.varlight.spigot.nms;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.module.IPluginLifeCycleOperations;
import me.shawlaf.varlight.spigot.persistence.CustomLightStorage;
import me.shawlaf.varlight.util.ChunkCoords;
import me.shawlaf.varlight.util.IntPosition;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * An Interface, that defines methods for updating Light in Minecraft Internally
 */
public interface IMinecraftLightUpdater extends IPluginLifeCycleOperations {

    CompletableFuture<Void> updateLightSingleBlock(CustomLightStorage lightStorage, IntPosition position);

    CompletableFuture<Void> updateLightMultiBlock(CustomLightStorage lightStorage, Collection<IntPosition> positions, Collection<CommandSender> progressSubscribers);

    CompletableFuture<Void> updateLightChunk(CustomLightStorage lightStorage, ChunkCoords chunk, Collection<CommandSender> progressSubscribers);

    CompletableFuture<Void> updateLightMultiChunk(CustomLightStorage lightStorage, Collection<ChunkCoords> chunkPositions, Collection<CommandSender> progressSubscribers);

    CompletableFuture<Void> clearLightChunk(CustomLightStorage lightStorage, ChunkCoords chunk, Collection<CommandSender> progressSubscribers);

    CompletableFuture<Void> clearLightMultiChunk(CustomLightStorage lightStorage, Collection<ChunkCoords> chunkPositions, Collection<CommandSender> progressSubscribers);


    default CompletableFuture<Void> updateLightMultiBlock(CustomLightStorage lightStorage, Collection<IntPosition> positions) {
        return updateLightMultiBlock(lightStorage, positions, null);
    }

    default CompletableFuture<Void> updateLightChunk(CustomLightStorage lightStorage, ChunkCoords chunk) {
        return updateLightChunk(lightStorage, chunk, null);
    }

    default CompletableFuture<Void> updateLightMultiChunk(CustomLightStorage lightStorage, Collection<ChunkCoords> chunkPositions) {
        return updateLightMultiChunk(lightStorage, chunkPositions, null);
    }

    default CompletableFuture<Void> clearLightChunk(CustomLightStorage lightStorage, ChunkCoords chunk) {
        return clearLightChunk(lightStorage, chunk, null);
    }

    default CompletableFuture<Void> clearLightMultiChunk(CustomLightStorage lightStorage, Collection<ChunkCoords> chunkPositions) {
        return clearLightMultiChunk(lightStorage, chunkPositions, null);
    }

    default CompletableFuture<Void> updateLightSingleBlock(World world, IntPosition position) throws VarLightNotActiveException {
        return updateLightSingleBlock(getPlugin().getApi().requireVarLightEnabled(world), position);
    }

    default CompletableFuture<Void> updateLightMultiBlock(World world, Collection<IntPosition> positions) throws VarLightNotActiveException {
        return updateLightMultiBlock(getPlugin().getApi().requireVarLightEnabled(world), positions);
    }

    default CompletableFuture<Void> updateLightChunk(World world, ChunkCoords chunkCoords) throws VarLightNotActiveException {
        return clearLightChunk(getPlugin().getApi().requireVarLightEnabled(world), chunkCoords);
    }

    default CompletableFuture<Void> updateLightMultiChunk(World world, Collection<ChunkCoords> chunkPositions) throws VarLightNotActiveException {
        return clearLightMultiChunk(getPlugin().getApi().requireVarLightEnabled(world), chunkPositions);
    }

    default CompletableFuture<Void> clearLightChunk(World world, ChunkCoords chunkCoords) throws VarLightNotActiveException {
        return clearLightChunk(getPlugin().getApi().requireVarLightEnabled(world), chunkCoords);
    }

    default CompletableFuture<Void> clearLightMultiChunk(World world, Collection<ChunkCoords> chunkPositions) throws VarLightNotActiveException {
        return clearLightMultiChunk(getPlugin().getApi().requireVarLightEnabled(world), chunkPositions);
    }

    VarLightPlugin getPlugin();

}
