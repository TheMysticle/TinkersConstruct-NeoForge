package slimeknights.tconstruct.library.recipe.partbuilder.recycle;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.recipe.TConstructLoadableRecipeSerializer;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

import java.util.LinkedHashMap;
import java.util.Map;

public class PartBuilderRecycleSerializer extends TConstructLoadableRecipeSerializer<PartBuilderRecycle> {
  public PartBuilderRecycleSerializer() {
    super(PartBuilderRecycle.LOADER);
  }

  @Override
  public StreamCodec<RegistryFriendlyByteBuf, PartBuilderRecycle> streamCodec() {
    return StreamCodec.of(this::encode, this::decode);
  }

  private void encode(RegistryFriendlyByteBuf buffer, PartBuilderRecycle recipe) {
    ResourceLocation id = getRecipeId(recipe);
    try {
      buffer.writeResourceLocation(id);
      Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getToolIngredient());
      Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.getPatternIngredient());
      Map<Pattern,ItemOutput> results = recipe.getResults();
      buffer.writeVarInt(results.size());
      for (Map.Entry<Pattern,ItemOutput> entry : results.entrySet()) {
        Pattern.PARSER.encode(buffer, entry.getKey());
        JsonElement json = ItemOutput.Loadable.OPTIONAL_STACK.serialize(entry.getValue());
        buffer.writeUtf(json.toString(), Short.MAX_VALUE);
      }
    } catch (RuntimeException e) {
      Mantle.logger.error("{}: Error writing recipe {} to packet", getClass().getSimpleName(), id, e);
      throw e;
    }
  }

  private PartBuilderRecycle decode(RegistryFriendlyByteBuf buffer) {
    ResourceLocation id = buffer.readResourceLocation();
    try {
      Ingredient tool = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
      Ingredient pattern = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
      int size = buffer.readVarInt();
      TypedMap context = buildContext(id).build();
      Map<Pattern,ItemOutput> results = new LinkedHashMap<>(size);
      for (int index = 0; index < size; index++) {
        Pattern key = Pattern.PARSER.decode(buffer, context);
        JsonElement value = JsonParser.parseString(buffer.readUtf(Short.MAX_VALUE));
        results.put(key, ItemOutput.Loadable.OPTIONAL_STACK.convert(value, "result", context));
      }
      return new PartBuilderRecycle(id, tool, pattern, results);
    } catch (RuntimeException e) {
      Mantle.logger.error("{}: Error reading recipe {} from packet", getClass().getSimpleName(), id, e);
      throw e;
    }
  }
}