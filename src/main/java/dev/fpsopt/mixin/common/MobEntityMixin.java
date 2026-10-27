package dev.fpsopt.mixin.common;

import dev.fpsopt.FpsOptConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * UZAK MOB YAPAY ZEKASI KISITLAMASI (Entity Tick / AI Optimization)
 *
 * Oyuncuya mobAiThrottleDistance blogundan uzak mobların AI'ı
 * yalnızca her mobAiThrottleInterval. tikte bir çalıştırılır.
 * Hedefi olanlar, tasmalılar, binek/yolcular, Ender Dragon, Wither ve
 * (mobAiSafeMode açıkken) köylüler, evcil hayvanlar, demir golemler muaftır.
 */
@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity {

    /** Son yakınlık kontrolünün sonucu; mobun kendi alanında yaşar, statik tutulmaz. */
    @Unique
    private boolean fpsopt$farFromPlayers;

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tickNewAi", at = @At("HEAD"), cancellable = true)
    private void fpsopt$throttleDistantAi(CallbackInfo ci) {
        if (this.world.isClient) {
            return;
        }

        FpsOptConfig cfg = FpsOptConfig.get();

        // Pahalı oyuncu aramasını mob başına saniyede bir yap (id ile kademelendirilmiş).
        if (((this.age + this.getEntityId()) % 20) == 0) {
            this.fpsopt$farFromPlayers = this.world.getClosestPlayer(this, cfg.mobAiThrottleDistance) == null;
        }
        if (!this.fpsopt$farFromPlayers) {
            return;
        }

        MobEntity self = (MobEntity) (Object) this;
        if (self.getTarget() != null || self.isLeashed() || self.hasPassengers() || self.hasVehicle()) {
            return;
        }

        Object o = this;
        if (o instanceof EnderDragonEntity || o instanceof WitherEntity) {
            return;
        }
        if (cfg.mobAiSafeMode
                && (o instanceof VillagerEntity || o instanceof TameableEntity || o instanceof IronGolemEntity)) {
            return;
        }

        if (((this.age + this.getEntityId()) % cfg.mobAiThrottleInterval) != 0) {
            ci.cancel();
        }
    }
}
