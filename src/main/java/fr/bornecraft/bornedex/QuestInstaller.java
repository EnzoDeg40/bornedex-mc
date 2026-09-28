package fr.bornecraft.bornedex;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * FTB Quests ne lit ses quêtes que depuis {@code config/ftbquests/quests}.
 * On y copie chaque fichier de quêtes embarqué dans le jar s'il n'existe pas encore.
 * Un fichier déjà présent n'est jamais écrasé (les éditions in-game sont conservées).
 */
public final class QuestInstaller {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESOURCE_ROOT = "/bornedex_quests/";

    private static final List<String> FILES = List.of(
            "data.snbt",
            "chapter_groups.snbt",
            "chapters/bornedex.snbt",
            "lang/en_us.snbt",
            "lang/fr_fr.snbt"
    );

    private QuestInstaller() {}

    public static void installIfMissing() {
        Path target = FMLPaths.CONFIGDIR.get().resolve("ftbquests").resolve("quests");
        int installed = 0;
        for (String rel : FILES) {
            Path out = target.resolve(rel);
            if (Files.exists(out)) {
                LOGGER.debug("[Bornedex] Quest file {} already present, skipping", out);
                continue;
            }
            try (InputStream in = QuestInstaller.class.getResourceAsStream(RESOURCE_ROOT + rel)) {
                if (in == null) {
                    LOGGER.warn("[Bornedex] Missing bundled quest file {}", rel);
                    continue;
                }
                Files.createDirectories(out.getParent());
                Files.copy(in, out);
                installed++;
            } catch (IOException e) {
                LOGGER.error("[Bornedex] Failed to install quest file {}", rel, e);
            }
        }
        if (installed > 0) {
            LOGGER.info("[Bornedex] Installed {} default FTB Quests file(s) into {}", installed, target);
        }
    }
}
