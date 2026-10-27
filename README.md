# FPS Optimizer — Minecraft 1.16.5 (Fabric)

Parçacık, varlık ve blok varlığı mesafe kırpması; uzak moblar için yapay zeka kısıtlaması;
tembel DataFixerUpper başlatma. Sodium / Lithium / LazyDFU ile **birlikte çalışacak şekilde** tasarlandı.

> **Durum:** Bu proje kaynak koduyla teslim edilir. Geliştirme ortamında Maven depolarına
> (Fabric, Mojang) erişim olmadığı için `.jar` orada derlenemedi. Kaynak, sözdizimi açısından
> kontrol edildi; Yarn adlarına karşı derleme ilk `gradlew build` çalıştırmanızda doğrulanır
> (bkz. "Derleme sırasında dikkat edilecekler").

---

## 1. Gereksinimler

| Bileşen | Sürüm |
|---|---|
| Minecraft | **1.16.5** |
| Fabric Loader | 0.11.0 veya üstü |
| Fabric API | 0.42.0+1.16 (1.16.5 için son sürüm) |
| Oyunu çalıştırmak ve projeyi derlemek için Java | **Java 8** (32 bit de olur, örn. 1.8.0_xxx) |

Derleme araçları Java 8 ile çalışacak şekilde seçildi: Fabric Loom 0.8 + Gradle 6.9.4.

## 2. Derleme (adım adım)

1. Java 8 yüklü olmalı. Komut penceresinde `java -version` → `1.8.0_xxx` görmelisiniz.
2. Klasör yolunda Türkçe/özel karakter olmasın (örn. `C:\fpsopt-mod`).
3. Proje klasöründe komut penceresi açıp: `gradlew.bat build` (Linux/macOS: `./gradlew build`).
4. İlk çalıştırma Gradle 6.9.4'ü, Minecraft'ı ve eşlemeleri indirir (internet gerekir, birkaç dakika).
5. `gradle.properties` içinde bellek `-Xmx1G` ayarlıdır (32 bit için). Bellek hatası alırsanız `-Xmx768M` deneyin
   ya da 64 bit bir bilgisayarda derleyip yalnızca `build\libs\fpsopt-1.0.0.jar` dosyasını taşıyın.
6. Çıktı: `build\libs\fpsopt-1.0.0.jar` (`-sources.jar` ile biteni kullanmayın).

## 3. Kurulum

1. Fabric Loader ile 1.16.5 profili oluşturun.
2. `fabric-api-0.42.0+1.16.jar` dosyasını `mods` klasörüne koyun.
3. `fpsopt-1.0.0.jar` dosyasını aynı `mods` klasörüne koyun.
4. Oyunu açın. İlk açılışta `config/fpsopt.json` oluşur.
5. Ayar değiştirmek için oyunu kapatıp bu dosyayı düzenleyin ve yeniden açın.

Sunucuda da çalışır (çift taraflı); sunucuda yalnızca mob AI ve DFU özellikleri etkindir,
Fabric API gereklidir.

## 4. Ayarlar (`config/fpsopt.json`)

| Ayar | Varsayılan | Açıklama |
|---|---|---|
| `particleCulling` | `true` | Parçacık kırpmasını aç/kapat |
| `particleMaxDistance` | `48.0` | Kameradan bu uzaklıktaki (blok) parçacıklar oluşturulmaz |
| `particleMaxCount` | `4000` | Aynı anda yaşayan toplam parçacık sınırı |
| `entityDistanceCulling` | `true` | Uzak varlıkların çizimini atla |
| `entityMaxRenderDistance` | `48.0` | Varlık çizim mesafesi (blok) |
| `blockEntityDistanceCulling` | `true` | Uzak sandık/tabela vb. çizimini atla |
| `blockEntityMaxRenderDistance` | `48.0` | Blok varlığı çizim mesafesi |
| `textureAnimationThrottle` | `false` | Doku animasyonlarını seyrelt (animasyonlar yavaşlar) |
| `textureAnimationDivider` | `2` | Kaç çağrıda bir doku güncellensin |
| `mobAiThrottle` | `true` | Uzak mob AI kısıtlaması |
| `mobAiThrottleDistance` | `64.0` | Bu mesafeden uzak (hiçbir oyuncuya yakın değil) moblar kısılır |
| `mobAiThrottleInterval` | `4` | Kısılan mobların AI'ı kaç tikte bir çalışsın |
| `mobAiSafeMode` | `true` | Köylü, evcil hayvan, demir golemi muaf |
| `lazyDfu` | `true` | DFU'yu tembel başlat |
| `gcOnDisconnect` | `true` | Dünyadan çıkınca arka planda GC iste |

Değerler okunurken güvenli aralığa çekilir; bozuk dosya oyunu çökertmez, varsayılanlara dönülür.

## 5. Hangi Mixin ne yapıyor, FPS'e nasıl katkı sağlıyor?

| Mixin | Hedef | Ne yapar | Katkı |
|---|---|---|---|
| `ParticleManagerMixin` | `ParticleManager.addParticle(ParticleEffect, 6×double)` | Kameradan uzak parçacıklar için `null` döner, nesne hiç oluşturulmaz. **Havai fişek (FIREWORK) muaf**: vanilla bu parçacığı null kontrolü yapmadan kullanır, null NPE verirdi. | CPU (tik + çizim), GC (nesne tahsisi yok) |
| `ParticleManagerMixin` | `ParticleManager.addParticle(Particle)` | Toplam yaşayan parçacık `particleMaxCount`'a ulaşınca yenilerini kuyruğa almaz. | Patlama/yağmur/çiftlik anlarında FPS çöküşünü önler |
| `EntityRenderDispatcherMixin` | `shouldRender(...)` | Config mesafesinin ötesindeki varlıkların çizimini (model, gölge, etiket, ekipman) atlar. Oyuncular ve büyük varlıklar (≥ 2.5 blok ort. kenar: ejderha, ghast, boss) muaf. | CPU + GPU çağrı sayısı |
| `BlockEntityRenderDispatcherMixin` | `render(BlockEntity, float, MatrixStack, VertexConsumerProvider)` | Uzak sandık/tabela/kafa çizimini atlar. Beacon ve End Gateway muaf. | Depolama odası/market üslerinde belirgin |
| `SpriteAtlasTextureMixin` | `tickAnimatedSprites()` | (İsteğe bağlı) Doku animasyonu güncellemesini seyreltir. Sodium varsa otomatik devre dışı. | GPU doku yükleme bant genişliği |
| `MobEntityMixin` | `tickNewAi()` | Oyuncuya `mobAiThrottleDistance` bloktan uzak mobların AI'ını her N tikte bir çalıştırır. Yakınlık kontrolü mob başına saniyede 1 kez (id ile kademelendirilmiş). Hedefi olan, tasmalı, binekte/yolcu olan, boss ve (safe mode'da) köylü/evcil/golem muaf. | Sunucu MSPT, tek oyunculu FPS |
| `SchemasMixin` | `Schemas.create()` | DFU'yu `LazyDataFixer` ile sarar; gerçek kurulum yalnızca eski veri yüklenirken yapılır. | Açılış süresi, açılışta RAM |
| `SharedConstantsMixin` | `SharedConstants.<clinit>` | `useChoiceTypeRegistrations=false`: kayıt aşamasında DFU şemalarının zorla kurulmasını önler (LazyDFU ile aynı yöntem). | Açılış süresi |
| `FpsOptMixinPlugin` | — | Kapalı özelliklerin Mixin'ini hiç yüklemez; LazyDFU ve Sodium varsa ilgili Mixin'leri atlar (çakışma önleme). | Sıfır ek yük |

**Bellek notları:** Mod hiçbir statik koleksiyonda varlık/dünya referansı tutmaz; mob başına durum
(`fpsopt$farFromPlayers`) mobun kendi nesnesinde yaşar ve onunla birlikte toplanır, dolayısıyla mod
kendi tarafında sızıntı oluşturmaz. Yan ürün olarak, dünyadan çıkınca ayrı bir düşük öncelikli
iş parçacığında GC istenir. Bu **vanilla veya diğer modlardaki sızıntıları düzeltmez**.

## 6. Bilinen yan etkiler / dürüst sınırlar

- **Mob AI kısıtlaması davranışı değiştirir:** uzaktaki mobların hareketi ve despawn sayacı yavaşlar.
  Köylü ticaret salonu, mob çiftliği gibi sistemleri oyuncudan 64+ blok uzakta ve yüklü chunk'ta
  bırakıyorsanız çalışma hızı düşer. Sorun yaşarsanız `mobAiThrottle=false` yapın ya da mesafeyi artırın.
- **Mesafe kırpması** uzaktaki varlıkların "ortadan kaybolmasına" yol açar (varsayılan 48 blok).
  Beğenmezseniz mesafeleri artırın.
- **Frustum / occlusion kırpması:** Vanilla zaten frustum kırpması ve chunk görünürlük grafiği kullanır.
  Sodium'un yaptığı gibi tam bir render motoru yeniden yazımı bu modun kapsamı dışındadır; en iyi sonuç
  için Sodium + Lithium + bu modu birlikte kullanın.
- **FPS kazancı** sahneye çok bağlıdır (parçacık/varlık/sandık yoğunluğu). Kazanç rakamı vermiyoruz; çünkü
  ölçüm yapılmadı. Ölçmek için F3 ekranı veya Spark modunu önce/sonra karşılaştırın.

## 7. Derleme sırasında dikkat edilecekler

Mixin hedefleri Yarn 1.16.5 adlarıyla yazıldı. Bir ad farklıysa Mixin annotation işlemcisi derleme anında
hata verir; hata mesajı hangi metot/alanın bulunamadığını söyler. Doğrulanması en gerekenler:

1. `ParticleManager.particles` alanı (`@Shadow`)
2. `Schemas.build(DataFixerBuilder)` ve `Schemas.create()`
3. `SharedConstants.useChoiceTypeRegistrations`
4. `SpriteAtlasTexture.tickAnimatedSprites()`
5. `BlockEntityRenderDispatcher.render(...)` imzası

Bunlar Yarn eşlemeleri açıkken (`./gradlew genSources` sonrası IDE'de Ctrl+Click) ya da
<https://linkie.shedaniel.dev/mappings> adresinden kolayca bulunur; kodda yalnızca ilgili ad değiştirilir.

`fpsopt.mixins.json` içinde `"required": false` olduğundan, çalışma zamanında bir Mixin uygulanamazsa
oyun çökmez; `latest.log` içinde `Mixin ... FAILED` uyarısı görürsünüz ve o özellik devre dışı kalır.

## 8. Sorun giderme

| Belirti | Çözüm |
|---|---|
| Loom/Gradle sürüm uyumsuzluğu hatası | `gradle/wrapper/gradle-wrapper.properties` içindeki Gradle sürümünün 6.9.4 olduğundan emin olun |
| Oyun açılmıyor, `requires java` hatası | Başlatıcıda Java 8 (veya üstü) seçin |
| Parçacıklar/varlıklar kayboluyor | `particleMaxDistance`, `entityMaxRenderDistance` değerlerini artırın |
| Uzak mob çiftliği yavaşladı | `mobAiThrottle=false` |
| LazyDFU/Sodium ile çakışma | Mod bunları algılayıp ilgili Mixin'i atlar; yine sorun varsa `lazyDfu=false` / `textureAnimationThrottle=false` |
| Dünya yüklenirken DFU hatası | `lazyDfu=false` yapıp bildirin |

## 9. Proje yapısı

```
fpsopt-mod/
├─ build.gradle · gradle.properties · settings.gradle · gradlew(.bat) · gradle/wrapper/
└─ src/main/
   ├─ resources/ fabric.mod.json · fpsopt.mixins.json
   └─ java/dev/fpsopt/
      ├─ FpsOptMod.java · FpsOptConfig.java
      ├─ client/FpsOptClient.java
      ├─ dfu/LazyDataFixer.java
      └─ mixin/
         ├─ FpsOptMixinPlugin.java
         ├─ client/ ParticleManagerMixin · EntityRenderDispatcherMixin · BlockEntityRenderDispatcherMixin · SpriteAtlasTextureMixin
         └─ common/ MobEntityMixin · SchemasMixin · SharedConstantsMixin
```

Lisans: MIT.
