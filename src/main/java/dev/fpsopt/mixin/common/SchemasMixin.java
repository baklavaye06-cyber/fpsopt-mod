package dev.fpsopt.mixin.common;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import dev.fpsopt.dfu.LazyDataFixer;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.Schemas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ForkJoinPool;

/**
 * DataFixerUpper (DFU) TEMBEL BAŞLATMA
 *
 * <p>Vanilla {@code Schemas.create()} açılışta tüm sürüm şemalarını ve düzeltmelerini kurar.
 * Burada bu iş, {@link LazyDataFixer} ile gerçekten bir DFU çağrısı gerektiğinde yapılır
 * (yalnızca eski sürüm bir dünya/veri yüklenirken). Güncel dünyalarda hiç yapılmaz.</p>
 */
@Mixin(Schemas.class)
public abstract class SchemasMixin {

    /** Vanilla'nın şemaları ve düzeltmeleri builder'a kaydeden özel metodu. */
    @Shadow
    private static void build(DataFixerBuilder builder) {
        throw new AssertionError("Mixin shadow");
    }

    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void fpsopt$lazyCreate(CallbackInfoReturnable<DataFixer> cir) {
        cir.setReturnValue(new LazyDataFixer(SchemasMixin::fpsopt$buildRealFixer));
    }

    private static DataFixer fpsopt$buildRealFixer() {
        DataFixerBuilder builder = new DataFixerBuilder(SharedConstants.getGameVersion().getWorldVersion());
        build(builder);
        return builder.build(ForkJoinPool.commonPool());
    }
}
