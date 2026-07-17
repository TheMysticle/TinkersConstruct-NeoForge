package slimeknights.tconstruct.fluids.data;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.core.registries.Registries;
import slimeknights.mantle.data.GenericDataProvider;

import java.util.concurrent.CompletableFuture;

/** Quick and dirty data provider to generate fluid bucket models */
public class FluidBucketModelProvider extends GenericDataProvider {
  private final String modId;
  public FluidBucketModelProvider(PackOutput packOutput, String modId) {
    super(packOutput, Target.RESOURCE_PACK, "models/item");
    this.modId = modId;
  }

  /** Makes the JSON for a given bucket */
  @SuppressWarnings("deprecation")  // best way to get keys
  private static JsonObject makeJson(BucketItem bucket) {
    JsonObject json = new JsonObject();
    json.addProperty("parent", "minecraft:item/generated");
    JsonObject textures = new JsonObject();
    textures.addProperty("layer0", "minecraft:item/bucket");
    textures.addProperty("layer1", "tconstruct:item/potion_bucket_contents");
    json.add("textures", textures);
    return json;
  }

  @SuppressWarnings("deprecation")  // easiest item set lookup
  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    return allOf(
      BuiltInRegistries.ITEM.entrySet().stream()
        .filter(entry -> entry.getKey().location().getNamespace().equals(modId) && entry.getValue() instanceof BucketItem)
        .map(entry -> saveJson(cache, entry.getKey().location(), makeJson((BucketItem)entry.getValue()))));
  }

  @Override
  public String getName() {
    return modId + " Fluid Bucket Model Provider";
  }
}
