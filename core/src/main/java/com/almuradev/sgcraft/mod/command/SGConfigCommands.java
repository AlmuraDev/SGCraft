/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.almuradev.sgcraft.mod.command;

import com.almuradev.sgcraft.mod.SGConfig;
import com.almuradev.sgcraft.mod.SGServer;
import com.almuradev.sgcraft.mod.settings.SettingsCatalog;
import com.almuradev.sgcraft.mod.settings.SettingsCatalog.Setting;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * {@code /sgcraft config}: every setting the settings screen shows besides the address settings, by its place in
 * {@code sgcraft-server.json}. On its own it lists them; {@code <key>} shows one, with what it does, and
 * {@code <key> <value>} sets it. A setting applies at once unless it says it waits for a restart, and every change is
 * written to the settings file.
 */
final class SGConfigCommands {

    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(key ->
            SGCommands.text("config.unknown", "There's no setting called %s. /sgcraft config lists them", key));

    private SGConfigCommands() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> config() {
        return Commands.literal("config").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> SGConfigCommands.list(context.getSource()))
                .then(Commands.argument("key", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(SettingsCatalog.all()
                                .stream().map(Setting::key), builder))
                        .executes(context -> SGConfigCommands.show(context.getSource(),
                                StringArgumentType.getString(context, "key")))
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                .suggests((context, builder) -> {
                                    final String key = StringArgumentType.getString(context, "key");
                                    return SharedSuggestionProvider.suggest(SettingsCatalog.find(key)
                                            .map(SGConfigCommands::suggestions).orElse(List.of()), builder);
                                })
                                .executes(context -> SGConfigCommands.set(context.getSource(),
                                        StringArgumentType.getString(context, "key"),
                                        StringArgumentType.getString(context, "value")))));
    }

    private static List<String> suggestions(final Setting setting) {
        return setting.type() == SettingsCatalog.Type.BOOL ? List.of("true", "false")
                : List.of(setting.valueIn(SettingsCatalog.running(SGServer.require())), setting.defaultValue());
    }

    private static int list(final CommandSourceStack source) {
        final SGConfig running = SettingsCatalog.running(SGServer.require());
        final SGConfig file = SGConfig.load();
        source.sendSuccess(() -> SGCommands.text("config.header", "SGCraft's settings. /sgcraft config <key> shows "
                + "what one does:"), false);
        SettingsCatalog.Page page = null;
        for (final Setting setting : SettingsCatalog.all()) {
            if (setting.page() != page) {
                page = setting.page();
                final Component title = page.title();
                source.sendSuccess(() -> Component.empty().append(title).withStyle(ChatFormatting.GOLD), false);
            }
            source.sendSuccess(() -> SGConfigCommands.line(setting, running, file), false);
        }
        return SettingsCatalog.all().size();
    }

    private static int show(final CommandSourceStack source, final String key) throws CommandSyntaxException {
        final Setting setting = SettingsCatalog.find(key).orElseThrow(() -> SGConfigCommands.UNKNOWN.create(key));
        source.sendSuccess(() -> SGConfigCommands.line(setting, SettingsCatalog.running(SGServer.require()),
                SGConfig.load()), false);
        source.sendSuccess(() -> Component.literal("  ").append(setting.tip()).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  ").append(SGCommands.text("config.default", "Default: %s",
                setting.defaultValue())).withStyle(ChatFormatting.GRAY), false);
        if (!setting.live()) {
            source.sendSuccess(() -> Component.literal("  ").append(SGCommands.text("config.restart", "Changes "
                    + "take effect when the server next starts")).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int set(final CommandSourceStack source, final String key, final String value)
            throws CommandSyntaxException {
        if (SettingsCatalog.find(key).isEmpty()) {
            throw SGConfigCommands.UNKNOWN.create(key);
        }
        final SettingsCatalog.Result result = SettingsCatalog.set(SGServer.require(), key, value);
        if (result.refusal().isPresent()) {
            source.sendFailure(result.refusal().get());
            return 0;
        }
        final List<Component> messages = result.messages();
        source.sendSuccess(messages::getFirst, true);
        messages.stream().skip(1).forEach(source::sendFailure);
        return 1;
    }

    // The setting's key and value, with its value after a restart when that is different
    private static Component line(final Setting setting, final SGConfig running, final SGConfig file) {
        final String now = setting.valueIn(running);
        final String next = setting.valueIn(file);
        final MutableComponent line = Component.literal("  ")
                .append(Component.literal(setting.key()).withStyle(ChatFormatting.AQUA))
                .append(" = " + now);
        if (!now.equals(next)) {
            line.append(" ").append(SGCommands.text("config.next", "(%s after a restart)", next)
                    .withStyle(ChatFormatting.YELLOW));
        }
        return line;
    }
}
