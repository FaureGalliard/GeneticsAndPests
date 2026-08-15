package com.fauregalliard.geneticsandpests.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.fauregalliard.geneticsandpests.GeneticsAndPests;
import com.fauregalliard.geneticsandpests.genetics.CropGenes;
import com.fauregalliard.geneticsandpests.genetics.Disease;
import com.fauregalliard.geneticsandpests.genetics.PlantGrowth;
import com.fauregalliard.geneticsandpests.genetics.PlantState;
import com.fauregalliard.geneticsandpests.registry.ModAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Paints a stain on the ground under every infected plant.
 *
 * <p>Particles alone turned out to be too easy to miss — you have to already be looking at the
 * plant to notice them. A stain on the soil is what makes an outbreak legible from across a field.
 *
 * <p>There is one texture per disease and none per plant, which is the whole point: the decal is
 * drawn on whatever the plant is rooted in, so wheat stains its farmland and cocoa stains the log
 * behind it, and a crop from another mod needs no art of its own to take part.
 */
@EventBusSubscriber(modid = GeneticsAndPests.MODID, value = Dist.CLIENT)
public final class DiseaseStainRenderer {
    /** How far out, in chunks, stains are drawn. */
    private static final int CHUNK_RADIUS = 3;

    /** Lifts the decal off the surface so it does not fight with it for depth. */
    private static final double LIFT = 0.005D;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        PoseStack pose = event.getPoseStack();
        if (level == null || pose == null) {
            return;
        }

        Vec3 camera = minecraft.gameRenderer.getMainCamera().position();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        ChunkPos centre = new ChunkPos(BlockPos.containing(camera));

        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(centre.x + dx, centre.z + dz, false);
                if (chunk == null) {
                    continue;
                }

                CropGenes stored = chunk.getExistingDataOrNull(ModAttachments.CROP_GENES);
                if (stored == null || stored.isEmpty()) {
                    continue;
                }

                for (BlockPos pos : stored.positions()) {
                    PlantState plant = stored.get(pos);
                    Disease disease = plant == null ? null : plant.diseaseOrNull();
                    if (disease != null) {
                        drawStain(level, pose, buffers, camera, pos, disease);
                    }
                }
            }
        }

        buffers.endBatch();
    }

    private static void drawStain(ClientLevel level, PoseStack pose, MultiBufferSource buffers,
                                  Vec3 camera, BlockPos pos, Disease disease) {
        BlockState state = level.getBlockState(pos);
        Direction support = PlantGrowth.supportDirection(state);
        Direction.Axis axis = support.getAxis();

        // Where the surface actually is. Farmland is an inch shy of a full block, so a decal drawn
        // at the block boundary would hover above the soil rather than sit on it.
        double plane = surfaceOffset(level, pos, state, support, axis);

        pose.pushPose();
        pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

        // No culling, so the winding of the quad cannot make it invisible from the side you happen
        // to be standing on.
        RenderType type = RenderTypes.entityCutoutNoCull(disease.stainTexture());
        VertexConsumer consumer = buffers.getBuffer(type);
        PoseStack.Pose entry = pose.last();
        int light = LevelRenderer.getLightColor(level, pos);

        float nx = -support.getStepX();
        float ny = -support.getStepY();
        float nz = -support.getStepZ();

        for (int corner = 0; corner < 4; corner++) {
            // The two axes that are not the support axis span the face; a and b walk its corners.
            float a = (corner == 1 || corner == 2) ? 1.0F : 0.0F;
            float b = corner >= 2 ? 1.0F : 0.0F;
            float flat = (float) plane;

            float x = switch (axis) {
                case X -> flat;
                default -> a;
            };
            float y = switch (axis) {
                case Y -> flat;
                case X -> a;
                case Z -> b;
            };
            float z = switch (axis) {
                case Z -> flat;
                default -> b;
            };

            consumer.addVertex(entry, x, y, z)
                    .setColor(255, 255, 255, 255)
                    .setUv(a, b)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(entry, nx, ny, nz);
        }

        pose.popPose();
    }

    /**
     * The coordinate along the support axis where the decal should sit, in the plant block's own
     * space: the top of the farmland below, or the face of the log behind.
     */
    private static double surfaceOffset(ClientLevel level, BlockPos pos, BlockState state,
                                        Direction support, Direction.Axis axis) {
        BlockPos supportPos = PlantGrowth.supportPos(level, pos, state);
        BlockState supportState = level.getBlockState(supportPos);
        var shape = supportState.getShape(level, supportPos);

        if (shape.isEmpty()) {
            return support.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? LIFT : 1.0D - LIFT;
        }

        return support.getAxisDirection() == Direction.AxisDirection.NEGATIVE
                ? -(1.0D - shape.max(axis)) + LIFT
                : 1.0D + shape.min(axis) - LIFT;
    }

    private DiseaseStainRenderer() {}
}
