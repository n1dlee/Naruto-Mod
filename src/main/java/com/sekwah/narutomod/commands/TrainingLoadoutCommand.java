package com.sekwah.narutomod.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.clan.NinjaClan;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

final class TrainingLoadoutCommand {
    private TrainingLoadoutCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> add() {
        return Commands.literal("everything").executes(TrainingLoadoutCommand::enable);
    }

    static LiteralArgumentBuilder<CommandSourceStack> remove() {
        return Commands.literal("everything").executes(TrainingLoadoutCommand::disable);
    }

    private static int enable(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var actor = source.getPlayerOrException();
        var data = actor.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null) {
            source.sendFailure(Component.translatable("commands.narutomod.training.unavailable"));
            return 0;
        }
        if (!NinjaClan.isValid(data.getClanId())) {
            source.sendFailure(Component.translatable("commands.narutomod.training.clan"));
            return 0;
        }
        boolean refill = data.hasTrainingLoadout();
        data.enableTrainingLoadout();
        source.sendSuccess(() -> Component.translatable(refill
                ? "commands.narutomod.training.refilled" : "commands.narutomod.training.enabled"), false);
        return 1;
    }

    private static int disable(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var actor = source.getPlayerOrException();
        var data = actor.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (data == null || !data.disableTrainingLoadout()) {
            source.sendFailure(Component.translatable("commands.narutomod.training.inactive"));
            return 0;
        }
        if (data.getCurrentlyChanneledAbility() != null) {
            data.setCurrentlyChanneledAbility(actor, null);
        }
        source.sendSuccess(() -> Component.translatable("commands.narutomod.training.disabled"), false);
        return 1;
    }
}
