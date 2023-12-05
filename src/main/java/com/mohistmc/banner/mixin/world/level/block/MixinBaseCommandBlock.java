package com.mohistmc.banner.mixin.world.level.block;

import com.google.common.base.Joiner;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BaseCommandBlock;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.event.server.ServerCommandEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseCommandBlock.class)
public class MixinBaseCommandBlock {


    // @formatter:off
    @Shadow private Component name;
    // @formatter:on

    @Redirect(method = "performCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)I"))
    private int banner$serverCommand(Commands commands, CommandSourceStack sender, String command) {
        Joiner joiner = Joiner.on(" ");
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        ServerCommandEvent event = new ServerCommandEvent(sender.banner$getBukkitSender(), command);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return 0;
        }
        command = event.getCommand();

        String[] args = command.split(" ");

        String cmd = args[0];
        if (cmd.startsWith("minecraft:")) cmd = cmd.substring("minecraft:".length());
        if (cmd.startsWith("bukkit:")) cmd = cmd.substring("bukkit:".length());

        if (cmd.equalsIgnoreCase("stop") || cmd.equalsIgnoreCase("kick") || cmd.equalsIgnoreCase("op")
                || cmd.equalsIgnoreCase("deop") || cmd.equalsIgnoreCase("ban") || cmd.equalsIgnoreCase("ban-ip")
                || cmd.equalsIgnoreCase("pardon") || cmd.equalsIgnoreCase("pardon-ip") || cmd.equalsIgnoreCase("reload")) {
            return 0;
        }

        if (((CraftServer) Bukkit.getServer()).getCommandBlockOverride(args[0])) {
            args[0] = "minecraft:" + args[0];
        }

        return commands.performPrefixedCommand(sender, joiner.join(args));
    }

    @Inject(method = "setName", at = @At("RETURN"))
    public void banner$setName(Component nameIn, CallbackInfo ci) {
        if (this.name == null) {
            this.name = Component.literal("@");
        }
    }
}
