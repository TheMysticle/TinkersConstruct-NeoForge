package slimeknights.tconstruct.library.tools.item.armor;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;

/** Armor model that applies multiple texture layers in order */
public class MultilayerArmorItem extends ModifiableArmorItem {
  private final ResourceLocation name;
  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorItem.Type slot, Properties properties) {
    this(material, slot, properties, material.getId());
  }

  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorItem.Type slot, Properties properties, ResourceLocation name) {
    super(material, slot, properties);
    this.name = name;
  }

  @SuppressWarnings("removal")
  public MultilayerArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type slot, Properties properties, ToolDefinition toolDefinition) {
    super(material, slot, properties, toolDefinition);
    this.name = material.unwrapKey().map(key -> key.location()).orElse(ResourceLocation.withDefaultNamespace("unknown"));
  }

  public MultilayerArmorItem(DummyArmorMaterial material, ArmorItem.Type slot, Properties properties, ToolDefinition toolDefinition) {
    super(material.getHolder(), slot, properties, toolDefinition);
    this.name = material.getId();
  }

  /** Gets the name of this armor for model resolution */
  public ResourceLocation getArmorName() {
    return name;
  }

  // Note: getArmorTexture and initializeClient were removed in NeoForge 1.21.
  // Armor rendering is handled through ArmorModelManager and IClientItemExtensions registered via client events.
}
