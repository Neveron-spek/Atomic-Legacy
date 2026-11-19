package com.atomiclegacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.tileentity.SoyuzLauncherBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * [parity P2: soyuz_launcher] RenderSoyuzLauncher + SoyuzLauncherPronter: six OBJ parts
 * (face culling off), tower and support swing 45 degrees open without a rocket and close
 * over the last 20 countdown ticks; the rocket (SoyuzPronter skin) sits 5 blocks up.
 */
public final class SoyuzLauncherBlockEntityRenderer implements LongRangeBlockEntityRenderer<SoyuzLauncherBlockEntity> {
    private static final ModelResourceLocation LEGS = MachineModelRenderer.standalone("machine/soyuz_launcher_legs");
    private static final ModelResourceLocation TABLE = MachineModelRenderer.standalone("machine/soyuz_launcher_table");
    private static final ModelResourceLocation TOWER_BASE = MachineModelRenderer.standalone("machine/soyuz_launcher_tower_base");
    private static final ModelResourceLocation TOWER = MachineModelRenderer.standalone("machine/soyuz_launcher_tower");
    private static final ModelResourceLocation SUPPORT_BASE = MachineModelRenderer.standalone("machine/soyuz_launcher_support_base");
    private static final ModelResourceLocation SUPPORT = MachineModelRenderer.standalone("machine/soyuz_launcher_support");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public SoyuzLauncherBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(LEGS);
        event.register(TABLE);
        event.register(TOWER_BASE);
        event.register(TOWER);
        event.register(SUPPORT_BASE);
        event.register(SUPPORT);
    }

    @Override
    public void render(SoyuzLauncherBlockEntity launcher, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5D, -4.0D, 0.5D);
        double open = 45.0D;
        int timer = 20;
        double rot = open;
        if (launcher.renderRocketType() >= 0) rot = 0;
        if (launcher.renderStarting() && launcher.renderCountdown() < timer) {
            rot = (timer - launcher.renderCountdown() + partialTick) * open / timer;
        }
        part(LEGS, pose, buffers, light, overlay);
        part(TABLE, pose, buffers, light, overlay);
        part(TOWER_BASE, pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0.0D, 5.5D, 5.5D);
        pose.mulPose(Axis.XP.rotationDegrees((float) rot));
        pose.translate(0.0D, -5.5D, -5.5D);
        part(TOWER, pose, buffers, light, overlay);
        pose.popPose();
        part(SUPPORT_BASE, pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0.0D, 5.5D, -6.5D);
        pose.mulPose(Axis.XN.rotationDegrees((float) rot));
        pose.translate(0.0D, -5.5D, 6.5D);
        part(SUPPORT, pose, buffers, light, overlay);
        pose.popPose();
        int type = launcher.renderRocketType();
        if (type >= 0 && type < SoyuzLaunchpadBlockEntityRenderer.SKINS.length) {
            pose.translate(0.0D, 5.0D, 0.0D);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SoyuzLaunchpadBlockEntityRenderer.SKINS[type]),
                    pose, buffers, RENDER_STATE, light, overlay);
        }
        pose.popPose();
    }

    private static void part(ModelResourceLocation model, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        MachineModelRenderer.renderUnculledCutoutNoCull(MachineModelRenderer.model(model), pose, buffers, RENDER_STATE, light, overlay);
    }

    @Override
    public AABB getRenderBoundingBox(SoyuzLauncherBlockEntity launcher) {
        // TileEntity.INFINITE_EXTENT_AABB in the reference; the rocket reaches ~50 blocks above the table.
        return new AABB(launcher.getBlockPos()).inflate(12.0D, 4.0D, 12.0D).expandTowards(0.0D, 60.0D, 0.0D);
    }
}
