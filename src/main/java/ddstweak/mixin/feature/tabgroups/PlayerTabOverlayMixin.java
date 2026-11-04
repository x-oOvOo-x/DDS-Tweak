package ddstweak.mixin.feature.tabgroups;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import ddstweak.config.Configs;
import ddstweak.feature.tabgroups.TabGroupLayout;
import ddstweak.integration.dds.playerinventory.PlayerInventoryTargetType;
import ddstweak.integration.provider.PlayerIdentityResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.client.multiplayer.PlayerInfo;
//#if MC >= 260100
//$$ import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.Comparator;
import java.util.List;

/** Client-only Tab grouping, without fake PlayerInfo rows or rewritten packets. */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private Component footer;
    @Unique private int[] ddstweak$offsets = new int[0];
    @Unique private int ddstweak$index;
    @Unique private int ddstweak$maxOffset;
    @Unique private boolean ddstweak$rowPose;
    @Unique private boolean ddstweak$footerPose;

    @ModifyArg(method = "getPlayerInfos",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"),
            index = 0, require = 1)
    private Comparator<PlayerInfo> ddstweak$groupComparator(Comparator<PlayerInfo> vanilla) {
        if (!Configs.Visual.TAB_FAKE_PLAYERS_LAST.getBooleanValue()) return vanilla;
        return Comparator.comparingInt(PlayerTabOverlayMixin::ddstweak$group).thenComparing(vanilla);
    }

    @Unique private static int ddstweak$group(PlayerInfo info) {
        boolean fake = PlayerIdentityResolver.resolve(info.getProfile().id()) == PlayerInventoryTargetType.FAKE;
        return TabGroupLayout.group(fake, info.getGameMode() == GameType.SPECTATOR);
    }

    //#if MC >= 260100
    //$$ @WrapOperation(method = "extractRenderState",
    //#else
    @WrapOperation(method = "render",
    //#endif
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;getPlayerInfos()Ljava/util/List;"),
            require = 1)
    private List<PlayerInfo> ddstweak$capturePlayers(PlayerTabOverlay overlay, Operation<List<PlayerInfo>> original) {
        List<PlayerInfo> infos = original.call(overlay);
        ddstweak$index = 0;
        ddstweak$maxOffset = 0;
        ddstweak$offsets = new int[0];
        ddstweak$rowPose = false;
        ddstweak$footerPose = false;
        if (Configs.Visual.TAB_FAKE_PLAYERS_LAST.getBooleanValue() && infos.size() > 1) {
            int[] groups = new int[infos.size()];
            for (int i = 0; i < infos.size(); i++) groups[i] = ddstweak$group(infos.get(i));
            ddstweak$offsets = TabGroupLayout.offsets(groups);
            ddstweak$maxOffset = TabGroupLayout.maximumOffset(ddstweak$offsets);
        }
        return infos;
    }

    /** Extend the Tab's surrounding background to contain the tallest column. */
    //#if MC >= 260100
    //$$ @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V", ordinal = 1), require = 1)
    //$$ private void ddstweak$background(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
    //#else
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1), require = 1)
    private void ddstweak$background(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
    //#endif
        // Preserve all vanilla entries when the GUI cannot accommodate extra spacing.
        if (ddstweak$maxOffset > 0) {
            int footerLines = footer == null ? 0 : minecraft.font.split(footer, graphics.guiWidth() - 50).size();
            if (!TabGroupLayout.fitsVertically(y2, footerLines, graphics.guiHeight(), ddstweak$maxOffset)) {
                ddstweak$offsets = new int[0];
                ddstweak$maxOffset = 0;
            }
        }
        original.call(graphics, x1, y1, x2, y2 + ddstweak$maxOffset, color);
    }

    /** A per-row pose keeps background, head, text, score and ping perfectly aligned. */
    //#if MC >= 260100
    //$$ @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V", ordinal = 2), require = 1)
    //$$ private void ddstweak$row(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
    //#else
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 2), require = 1)
    private void ddstweak$row(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
    //#endif
        int offset = ddstweak$index < ddstweak$offsets.length ? ddstweak$offsets[ddstweak$index] : 0;
        ddstweak$index++;
        if (offset > 0) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(0, offset);
            ddstweak$rowPose = true;
        }
        original.call(graphics, x1, y1, x2, y2, color);
    }

    /** Ping is the last vanilla operation in a row. Pop even if drawing fails. */
    //#if MC >= 260100
    //$$ @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;extractPingIcon(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIILnet/minecraft/client/multiplayer/PlayerInfo;)V"), require = 1)
    //$$ private void ddstweak$endRow(PlayerTabOverlay overlay, GuiGraphicsExtractor graphics, int width, int x, int y,
    //$$                              PlayerInfo info, Operation<Void> original) {
    //#else
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;renderPingIcon(Lnet/minecraft/client/gui/GuiGraphics;IIILnet/minecraft/client/multiplayer/PlayerInfo;)V"), require = 1)
    private void ddstweak$endRow(PlayerTabOverlay overlay, GuiGraphics graphics, int width, int x, int y,
                                PlayerInfo info, Operation<Void> original) {
    //#endif
        try {
            original.call(overlay, graphics, width, x, y, info);
        } finally {
            if (ddstweak$rowPose) {
                graphics.pose().popMatrix();
                ddstweak$rowPose = false;
            }
        }
    }

    /** Footer content follows the extended Tab background. */
    //#if MC >= 260100
    //$$ @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V", ordinal = 3), require = 1)
    //$$ private void ddstweak$footer(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
    //#else
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 3), require = 1)
    private void ddstweak$footer(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
    //#endif
        if (ddstweak$maxOffset > 0) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(0, ddstweak$maxOffset);
            ddstweak$footerPose = true;
        }
        original.call(graphics, x1, y1, x2, y2, color);
    }

    /**
     * Render-state extraction can fail between row background and Ping.
     * Always restore pending transforms, including on exceptional exit.
     */
    //#if MC >= 260100
    //$$ @WrapMethod(method = "extractRenderState")
    //$$ private void ddstweak$restorePose(GuiGraphicsExtractor graphics, int width,
    //$$                                   net.minecraft.world.scores.Scoreboard scoreboard,
    //$$                                   net.minecraft.world.scores.Objective objective, Operation<Void> original) {
    //#else
    @WrapMethod(method = "render")
    private void ddstweak$restorePose(GuiGraphics graphics, int width,
                                     net.minecraft.world.scores.Scoreboard scoreboard,
                                     net.minecraft.world.scores.Objective objective, Operation<Void> original) {
    //#endif
        try {
            original.call(graphics, width, scoreboard, objective);
        } finally {
            if (ddstweak$footerPose) {
                graphics.pose().popMatrix();
                ddstweak$footerPose = false;
            }
            if (ddstweak$rowPose) {
                graphics.pose().popMatrix();
                ddstweak$rowPose = false;
            }
        }
    }
}
