package fr.bornecraft.bornedex;

import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * FTB Quests only reads its quests from {@code config/ftbquests/quests}.
 * The bundled chapter is installed there once, then a marker file ({@code config/bornedex/quests_installed})
 * prevents any further install, so a pack author can delete or edit the chapter freely.
 * <ul>
 *     <li>Quest files are only copied if missing: existing files (pack settings, in-game edits) are never overwritten.</li>
 *     <li>Lang files are merged: our keys are added to an existing file, existing translations are kept.</li>
 *     <li>The whole install can be disabled with {@link BornedexConfig#INSTALL_DEFAULT_QUESTS}.</li>
 * </ul>
 * Only loaded when FTB Quests is present (it references FTB Library).
 */
public final class QuestInstaller {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESOURCE_ROOT = "/bornedex_quests/";

    private static final List<String> QUEST_FILES = List.of(
            "data.snbt",
            "chapter_groups.snbt",
            "chapters/bornedex.snbt"
    );

    private static final List<String> LANG_FILES = List.of(
            "lang/en_us.snbt",
            "lang/fr_fr.snbt"
    );

    private QuestInstaller() {}

    public static void install() {
        if (!BornedexConfig.INSTALL_DEFAULT_QUESTS.get()) {
            LOGGER.debug("[Bornedex] Default quest install disabled in config");
            return;
        }
        Path marker = FMLPaths.CONFIGDIR.get().resolve(Bornedex.MOD_ID).resolve("quests_installed");
        if (Files.exists(marker)) {
            LOGGER.debug("[Bornedex] Default quests already installed ({} exists), skipping", marker);
            return;
        }

        Path target = FMLPaths.CONFIGDIR.get().resolve("ftbquests").resolve("quests");
        boolean ok = true;
        int changed = 0;
        for (String rel : QUEST_FILES) {
            try {
                if (copyIfMissing(rel, target.resolve(rel))) {
                    changed++;
                }
            } catch (IOException e) {
                LOGGER.error("[Bornedex] Failed to install quest file {}", rel, e);
                ok = false;
            }
        }
        for (String rel : LANG_FILES) {
            try {
                if (mergeLang(rel, target.resolve(rel))) {
                    changed++;
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.error("[Bornedex] Failed to merge quest lang file {}", rel, e);
                ok = false;
            }
        }
        if (changed > 0) {
            LOGGER.info("[Bornedex] Installed or updated {} FTB Quests file(s) in {}", changed, target);
        }

        // On failure, no marker: the install is retried on next launch
        if (!ok) {
            return;
        }
        try {
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "Delete this file to reinstall the Bornedex FTB Quests chapter on next launch.\n");
        } catch (IOException e) {
            LOGGER.error("[Bornedex] Failed to write quest install marker {}", marker, e);
        }
    }

    private static boolean copyIfMissing(String rel, Path out) throws IOException {
        if (Files.exists(out)) {
            LOGGER.debug("[Bornedex] Quest file {} already present, skipping", out);
            return false;
        }
        try (InputStream in = openResource(rel)) {
            Files.createDirectories(out.getParent());
            Files.copy(in, out);
        }
        return true;
    }

    /** Adds our missing keys to an existing lang file (copies it if absent). Returns true if the file was written. */
    private static boolean mergeLang(String rel, Path out) throws IOException {
        if (!Files.exists(out)) {
            return copyIfMissing(rel, out);
        }
        CompoundTag existing = SNBT.tryRead(out);
        if (existing == null) {
            throw new IOException("Unreadable lang file " + out);
        }
        CompoundTag bundled;
        try (InputStream in = openResource(rel);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            bundled = SNBT.readLines(reader.lines().toList());
        }
        int added = 0;
        for (String key : bundled.getAllKeys()) {
            if (!existing.contains(key)) {
                existing.put(key, bundled.get(key).copy());
                added++;
            }
        }
        if (added == 0) {
            return false;
        }
        SNBT.tryWrite(out, existing);
        LOGGER.info("[Bornedex] Merged {} key(s) into {}", added, out);
        return true;
    }

    private static InputStream openResource(String rel) throws IOException {
        InputStream in = QuestInstaller.class.getResourceAsStream(RESOURCE_ROOT + rel);
        if (in == null) {
            throw new IOException("Missing bundled quest file " + rel);
        }
        return in;
    }
}
