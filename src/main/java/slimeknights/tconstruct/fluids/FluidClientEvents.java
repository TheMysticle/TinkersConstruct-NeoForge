package slimeknights.tconstruct.fluids;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BucketItem;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;

@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
public class FluidClientEvents extends ClientEventBase {
  private static final java.util.concurrent.ConcurrentHashMap<net.minecraft.resources.ResourceLocation, Integer> COLOR_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

  private static int getFluidColor(net.minecraft.world.level.material.Fluid fluid) {
      net.minecraft.resources.ResourceLocation still = IClientFluidTypeExtensions.of(fluid).getStillTexture();
      if (still == null) return -1;
      return COLOR_CACHE.computeIfAbsent(still, s -> {
          net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = net.minecraft.client.Minecraft.getInstance().getModelManager().getAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).getSprite(s);
          //noinspection ConstantValue
          if (sprite == null || sprite.contents().name() == net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()) return -1;
          float r = 0, g = 0, b = 0;
          float count = 0;
          float[] hsb = new float[3];
          try {
              net.minecraft.client.renderer.texture.SpriteContents contents = sprite.contents();
              for (int x = 0; x < contents.width(); x++) {
                  for (int y = 0; y < contents.height(); y++) {
                      int argb = sprite.getPixelRGBA(0, x, y);
                      int ca = argb >> 24 & 0xFF;
                      if (ca > 0x7F) {
                          int cr = argb & 0xFF;
                          int cg = (argb >> 8) & 0xFF;
                          int cb = (argb >> 16) & 0xFF;
                          if (Math.max(cr, Math.max(cg, cb)) > 0x1F) {
                              java.awt.Color.RGBtoHSB(cr, cg, cb, hsb);
                              float weight = hsb[1] + 0.1f;
                              r += cr * weight;
                              g += cg * weight;
                              b += cb * weight;
                              count += weight;
                          }
                      }
                  }
              }
          } catch (Exception e) { return -1; }
          if (count == 0) return -1;
          r /= count;
          g /= count;
          b /= count;
          java.awt.Color.RGBtoHSB((int)r, (int)g, (int)b, hsb);
          hsb[1] = Math.min(1.0f, hsb[1] * 1.25f);
          hsb[2] = Math.min(1.0f, hsb[2] * 1.25f);
          return java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
      });
  }
  @SubscribeEvent
  static void clientSetup(final FMLClientSetupEvent event) {
    setTranslucent(TinkerFluids.honey);
    // slime
    setTranslucent(TinkerFluids.earthSlime);
    setTranslucent(TinkerFluids.skySlime);
    setTranslucent(TinkerFluids.enderSlime);
    // molten
    setTranslucent(TinkerFluids.moltenDiamond);
    setTranslucent(TinkerFluids.moltenEmerald);
    setTranslucent(TinkerFluids.moltenGlass);
    setTranslucent(TinkerFluids.moltenGlass);
    setTranslucent(TinkerFluids.liquidSoul);
    setTranslucent(TinkerFluids.moltenSoulsteel);
    setTranslucent(TinkerFluids.moltenAmethyst);
  }

  @SubscribeEvent
  static void itemColors(final RegisterColorHandlersEvent.Item event) {
    event.register((stack, index) -> index > 0 ? -1 : stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor(), TinkerFluids.potion.asItem());

    for (var item : BuiltInRegistries.ITEM) {
      if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(TConstruct.MOD_ID) && item instanceof BucketItem bucket) {
        if (bucket != TinkerFluids.potion.asItem()) {
          event.register((stack, index) -> {
            if (index == 1) {
              return getFluidColor(bucket.content);
            }
            return -1;
          }, bucket);
        }
      }
    }
  }

  private static void setTranslucent(FlowingFluidObject<?> fluid) {    ItemBlockRenderTypes.setRenderLayer(fluid.getStill(), RenderType.translucent());
    ItemBlockRenderTypes.setRenderLayer(fluid.getFlowing(), RenderType.translucent());
  }
}
