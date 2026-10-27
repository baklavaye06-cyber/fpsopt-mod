package dev.fpsopt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mod ayarları. Dosya: {@code config/fpsopt.json}.
 *
 * <p>Bu sınıf bilerek Minecraft sınıflarına BAĞIMLI DEĞİLDİR; çünkü Mixin eklentisi
 * (FpsOptMixinPlugin) oyun sınıfları yüklenmeden önce ayarları okuyabilmelidir.
 * Alanlar public ve düz tiplerdir: Mixin'ler her çağrıda ucuzca okuyabilsin diye.</p>
 */
public final class FpsOptConfig {

    // ---------------- Parçacıklar ----------------
    /** Uzak / fazla parçacıkları kırp. */
    public boolean particleCulling = true;
    /** Kameradan bu mesafeden (blok) uzaktaki parçacıklar hiç oluşturulmaz. */
    public double particleMaxDistance = 48.0;
    /** Aynı anda yaşayabilecek toplam parçacık sınırı (vanilla: tür başına 16384). */
    public int particleMaxCount = 4000;

    // ---------------- Varlık / blok varlığı çizimi ----------------
    /** Belirli mesafeden uzaktaki varlıkların (mob, eşya vb.) çizimini atla. */
    public boolean entityDistanceCulling = true;
    public double entityMaxRenderDistance = 48.0;
    /** Sandık, tabela, kafa vb. blok varlıkları için mesafe kırpması. */
    public boolean blockEntityDistanceCulling = true;
    public double blockEntityMaxRenderDistance = 48.0;

    // ---------------- Doku animasyonları ----------------
    /** Su/lav gibi animasyonlu dokuları her N. tikte güncelle (görsel olarak yavaşlar). */
    public boolean textureAnimationThrottle = false;
    public int textureAnimationDivider = 2;

    // ---------------- Mob yapay zekası (sunucu / tek oyunculu) ----------------
    /** Oyuncudan uzak mobların yapay zekasını her N. tikte bir çalıştır. */
    public boolean mobAiThrottle = true;
    /** Bu mesafeden (blok) uzak mobların AI'ı kısılır. */
    public double mobAiThrottleDistance = 64.0;
    /** Uzak mobların AI'ı kaç tikte bir çalışsın. */
    public int mobAiThrottleInterval = 4;
    /** Köylü, evcil hayvan ve demir golemleri kısıtlamadan muaf tut (önerilir). */
    public boolean mobAiSafeMode = true;

    // ---------------- DFU ----------------
    /** DataFixerUpper'ı tembel başlat (LazyDFU benzeri). Açılış süresini kısaltır. */
    public boolean lazyDfu = true;

    // ---------------- Bellek ----------------
    /** Sunucudan/dünyadan çıkınca ayrı bir iş parçacığında GC iste. */
    public boolean gcOnDisconnect = true;

    // =====================================================================

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static FpsOptConfig instance = new FpsOptConfig();
    private static boolean loaded;

    public static FpsOptConfig get() {
        return instance;
    }

    /** Ayarları diskten yükler (yoksa varsayılanları yazar). Birden çok kez çağrılabilir. */
    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;

        Path file = FabricLoader.getInstance().getConfigDir().resolve("fpsopt.json");
        FpsOptConfig cfg = null;

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                cfg = GSON.fromJson(reader, FpsOptConfig.class);
            } catch (Exception e) {
                // Bozuk dosya oyunu çökertmesin; varsayılanlara dön.
                System.err.println("[FPS Optimizer] Ayar dosyası okunamadı, varsayılanlar kullanılıyor: " + e);
            }
        }

        if (cfg == null) {
            cfg = new FpsOptConfig();
        }
        cfg.sanitize();
        instance = cfg;

        // Eksik alanlar (yeni sürümlerde eklenenler) dosyaya yazılsın.
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(cfg, writer);
            }
        } catch (IOException e) {
            System.err.println("[FPS Optimizer] Ayar dosyası yazılamadı: " + e);
        }
    }

    /** Mantıksız değerleri güvenli aralığa çeker. */
    private void sanitize() {
        particleMaxDistance = clamp(particleMaxDistance, 8.0, 512.0);
        particleMaxCount = (int) clamp(particleMaxCount, 256, 16384);
        entityMaxRenderDistance = clamp(entityMaxRenderDistance, 16.0, 512.0);
        blockEntityMaxRenderDistance = clamp(blockEntityMaxRenderDistance, 16.0, 512.0);
        textureAnimationDivider = (int) clamp(textureAnimationDivider, 1, 20);
        mobAiThrottleDistance = clamp(mobAiThrottleDistance, 32.0, 512.0);
        mobAiThrottleInterval = (int) clamp(mobAiThrottleInterval, 1, 20);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
