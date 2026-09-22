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
 * Au premier lancement, on y copie les fichiers de quêtes embarqués dans le jar.
 * Si le dossier existe déjà, on ne touche à rien (les éditions in-game sont conservées).
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
        if (Files.isDirectory(target)) {
            LOGGER.debug("[Bornedex] Quest folder already present at {}, skipping install", target);
            return;
        }
        LOGGER.info("[Bornedex] Installing default FTB Quests files into {}", target);
        for (String rel : FILES) {
            try (InputStream in = QuestInstaller.class.getResourceAsStream(RESOURCE_ROOT + rel)) {
                if (in == null) {
                    LOGGER.warn("[Bornedex] Missing bundled quest file {}", rel);
                    continue;
                }
                Path out = target.resolve(rel);
                Files.createDirectories(out.getParent());
                Files.copy(in, out);
            } catch (IOException e) {
                LOGGER.error("[Bornedex] Failed to install quest file {}", rel, e);
            }
        }
    }
}
