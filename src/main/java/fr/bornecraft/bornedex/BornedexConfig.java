package fr.bornecraft.bornedex;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Startup config ({@code config/bornedex-startup.toml}).
 * STARTUP configs are loaded as soon as they are registered, so they can be read from the mod constructor.
 */
public final class BornedexConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue INSTALL_DEFAULT_QUESTS = BUILDER
            .comment("Copy the Bornedex chapter into config/ftbquests/quests on first launch.",
                    "Installation only happens once: delete config/bornedex/quests_installed to run it again.")
            .define("installDefaultQuests", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private BornedexConfig() {}
}
