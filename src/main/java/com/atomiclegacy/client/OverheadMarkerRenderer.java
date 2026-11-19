package com.atomiclegacy.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.items.LegacyArmorModParity;
import com.atomiclegacy.network.MarkerPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * [parity b2: assembly markers] RenderOverhead.renderMarkers + drawTag (called from
 * ModEventHandlerClient#onRenderWorldLastEvent while HbmPlayerProps.enableHUD is set): every marker is a block
 * outline drawn as GL_LINES without depth test in the marker colour; its tag (label, plus the distance in metres
 * while the player looks at it) is pulled to at most 16 blocks from the eye and only drawn within 100 blocks.
 * Markers leave when their time is up or the player is farther away than their distance.
 */
@EventBusSubscriber(modid = AtomicLegacy.MOD_ID, value = Dist.CLIENT)
public final class OverheadMarkerRenderer {
    public static final Map<BlockPos, Marker> queuedMarkers = new HashMap<>();
    private static final Map<BlockPos, Marker> markers = new HashMap<>();

    private OverheadMarkerRenderer() {
    }

    public static void queue(MarkerPayload payload) {
        queuedMarkers.put(payload.pos(), new Marker(payload.color())
                .setDist(payload.dist())
                .setExpire(payload.expires() > 0 ? System.currentTimeMillis() + payload.expires() : 0)
                .withLabel(payload.label().isEmpty() ? null : payload.label()));
    }

    public static int activeMarkers() {
        return markers.size() + queuedMarkers.size();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        queuedMarkers.clear();
        markers.clear();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !LegacyArmorModParity.clientHud) return;

        markers.putAll(queuedMarkers);
        queuedMarkers.clear();

        if (markers.isEmpty()) return;

        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        // the original measures from the interpolated player position, not from the camera
        Vec3 playerPos = player.getPosition(partialTicks);
        double x = playerPos.x;
        double y = playerPos.y;
        double z = playerPos.z;
        Vec3 camera = event.getCamera().getPosition();

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = buffers.getBuffer(Types.MARKER_LINES);
        Matrix4f pose = poseStack.last().pose();

        Iterator<Map.Entry<BlockPos, Marker>> it = markers.entrySet().iterator();
        List<Map.Entry<BlockPos, Marker>> tagList = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Marker> entry = it.next();
            BlockPos pos = entry.getKey();
            Marker marker = entry.getValue();

            float minX = (float) (pos.getX() + marker.minX - camera.x);
            float minY = (float) (pos.getY() + marker.minY - camera.y);
            float minZ = (float) (pos.getZ() + marker.minZ - camera.z);
            float maxX = (float) (pos.getX() + marker.maxX - camera.x);
            float maxY = (float) (pos.getY() + marker.maxY - camera.y);
            float maxZ = (float) (pos.getZ() + marker.maxZ - camera.z);
            float r = ((marker.color >> 16) & 0xFF) / 255F;
            float g = ((marker.color >> 8) & 0xFF) / 255F;
            float b = (marker.color & 0xFF) / 255F;

            float[][] v = {
                    {minX, maxY, minZ}, {minX, minY, minZ},
                    {minX, maxY, minZ}, {maxX, maxY, minZ},
                    {maxX, maxY, minZ}, {maxX, minY, minZ},
                    {minX, minY, minZ}, {maxX, minY, minZ},
                    {maxX, minY, minZ}, {maxX, minY, maxZ},
                    {maxX, maxY, maxZ}, {maxX, maxY, minZ},
                    {maxX, maxY, maxZ}, {maxX, minY, maxZ},
                    {minX, maxY, minZ}, {minX, maxY, maxZ},
                    {minX, maxY, maxZ}, {minX, minY, maxZ},
                    {minX, maxY, maxZ}, {maxX, maxY, maxZ},
                    {minX, minY, maxZ}, {maxX, minY, maxZ},
                    {minX, minY, minZ}, {minX, minY, maxZ}
            };
            for (float[] p : v) buffer.addVertex(pose, p[0], p[1], p[2]).setColor(r, g, b, 1F);

            tagList.add(entry);

            if (marker.expire > 0 && System.currentTimeMillis() > marker.expire) {
                it.remove();
            } else if (marker.maxDist > 0) {
                double aX = pos.getX() + (marker.maxX - marker.minX) / 2D;
                double aY = pos.getY() + (marker.maxY - marker.minY) / 2D;
                double aZ = pos.getZ() + (marker.maxZ - marker.minZ) / 2D;
                if (new Vec3(x - aX, y - aY, z - aZ).length() > marker.maxDist) {
                    it.remove();
                }
            }
        }
        buffers.endBatch(Types.MARKER_LINES);

        Font font = mc.font;
        Vec3 look = player.getViewVector(1.0F);
        for (Map.Entry<BlockPos, Marker> entry : tagList) {
            BlockPos pos = entry.getKey();
            Marker marker = entry.getValue();

            double aX = pos.getX() + (marker.maxX - marker.minX) / 2D;
            double aY = pos.getY() + (marker.maxY - marker.minY) / 2D;
            double aZ = pos.getZ() + (marker.maxZ - marker.minZ) / 2D;
            Vec3 vec = new Vec3(aX - x, aY - y, aZ - z);
            double len = vec.lengthSqr();
            double sqrt = Math.sqrt(len);
            double mult = Math.min(sqrt, 16D);
            if (sqrt > 0) vec = vec.scale(mult / sqrt);
            Vec3 diff = vec.normalize();
            String label = marker.label;
            if (label == null) {
                label = "";
            }

            if (Math.abs(look.x - diff.x) + Math.abs(look.y - diff.y) + Math.abs(look.z - diff.z) < 0.15) {
                label += (!label.isEmpty() ? " " : "") + ((int) sqrt) + "m";
            }

            // drawTag(1F, len, label, vec, 100, true, color, color)
            if (!label.isEmpty() && len <= 100D * 100D) {
                float scale = 0.016666668F * 1.6F;
                poseStack.pushPose();
                // vec is relative to the player position; the pose stack is relative to the camera
                poseStack.translate(vec.x + x - camera.x, vec.y + 1F + y - camera.y, vec.z + z - camera.z);
                poseStack.mulPose(event.getCamera().rotation());
                poseStack.scale(scale, -scale, scale);
                Matrix4f matrix = poseStack.last().pose();
                int color = 0xFF000000 | marker.color;
                float textX = -font.width(label) / 2F;
                font.drawInBatch(label, textX, 0, color, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, 0x40000000, LightTexture.FULL_BRIGHT);
                font.drawInBatch(label, textX, 0, color, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
                poseStack.popPose();
            }
        }
        buffers.endBatch();
    }

    public static final class Marker {
        double minX = 0;
        double minY = 0;
        double minZ = 0;
        double maxX = 1;
        double maxY = 1;
        double maxZ = 1;

        final int color;
        String label;

        long expire;
        double maxDist;

        public Marker(int color) {
            this.color = color;
        }

        public Marker setExpire(long expire) {
            this.expire = expire;
            return this;
        }

        public Marker setDist(double maxDist) {
            this.maxDist = maxDist;
            return this;
        }

        public Marker withLabel(String label) {
            this.label = label;
            return this;
        }
    }

    private static final class Types extends RenderType {
        /** GL_LINES, GL_TEXTURE_2D/GL_LIGHTING/GL_DEPTH_TEST off, blend 770/771, default line width 1. */
        static final RenderType MARKER_LINES = create("hbm_overhead_marker_lines",
                DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES, 1536, false, false,
                CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(1.0D)))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));

        private Types(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                      boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }
    }
}
