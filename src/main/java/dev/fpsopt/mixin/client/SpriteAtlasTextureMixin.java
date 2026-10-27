package dev.fpsopt.mixin.client;

import dev.fpsopt.FpsOptConfig;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DOKU ANİMASYONU KISITLAMASI (varsayılan: KAPALI)
 *
 * <p>Su, lav, ateş, portal gibi animasyonlu dokular her kare GPU'ya yeniden yüklenir.
 * Bu Mixin, atlas güncellemesini her {@code textureAnimationDivider}. çağrıda bir çalıştırır.
 * Görsel bedeli: animasyonlar o oranda yavaşlar. Bu yüzden varsayılan olarak kapalıdır.
 * Sodium yüklüyse otomatik devre dışı kalır (Sodium zaten görünmeyen dokuları atlar).</p>
 */
@Mixin(SpriteAtlasTexture.class)
public abstract class SpriteAtlasTextureMixin {

    /** Atlas başına sayaç (blok atlası, parçacık atlası vb. ayrı ayrı sayılır). */
    @Unique
    private int fpsopt$tickCounter;

    @Inject(method = "tickAnimatedSprites", at = @At("HEAD"), cancellable = true)
    private void fpsopt$throttleAnimations(CallbackInfo ci) {
        int divider = FpsOptConfig.get().textureAnimationDivider;
        if (divider > 1 && (this.fpsopt$tickCounter++ % divider) != 0) {
            ci.cancel();
        }
    }
}
