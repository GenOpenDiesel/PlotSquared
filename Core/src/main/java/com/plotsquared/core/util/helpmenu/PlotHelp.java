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
package com.plotsquared.core.util.helpmenu;

import com.plotsquared.core.configuration.caption.StaticCaption;
import com.plotsquared.core.configuration.file.YamlConfiguration;
import com.plotsquared.core.player.PlotPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Fully configurable content for the default {@code /plot help} page, driven by {@code config/pomoc.yml}.
 * <p>
 * Server owners decide exactly what every line says, including colours and clickable commands.
 * Each entry under {@code pages} is one page; pages are navigated with the clickable
 * {@code [<<<]} / {@code [>>>]} buttons (placeholder {@code <nawigacja>}) or {@code /plot help <number>}.
 */
public class PlotHelp {

    private static final Logger LOGGER = LogManager.getLogger("PlotSquared/" + PlotHelp.class.getSimpleName());

    private boolean enabled;
    private List<List<String>> pages = new ArrayList<>();

    /**
     * (Re)load {@code pomoc.yml} from the config folder, writing the default file if it does not exist yet.
     *
     * @param configFolder the PlotSquared {@code config} folder
     */
    public void load(final File configFolder) {
        final File file = new File(configFolder, "pomoc.yml");
        if (!file.exists()) {
            try {
                Files.writeString(file.toPath(), DEFAULT_CONTENT, StandardCharsets.UTF_8);
            } catch (final IOException e) {
                LOGGER.error("Failed to create pomoc.yml", e);
            }
        }
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        this.enabled = yaml.getBoolean("enabled", true);
        final List<List<String>> loaded = new ArrayList<>();
        final List<?> rawPages = yaml.getList("pages");
        if (rawPages != null) {
            for (final Object pageObject : rawPages) {
                if (pageObject instanceof List<?> rawLines) {
                    final List<String> lines = new ArrayList<>();
                    for (final Object line : rawLines) {
                        lines.add(String.valueOf(line));
                    }
                    loaded.add(lines);
                }
            }
        }
        this.pages = loaded;
    }

    /**
     * @return {@code true} if the custom help is enabled and has at least one page to show
     */
    public boolean isEnabled() {
        return this.enabled && !this.pages.isEmpty();
    }

    /**
     * Render the requested page of the custom help to the player.
     *
     * @param player      the recipient
     * @param page        1-indexed page number ({@code 0} or out-of-range values are clamped)
     * @param helpCommand base command used by the clickable navigation, e.g. {@code /plot help}
     * @return {@code true} if something was sent, {@code false} if the custom help is disabled/empty
     */
    public boolean render(final PlotPlayer<?> player, final int page, final String helpCommand) {
        if (!isEnabled()) {
            return false;
        }
        int index = page <= 0 ? 0 : page - 1;
        if (index >= this.pages.size()) {
            index = this.pages.size() - 1;
        }
        final String navigation = buildNavigation(index, this.pages.size(), helpCommand);
        final List<String> lines = this.pages.get(index);
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(lines.get(i)
                    .replace("<nawigacja>", navigation)
                    .replace("<strona>", String.valueOf(index + 1))
                    .replace("<max_stron>", String.valueOf(this.pages.size())));
        }
        player.sendMessage(StaticCaption.of(builder.toString()));
        return true;
    }

    private String buildNavigation(final int index, final int maxPages, final String helpCommand) {
        final StringBuilder navigation = new StringBuilder();
        if (index > 0) {
            navigation.append("<click:run_command:'").append(helpCommand).append(' ').append(index)
                    .append("'><hover:show_text:'<gray>Poprzednia strona</gray>'><gold>[<<<]</gold></hover></click>");
        }
        if (index > 0 && index < maxPages - 1) {
            navigation.append("<gray> </gray>");
        }
        if (index < maxPages - 1) {
            navigation.append("<click:run_command:'").append(helpCommand).append(' ').append(index + 2)
                    .append("'><hover:show_text:'<gray>Następna strona</gray>'><gold>[>>>]</gold></hover></click>");
        }
        return navigation.toString();
    }

    private static final String DEFAULT_CONTENT = """
            # ============================================================
            #  pomoc.yml  -  pełna kontrola nad /plot help
            # ============================================================
            # Tutaj decydujesz DOKŁADNIE co i jak pokazuje /plot help.
            #
            #  - Każdy element listy "pages" to osobna STRONA.
            #  - Każda strona to lista LINII - tekst od a do z piszesz sam.
            #  - Gracz przełącza strony klikając [<<<] / [>>>]  albo  /plot help <numer>.
            #
            # Formatowanie (MiniMessage):
            #   <gold>tekst</gold>, <gray>, <red>, <green>, <yellow>, <aqua>, <bold>, <italic>
            #   <dark_gray><strikethrough>------   (separatory / linie)
            #
            # Klikalna komenda (gracz klika i komenda się wykonuje):
            #   <click:run_command:'/plot auto'><gold>/plot auto</gold></click>
            # Wpisanie komendy do czatu (gracz dopisuje argument):
            #   <click:suggest_command:'/plot add '><gold>/plot add</gold></click>
            # Podpowiedź po najechaniu myszką:
            #   <hover:show_text:'<gray>Opis'>...</hover>
            #
            # Specjalne wstawki:
            #   <nawigacja>   ->  klikalne przyciski [<<<] [>>>]
            #   <strona>      ->  numer aktualnej strony
            #   <max_stron>   ->  liczba wszystkich stron
            #
            # enabled: false  ->  wyłącza ten plik i wraca do domyślnej pomocy PlotSquared.
            # Po zmianach wpisz na serwerze:  /plot reload
            # ============================================================

            enabled: true

            pages:
              # ---------------- STRONA 1 ----------------
              -
                - "<dark_gray><strikethrough>------------------------------"
                - "<gold><bold>            Pomoc - Działki"
                - "<dark_gray><strikethrough>------------------------------"
                - "<click:run_command:'/plot home'><hover:show_text:'<gray>Kliknij, aby wykonać'><gold>/plot home <dark_gray>- <gray>Teleport na swoją działkę</hover></click>"
                - "<click:run_command:'/plot auto'><hover:show_text:'<gray>Kliknij, aby wykonać'><gold>/plot auto <dark_gray>- <gray>Zajmij najbliższą wolną działkę</hover></click>"
                - "<click:run_command:'/plot claim'><hover:show_text:'<gray>Kliknij, aby wykonać'><gold>/plot claim <dark_gray>- <gray>Zajmij działkę, na której stoisz</hover></click>"
                - "<click:run_command:'/plot info'><hover:show_text:'<gray>Kliknij, aby wykonać'><gold>/plot info <dark_gray>- <gray>Informacje o działce</hover></click>"
                - "<click:run_command:'/plot delete'><hover:show_text:'<gray>Kliknij, aby wykonać'><gold>/plot delete <dark_gray>- <gray>Usuń swoją działkę</hover></click>"
                - "<click:run_command:'/plot merge'><hover:show_text:'<gray>Kliknij, aby wykonać'><gold>/plot merge <dark_gray>- <gray>Połącz swoje działki</hover></click>"
                - "<dark_gray><strikethrough>------</strikethrough> <gold>Strona <strona>/<max_stron></gold> <nawigacja> <dark_gray><strikethrough>------"
              # ---------------- STRONA 2 ----------------
              -
                - "<dark_gray><strikethrough>------------------------------"
                - "<gold><bold>            Pomoc - Działki"
                - "<dark_gray><strikethrough>------------------------------"
                - "<click:suggest_command:'/plot visit '><hover:show_text:'<gray>Kliknij, aby wpisać'><gold>/plot visit <gray>[gracz] <dark_gray>- <gray>Odwiedź czyjąś działkę</hover></click>"
                - "<click:suggest_command:'/plot add '><hover:show_text:'<gray>Kliknij, aby wpisać'><gold>/plot add <gray>[gracz] <dark_gray>- <gray>Dodaj gracza (gdy jesteś online)</hover></click>"
                - "<click:suggest_command:'/plot trust '><hover:show_text:'<gray>Kliknij, aby wpisać'><gold>/plot trust <gray>[gracz] <dark_gray>- <gray>Zaufaj graczowi (także offline)</hover></click>"
                - "<click:suggest_command:'/plot remove '><hover:show_text:'<gray>Kliknij, aby wpisać'><gold>/plot remove <gray>[gracz] <dark_gray>- <gray>Usuń gracza z działki</hover></click>"
                - "<click:suggest_command:'/plot deny '><hover:show_text:'<gray>Kliknij, aby wpisać'><gold>/plot deny <gray>[gracz] <dark_gray>- <gray>Zablokuj graczowi wejście</hover></click>"
                - "<dark_gray><strikethrough>------</strikethrough> <gold>Strona <strona>/<max_stron></gold> <nawigacja> <dark_gray><strikethrough>------"
            """;

}
