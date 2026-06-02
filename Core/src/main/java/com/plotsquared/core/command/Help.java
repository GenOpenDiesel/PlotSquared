/*
 * PlotSquared, a land and world management plugin for Minecraft.
 * Copyright (C) IntellectualSites <https://intellectualsites.com>
 * Copyright (C) IntellectualSites team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plotsquared.core.command;

import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.util.MathMan;
import com.plotsquared.core.util.StringMan;
import com.plotsquared.core.util.helpmenu.HelpMenu;
import com.plotsquared.core.util.task.RunnableVal2;
import com.plotsquared.core.util.task.RunnableVal3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@CommandDeclaration(command = "help",
        aliases = "?",
        category = CommandCategory.INFO,
        usage = "help [category | #]",
        permission = "plots.use")
public class Help extends Command {

    /**
     * The commands shown (in order) on the default {@code /plot help} page, before any
     * category is selected. Use the clickable {@code [>>>]} / {@code [<<<]} navigation to page through them.
     */
    private static final List<String> FAVORITE_COMMANDS = List.of(
            "home", "auto", "claim", "info", "delete", "merge", "visit", "add", "trust", "remove", "deny"
    );

    public Help(Command parent) {
        super(parent, true);
    }

    @Override
    public boolean canExecute(PlotPlayer<?> player, boolean message) {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> execute(
            PlotPlayer<?> player, String[] args,
            RunnableVal3<Command, Runnable, Runnable> confirm,
            RunnableVal2<Command, CommandResult> whenDone
    ) {
        switch (args.length) {
            case 0 -> {
                return displayHelp(player, null, 0);
            }
            case 1 -> {
                if (MathMan.isInteger(args[0])) {
                    try {
                        return displayHelp(player, null, Integer.parseInt(args[0]));
                    } catch (NumberFormatException ignored) {
                        return displayHelp(player, null, 1);
                    }
                } else {
                    return displayHelp(player, args[0], 1);
                }
            }
            case 2 -> {
                if (MathMan.isInteger(args[1])) {
                    try {
                        return displayHelp(player, args[0], Integer.parseInt(args[1]));
                    } catch (NumberFormatException ignored) {
                        return displayHelp(player, args[0], 1);
                    }
                }
                return CompletableFuture.completedFuture(false);
            }
            default -> sendUsage(player);
        }
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> displayHelp(
            final PlotPlayer<?> player, final String catRaw,
            final int page
    ) {
        return CompletableFuture.supplyAsync(() -> {
            String cat = catRaw;

            CommandCategory catEnum = null;
            if (cat != null) {
                if (!"all".equalsIgnoreCase(cat)) {
                    for (CommandCategory c : CommandCategory.values()) {
                        if (StringMan.isEqualIgnoreCaseToAny(cat, c.name(), c.toString())) {
                            catEnum = c;
                            cat = c.name();
                            break;
                        }
                    }
                    if (catEnum == null) {
                        cat = null;
                    }
                }
            }
            final String label = getParent().toString();
            if (cat == null) {
                // Default help: prefer the fully configurable config/pomoc.yml content, so server
                // owners control exactly what is shown. Falls back to the curated list below if disabled.
                if (com.plotsquared.core.PlotSquared.get().getPlotHelp()
                        .render(player, page, "/" + label + " help")) {
                    return true;
                }
                // Fallback: a curated list of the most common commands, paginated
                // and navigable through the clickable [<<<] / [>>>] buttons.
                final List<Command> favorites = new ArrayList<>();
                for (final String name : FAVORITE_COMMANDS) {
                    final Command command = MainCommand.getInstance().getCommand(name);
                    if (command != null && command.canExecute(player, false)) {
                        favorites.add(command);
                    }
                }
                new HelpMenu(player)
                        .setCommands(favorites)
                        .setHelpCommand("/" + label + " help")
                        .generateMaxPages()
                        .generatePage(page - 1, label, player)
                        .render();
                return true;
            }
            new HelpMenu(player)
                    .setCategory(catEnum)
                    .setHelpCommand("/" + label + " help " + cat.toLowerCase(Locale.ENGLISH))
                    .getCommands()
                    .generateMaxPages()
                    .generatePage(page - 1, label, player)
                    .render();
            return true;
        });
    }

    @Override
    public Collection<Command> tab(PlotPlayer<?> player, String[] args, boolean space) {
        final String argument = args[0].toLowerCase(Locale.ENGLISH);
        List<Command> result = new ArrayList<>();

        for (final CommandCategory category : CommandCategory.values()) {
            if (!category.canAccess(player)) {
                continue;
            }
            String name = category.name().toLowerCase();
            if (!name.startsWith(argument)) {
                continue;
            }
            result.add(new Command(null, false, name, "", RequiredType.NONE, null) {
            });
        }
        // add the category "all"
        if ("all".startsWith(argument)) {
            result.add(new Command(null, false, "all", "", RequiredType.NONE, null) {
            });
        }
        return result;
    }

}
