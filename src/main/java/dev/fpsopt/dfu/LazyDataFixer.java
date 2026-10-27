package dev.fpsopt.dfu;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import dev.fpsopt.FpsOptMod;

import java.util.function.Supplier;

/**
 * Gerçek {@link DataFixer}'ı ilk gerçek ihtiyaç anına kadar oluşturmayan sarmalayıcı
 * (LazyDFU'daki fikre benzer).
 *
 * <p>Vanilla açılışta yüzlerce şemayı ve düzeltmeyi hemen kurar; bu yüzlerce ms - birkaç sn sürer
 * ve çoğu oyunda (güncel sürüm dünyaları) hiç kullanılmaz. Burada {@code version >= newVersion}
 * ise DFU'nun zaten girdiyi aynen döndürdüğü bilindiği için gerçek fixer hiç kurulmaz.
 * Eski bir dünya yüklenirse (version &lt; newVersion) fixer o anda kurulur; davranış aynıdır.</p>
 */
public final class LazyDataFixer implements DataFixer {

    private final Supplier<DataFixer> factory;
    private volatile DataFixer delegate;

    public LazyDataFixer(Supplier<DataFixer> factory) {
        this.factory = factory;
    }

    private DataFixer delegate() {
        DataFixer d = this.delegate;
        if (d == null) {
            synchronized (this) {
                d = this.delegate;
                if (d == null) {
                    long start = System.nanoTime();
                    d = this.factory.get();
                    this.delegate = d;
                    FpsOptMod.LOGGER.info("DataFixerUpper tembel olarak başlatıldı ({} ms).",
                            (System.nanoTime() - start) / 1_000_000L);
                }
            }
        }
        return d;
    }

    @Override
    public <T> Dynamic<T> update(DSL.TypeReference type, Dynamic<T> input, int version, int newVersion) {
        if (version >= newVersion) {
            // Vanilla DataFixerUpper da bu durumda girdiyi değiştirmeden döndürür.
            return input;
        }
        return delegate().update(type, input, version, newVersion);
    }

    @Override
    public Schema getSchema(int key) {
        return delegate().getSchema(key);
    }
}
