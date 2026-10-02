package fr.bornecraft.bornedex;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Advancements granted from code (they use the {@code minecraft:impossible} trigger),
 * used as FTB quest tasks.
 */
public final class ModAdvancements {
    /** First elevation engraved with a theodolite. */
    public static final ResourceLocation FIRST_SURVEY = id("first_survey");
    /** First elevation erased with a brush. */
    public static final ResourceLocation CLEAN_SLATE = id("clean_slate");

    private ModAdvancements() {}

    /**
     * Grants every remaining criterion of the advancement. Does nothing client side or if
     * the advancement is missing (e.g. removed by a datapack).
     */
    public static void award(Player player, ResourceLocation id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        AdvancementHolder advancement = serverPlayer.server.getAdvancements().get(id);
        if (advancement == null) {
            return;
        }
        PlayerAdvancements advancements = serverPlayer.getAdvancements();
        for (String criterion : advancements.getOrStartProgress(advancement).getRemainingCriteria()) {
            advancements.award(advancement, criterion);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Bornedex.MOD_ID, path);
    }
}
