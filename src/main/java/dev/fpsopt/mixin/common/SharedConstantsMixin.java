package dev.fpsopt.mixin.common;

import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Kayıt (registry) aşamasında DFU şemalarının zorla kurulmasını engeller.
 *
 * <p>Vanilla, {@code EntityType} ve {@code BlockEntityType} kayıtlarında
 * {@code Util.getChoiceType(...)} ile DFU şemasına başvurur; bu da {@link SchemasMixin}'deki
 * tembel yapıyı açılışta hemen tetikler. {@code useChoiceTypeRegistrations = false} bu aramaları
 * atlatır. (LazyDFU'nun da kullandığı yöntem. Veri kaydetme/yükleme davranışını etkilemez;
 * yalnızca geliştirici amaçlı tür doğrulamasını atlar.)</p>
 */
@Mixin(SharedConstants.class)
public abstract class SharedConstantsMixin {

    @Shadow
    public static boolean useChoiceTypeRegistrations;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void fpsopt$disableChoiceTypeRegistrations(CallbackInfo ci) {
        useChoiceTypeRegistrations = false;
    }
}
