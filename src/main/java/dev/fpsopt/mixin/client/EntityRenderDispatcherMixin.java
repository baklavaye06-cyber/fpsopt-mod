package dev.fpsopt.mixin.client;

import dev.fpsopt.FpsOptConfig;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * VARLIK MESAFE KIRPMASI (Entity Culling)
 *
 * <p>Vanilla zaten frustum (görüş piramidi) kırpması yapar ve varlığın boyutuna bağlı bir
 * mesafe sınırı uygular; ancak bu sınır büyük mobların çoğu için ~64+ bloktur. Burada
 * daha sıkı, ayarlanabilir bir sınır uyguluyoruz: {@code entityMaxRenderDistance} dışındaki
 * varlıklar için model, gölge, isim etiketi ve ekipman çizimi tamamen atlanır.</p>
 *
 * <p>Muafiyetler: oyuncular ve büyük varlıklar (ortalama kenar uzunluğu &ge; 2.5 blok;
 * ejderha, ghast, boss vb.), çünkü bunlar çok uzaktan da görülür.</p>
 *
 * <p>Parametreler {@code x, y, z} kamera konumudur (vanilla'nın kendi çağrısından gelir),
 * dolayısıyla ek nesne tahsisi yapılmaz.</p>
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(
            method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void fpsopt$distanceCull(Entity entity, Frustum frustum, double x, double y, double z,
                                     CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof PlayerEntity) {
            return;
        }

        double dx = entity.getX() - x;
        double dy = entity.getY() - y;
        double dz = entity.getZ() - z;
        double max = FpsOptConfig.get().entityMaxRenderDistance;

        if (dx * dx + dy * dy + dz * dz <= max * max) {
            return;
        }
        if (entity.getBoundingBox().getAverageSideLength() >= 2.5D) {
            return;
        }
        cir.setReturnValue(false);
    }
}
