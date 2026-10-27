package dev.fpsopt.client;

import dev.fpsopt.FpsOptConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * İstemci giriş noktası.
 *
 * <p>Bellek yönetimi: Bu mod hiçbir statik koleksiyonda varlık/dünya referansı tutmaz
 * (mob başına durum, varlığın kendi alanında yaşar; varlık ile birlikte çöp toplanır).
 * Ek olarak dünyadan çıkıldığında, ana menüde oyuncu beklerken bir GC istenir; böylece
 * kapatılan dünyanın yığın belleği hızla geri kazanılır.</p>
 */
@Environment(EnvType.CLIENT)
public class FpsOptClient implements ClientModInitializer {

    /** Ardışık GC isteklerini (örn. hızlı bağlan-çık) sınırlamak için. */
    private static volatile long lastGcRequestMillis = 0L;
    private static final long MIN_GC_INTERVAL_MILLIS = 10_000L;

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (!FpsOptConfig.get().gcOnDisconnect) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastGcRequestMillis < MIN_GC_INTERVAL_MILLIS) {
                return;
            }
            lastGcRequestMillis = now;

            // Oyun iş parçacığını bloklamamak için ayrı, düşük öncelikli bir thread.
            Thread gcThread = new Thread(System::gc, "FPSOpt-GC");
            gcThread.setDaemon(true);
            gcThread.setPriority(Thread.MIN_PRIORITY);
            gcThread.start();
        });
    }
}
