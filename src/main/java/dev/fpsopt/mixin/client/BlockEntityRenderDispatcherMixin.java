package dev.fpsopt.mixin.client;

import dev.fpsopt.FpsOptConfig;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * BLOK VARLIĞI MESAFE KIRPMASI
 *
 * <p>Sandık, tabela, kafa, ilan panosu, spawner gibi blok varlıklarının çizimi, her biri
 * için ayrı bir renderer çağrısı gerektirir. Tabela / sandık yoğun üslerde (depolama odaları,
 * market sistemleri) bu ciddi CPU yüküdür. Vanilla sınırı 64 bloktur; burada ayarlanabilir
 * ({@code blockEntityMaxRenderDistance}) daha kısa bir sınır uygulanır.</p>
 *
 * <p>Muaf: Beacon ve End Gateway (ışınları çok uzaktan görülmelidir).</p>
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

    @Inject(
            method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void fpsopt$distanceCull(BlockEntity blockEntity, float tickDelta, MatrixStack matrices,
                                     VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (blockEntity instanceof BeaconBlockEntity || blockEntity instanceof EndGatewayBlockEntity) {
            return;
        }

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        if (camera == null || !camera.isReady()) {
            return;
        }

        Vec3d cam = camera.getPos();
        BlockPos pos = blockEntity.getPos();
        double dx = pos.getX() + 0.5D - cam.x;
        double dy = pos.getY() + 0.5D - cam.y;
        double dz = pos.getZ() + 0.5D - cam.z;
        double max = FpsOptConfig.get().blockEntityMaxRenderDistance;

        if (dx * dx + dy * dy + dz * dz > max * max) {
            ci.cancel();
        }
    }
}
