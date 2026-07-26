package com.sekwah.narutomod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.function.Consumer;

public class NinjaCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> ninja = Commands.literal("ninja")
                .requires(source -> source.hasPermission(2));

        ninja.then(Commands.literal("get")
                .executes(ctx -> runForSelf(ctx.getSource(), data -> sendStats(ctx.getSource(), ctx.getSource().getPlayer(), data)))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            return runForTarget(ctx.getSource(), target, data -> sendStats(ctx.getSource(), target, data));
                        })));

        ninja.then(Commands.literal("set")
                .then(Commands.literal("chakra")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                        .executes(ctx -> mutateTarget(ctx, "chakra",
                                                data -> data.setChakra(FloatArgumentType.getFloat(ctx, "amount")))))))
                .then(Commands.literal("stamina")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                        .executes(ctx -> mutateTarget(ctx, "stamina",
                                                data -> data.setStamina(FloatArgumentType.getFloat(ctx, "amount")))))))
                .then(Commands.literal("rank")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("rank", StringArgumentType.word())
                                        .executes(ctx -> {
                                            int rank = parseRank(StringArgumentType.getString(ctx, "rank"));
                                            if (rank < 0) {
                                                sendInvalid(ctx.getSource(), "rank", "academy, genin, chunin, jonin, kage");
                                                return 0;
                                            }
                                            return mutateTarget(ctx, "rank", data -> data.setNinjaRank(rank));
                                        }))))
                .then(Commands.literal("clan")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("clan", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String clan = parseClan(StringArgumentType.getString(ctx, "clan"));
                                            if (clan == null) {
                                                sendInvalid(ctx.getSource(), "clan", "uzumaki, uchiha, hyuga, nara, haruno, senju, akimichi, yamanaka, inuzuka, aburame, none");
                                                return 0;
                                            }
                                            return mutateTarget(ctx, "clan", data -> data.setClanId(clan));
                                        }))))
                .then(Commands.literal("nature")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("nature", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String nature = parseNature(StringArgumentType.getString(ctx, "nature"));
                                            if (nature == null) {
                                                sendInvalid(ctx.getSource(), "nature", "fire, water, wind, earth, lightning, none");
                                                return 0;
                                            }
                                            return mutateTarget(ctx, "nature affinity", data -> data.setNatureAffinity(nature));
                                        }))))
                .then(Commands.literal("ninja")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> mutateTarget(ctx, "ninja mode",
                                                data -> data.setIsNinja(BoolArgumentType.getBool(ctx, "enabled"))))))));

        ninja.then(Commands.literal("add")
                .then(Commands.literal("chakra")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> mutateTarget(ctx, "chakra",
                                                data -> data.addChakra(FloatArgumentType.getFloat(ctx, "amount")))))))
                .then(Commands.literal("stamina")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> mutateTarget(ctx, "stamina",
                                                data -> data.addStamina(FloatArgumentType.getFloat(ctx, "amount")))))))
                .then(Commands.literal("xp")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                        .executes(ctx -> mutateTarget(ctx, "chakra XP",
                                                data -> data.addChakraXp(FloatArgumentType.getFloat(ctx, "amount"))))))));

        ninja.then(Commands.literal("reset")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> mutateTarget(ctx, "ninja data", INinjaData::resetProgression))));

        dispatcher.register(ninja);
    }

    private static int runForSelf(CommandSourceStack source, Consumer<INinjaData> consumer) {
        ServerPlayer player = source.getPlayer();
        return runForTarget(source, player, consumer);
    }

    private static int runForTarget(CommandSourceStack source, ServerPlayer target, Consumer<INinjaData> consumer) {
        target.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> consumer.accept(data));
        return Command.SINGLE_SUCCESS;
    }

    private static int mutateTarget(CommandContext<CommandSourceStack> ctx, String field, Consumer<INinjaData> mutation) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        return runForTarget(ctx.getSource(), target, data -> {
            mutation.accept(data);
            sendChanged(ctx.getSource(), target, field);
        });
    }

    private static void sendStats(CommandSourceStack source, ServerPlayer target, INinjaData data) {
        source.sendSuccess(() -> Component.translatable("commands.narutomod.ninja.get",
                target.getDisplayName(),
                formatFloat(data.getChakra()),
                formatFloat(data.getMaxChakra()),
                formatFloat(data.getStamina()),
                formatFloat(data.getMaxStamina()),
                formatFloat(data.getChakraXp()),
                rankName(data.getNinjaRank()),
                data.getClanId().isEmpty() ? "none" : data.getClanId(),
                String.valueOf(data.isNinjaModeEnabled())), false);
    }

    private static void sendChanged(CommandSourceStack source, ServerPlayer target, String field) {
        source.sendSuccess(() -> Component.translatable("commands.narutomod.ninja.changed",
                Component.literal(field).withStyle(ChatFormatting.YELLOW),
                target.getDisplayName()), true);
    }

    private static void sendInvalid(CommandSourceStack source, String field, String allowedValues) {
        source.sendFailure(Component.translatable("commands.narutomod.ninja.invalid",
                field,
                allowedValues));
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String rankName(int rank) {
        return switch (rank) {
            case 1 -> "genin";
            case 2 -> "chunin";
            case 3 -> "jonin";
            case 4 -> "kage";
            default -> "academy";
        };
    }

    private static int parseRank(String rank) {
        return switch (rank.toLowerCase(Locale.ROOT)) {
            case "academy" -> 0;
            case "genin" -> 1;
            case "chunin" -> 2;
            case "jonin" -> 3;
            case "kage" -> 4;
            default -> -1;
        };
    }

    private static String parseClan(String clan) {
        return switch (clan.toLowerCase(Locale.ROOT)) {
            case "uzumaki", "uchiha", "hyuga", "nara", "haruno", "senju",
                 "akimichi", "yamanaka", "inuzuka", "aburame" -> clan.toLowerCase(Locale.ROOT);
            case "none" -> "";
            default -> null;
        };
    }

    private static String parseNature(String nature) {
        return switch (nature.toLowerCase(Locale.ROOT)) {
            case "fire", "water", "wind", "earth", "lightning" -> nature.toLowerCase(Locale.ROOT);
            case "none" -> "";
            default -> null;
        };
    }
}
