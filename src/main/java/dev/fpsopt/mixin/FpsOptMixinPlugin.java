package dev.fpsopt.mixin;

import dev.fpsopt.FpsOptConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Hangi Mixin'lerin uygulanacağına config'e ve yüklü modlara göre karar verir.
 *
 * <ul>
 *   <li>Kapatılan özelliğin Mixin'i hiç yüklenmez (sıfır ek yük).</li>
 *   <li>LazyDFU modu yüklüyse bizim DFU Mixin'leri atlanır (çift müdahale/çakışma olmasın).</li>
 *   <li>Sodium yüklüyse doku animasyonu Mixin'i atlanır (Sodium bunu zaten kendi yönetir).</li>
 * </ul>
 */
public final class FpsOptMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        // Oyun sınıfları yüklenmeden önce ayarlar hazır olmalı.
        FpsOptConfig.load();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        FpsOptConfig c = FpsOptConfig.get();
        FabricLoader loader = FabricLoader.getInstance();
        String name = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);

        switch (name) {
            case "ParticleManagerMixin":
                return c.particleCulling;
            case "EntityRenderDispatcherMixin":
                return c.entityDistanceCulling;
            case "BlockEntityRenderDispatcherMixin":
                return c.blockEntityDistanceCulling;
            case "SpriteAtlasTextureMixin":
                return c.textureAnimationThrottle && !loader.isModLoaded("sodium");
            case "MobEntityMixin":
                return c.mobAiThrottle;
            case "SchemasMixin":
            case "SharedConstantsMixin":
                return c.lazyDfu && !loader.isModLoaded("lazydfu");
            default:
                return true;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
