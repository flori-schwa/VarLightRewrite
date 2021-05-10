package me.shawlaf.varlight.spigot.command.old.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.SUCCESS;

public class LuiConsumeExecutor extends SubCommandExecutor {
    public LuiConsumeExecutor(VarLightSubCommand command) {
        super(command);
    }

    public int executeGet(CommandContext<CommandSender> context) throws CommandSyntaxException {
        if (plugin.getVarLightConfig().isConsumeLui()) {
            info(command, context.getSource(), "Consume LUI is enabled.", ChatColor.GREEN);
        } else {
            info(command, context.getSource(), "Consume LUI is  disabled.", ChatColor.RED);
        }

        return SUCCESS;
    }

    public int executeEnable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getVarLightConfig().setConsumeLui(true);

        successBroadcast(command, context.getSource(), "Enabled Consume LUI.");

        return SUCCESS;
    }

    public int executeDisable(CommandContext<CommandSender> context) throws CommandSyntaxException {
        plugin.getVarLightConfig().setConsumeLui(false);

        successBroadcast(command, context.getSource(), "Disabled Consume LUI.");

        return SUCCESS;
    }
}
