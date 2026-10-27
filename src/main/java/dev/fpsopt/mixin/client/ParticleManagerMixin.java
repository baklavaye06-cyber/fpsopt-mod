package dev.fpsopt.mixin.client;

import dev.fpsopt.FpsOptConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Queue;

/**
 * PARÇACIK KIRPMA
 *
 * <p>Vanilla, ekranda hiç görünmeyecek kadar uzakta olsa bile her parçacığı oluşturur,
 * her tik günceller ve çizim kuyruğuna ekler. İki katmanlı kırpma yapıyoruz:</p>
 * <ol>
 *   <li><b>Mesafe:</b> Kameradan {@code particleMaxDistance} bloktan uzak parçacıklar
 *       oluşturulmadan reddedilir. Böylece nesne tahsisi (GC baskısı), tik ve çizim maliyeti
 *       tamamen kaybolur.</li>
 *   <li><b>Toplam sınır:</b> Yaşayan parçacık sayısı {@code particleMaxCount}'a ulaşınca yenileri
 *       kuyruğa alınmaz (patlama / yağmur / büyük çiftlik yoğunluklarında FPS çöküşünü önler).</li>
 * </ol>
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    @Shadow
    @Final
    private Map<ParticleTextureSheet, Queue<Particle>> particles;

    /**
     * Ana parçacık oluşturma noktası. Uzaktaysa {@code null} döndürürüz (vanilla çağıranlar
     * {@code @Nullable} sonucu zaten yönetir).
     *
     * <p>İSTİSNA: FIREWORK parçacığı. Vanilla havai fişek kodu dönen parçacığı null kontrolü
     * yapmadan kullanır; null dönmek NullPointerException'a yol açardı.</p>
     */
    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void fpsopt$cullDistantParticle(ParticleEffect parameters, double x, double y, double z,
                                            double velocityX, double velocityY, double velocityZ,
                                            CallbackInfoReturnable<Particle> cir) {
        FpsOptConfig cfg = FpsOptConfig.get();
        if (parameters.getType() == ParticleTypes.FIREWORK) {
            return;
        }

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        if (camera == null || !camera.isReady()) {
            return;
        }

        Vec3d cam = camera.getPos();
        double dx = x - cam.x;
        double dy = y - cam.y;
        double dz = z - cam.z;
        double max = cfg.particleMaxDistance;

        if (dx * dx + dy * dy + dz * dz > max * max) {
            cir.setReturnValue(null);
        }
    }

    /**
     * Kuyruğa ekleme noktası: toplam parçacık sınırı. Dönüş değeri {@code void} olduğundan
     * burada iptal etmek güvenlidir (çağıran kod parçacığı kullanmaya devam edebilir).
     */
    @Inject(
            method = "addParticle(Lnet/minecraft/client/particle/Particle;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void fpsopt$capParticleCount(Particle particle, CallbackInfo ci) {
        int total = 0;
        for (Queue<Particle> queue : this.particles.values()) {
            total += queue.size();
        }
        if (total >= FpsOptConfig.get().particleMaxCount) {
            ci.cancel();
        }
    }
}
