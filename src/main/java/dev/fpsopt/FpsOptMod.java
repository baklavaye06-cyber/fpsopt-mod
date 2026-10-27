package dev.fpsopt;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Ortak (client + server) giriş noktası. */
public class FpsOptMod implements ModInitializer {

    public static final String MOD_ID = "fpsopt";
    public static final Logger LOGGER = LogManager.getLogger("FPS Optimizer");

    @Override
    public void onInitialize() {
        // Ayarlar Mixin eklentisi tarafından zaten yüklenmiş olur; load() tekrar çağrılsa da zararsızdır.
        FpsOptConfig.load();
        FpsOptConfig c = FpsOptConfig.get();
        LOGGER.info("FPS Optimizer yüklendi. particles={}, entityCull={}, blockEntityCull={}, mobAi={}, lazyDfu={}",
                c.particleCulling, c.entityDistanceCulling, c.blockEntityDistanceCulling, c.mobAiThrottle, c.lazyDfu);
    }
}
