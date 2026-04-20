package slimeknights.tconstruct.library.modifiers.modules.build;

import com.google.common.collect.ImmutableSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.block.BlockPredicate;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.EnchantmentModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BlockHarvestModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.HarvestEnchantmentsModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.LevelingIntModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModuleBuilder;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.ToolHarvestContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Modules that add enchantments to a tool.
 * In 1.21+, enchantments are data-driven, so we store the enchantment ID and resolve it from the registry at runtime.
 */
public interface EnchantmentModule extends ModifierModule, LevelingIntModule, ConditionalModule<IToolStackView> {
  LoadableField<ResourceLocation,EnchantmentModule> ENCHANTMENT = Loadables.RESOURCE_LOCATION.requiredField("name", EnchantmentModule::enchantmentId);
  LoadableField<IJsonPredicate<BlockState>,EnchantmentModule> BLOCK = BlockPredicate.LOADER.defaultField("block", EnchantmentModule::block);
  LoadableField<IJsonPredicate<LivingEntity>,EnchantmentModule> HOLDER = LivingEntityPredicate.LOADER.defaultField("holder", EnchantmentModule::holder);

  ResourceLocation enchantmentId();

  default ResourceKey<Enchantment> enchantmentKey() {
    return ResourceKey.create(Registries.ENCHANTMENT, enchantmentId());
  }

  default IJsonPredicate<BlockState> block() {
    return BlockPredicate.ANY;
  }

  default IJsonPredicate<LivingEntity> holder() {
    return LivingEntityPredicate.ANY;
  }

  static Builder builder(ResourceKey<Enchantment> enchantment) {
    return new Builder(enchantment.location());
  }

  static Builder builder(ResourceLocation enchantmentId) {
    return new Builder(enchantmentId);
  }

  @SuppressWarnings("unused")
  @Setter
  @Accessors(fluent = true)
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  class Builder extends ModuleBuilder.Stack<Builder> {
    private final ResourceLocation enchantmentId;
    private LevelingInt level = LevelingInt.LEVEL;
    private IJsonPredicate<BlockState> block = BlockPredicate.ANY;
    private IJsonPredicate<LivingEntity> holder = LivingEntityPredicate.ANY;

    /** @deprecated use {@link #level(LevelingInt)} */
    @Deprecated(forRemoval = true)
    public Builder level(int level) {
      this.level = LevelingInt.eachLevel(level);
      return this;
    }

    public Constant constant() {
      if (block != BlockPredicate.ANY || holder != LivingEntityPredicate.ANY) {
        throw new IllegalStateException("Cannot build a constant enchantment module with block or holder conditions");
      }
      return new Constant(enchantmentId, level, condition);
    }

    public Protection protection() {
      if (block != BlockPredicate.ANY || holder != LivingEntityPredicate.ANY) {
        throw new IllegalStateException("Cannot build a constant enchantment module with block or holder conditions");
      }
      return new Protection(enchantmentId, level, condition);
    }

    public MainHandHarvest mainHandHarvest(ResourceLocation key) {
      return new MainHandHarvest(enchantmentId, level, condition, key, block, holder);
    }

    public ArmorHarvest armorHarvest(EquipmentSlot... slots) {
      if (slots.length == 0) {
        throw new IllegalArgumentException("Must have at least 1 slot");
      }
      Set<EquipmentSlot> set = ImmutableSet.copyOf(slots);
      if (set.contains(EquipmentSlot.MAINHAND)) {
        throw new IllegalArgumentException("Cannot create armor harvest for the main hand slot");
      }
      return new ArmorHarvest(enchantmentId, level, condition, set, block, holder);
    }

    public ArmorHarvest armorHarvest() {
      return armorHarvest(HarvestEnchantmentsModifierHook.APPLICABLE_SLOTS);
    }
  }

  @Accessors(fluent = true)
  @Getter
  @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
  class Constant implements EnchantmentModule, EnchantmentModifierHook {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<Constant>defaultHooks(ModifierHooks.ENCHANTMENTS);
    public static final RecordLoadable<Constant> LOADER = RecordLoadable.create(ENCHANTMENT, LevelingIntModule.FIELD, ModifierCondition.TOOL_FIELD, Constant::new);
    private final ResourceLocation enchantmentId;
    private final LevelingInt level;
    private final ModifierCondition<IToolStackView> condition;

    public Constant(ResourceLocation enchantmentId, int level) {
      this(enchantmentId, LevelingInt.eachLevel(level), ModifierCondition.ANY_TOOL);
    }

    public Constant(ResourceLocation enchantmentId, int level, ModifierCondition<IToolStackView> condition) {
      this(enchantmentId, LevelingInt.eachLevel(level), condition);
    }

    @Override
    public int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Holder<Enchantment> enchantment, int level) {
      if (enchantment.is(enchantmentKey()) && condition().matches(tool, modifier)) {
        level += getLevel(modifier);
      }
      return level;
    }

    @Override
    public void updateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Enchantment,Integer> map) {
      if (condition().matches(tool, modifier)) {
        // Enchantments are registry driven in 1.21+, so callers should prefer updateEnchantmentLevel().
      }
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
      return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<Constant> getLoader() {
      return LOADER;
    }
  }

  class Protection extends Constant {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<Protection>defaultHooks(ModifierHooks.ENCHANTMENTS);
    public static final RecordLoadable<Constant> LOADER = RecordLoadable.create(ENCHANTMENT, LevelingIntModule.FIELD, ModifierCondition.TOOL_FIELD, Protection::new);

    public Protection(ResourceLocation enchantmentId, LevelingInt level, ModifierCondition<IToolStackView> condition) {
      super(enchantmentId, level, condition);
    }

    public Protection(ResourceLocation enchantmentId, int level, ModifierCondition<IToolStackView> condition) {
      super(enchantmentId, level, condition);
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
      return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<Constant> getLoader() {
      return LOADER;
    }
  }

  record MainHandHarvest(ResourceLocation enchantmentId, LevelingInt level, ModifierCondition<IToolStackView> condition, ResourceLocation conditionFlag, IJsonPredicate<BlockState> block, IJsonPredicate<LivingEntity> holder) implements EnchantmentModule, EnchantmentModifierHook, BlockHarvestModifierHook {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<MainHandHarvest>defaultHooks(ModifierHooks.ENCHANTMENTS, ModifierHooks.BLOCK_HARVEST);
    public static final RecordLoadable<MainHandHarvest> LOADER = RecordLoadable.create(ENCHANTMENT, LevelingIntModule.FIELD, ModifierCondition.TOOL_FIELD, Loadables.RESOURCE_LOCATION.requiredField("condition_flag", MainHandHarvest::conditionFlag), BLOCK, HOLDER, MainHandHarvest::new);

    @Internal
    public MainHandHarvest {}

    /** @deprecated use {@link Builder#mainHandHarvest(ResourceLocation)} */
    @Deprecated(forRemoval = true)
    public MainHandHarvest(ResourceLocation enchantmentId, int level, ModifierCondition<IToolStackView> condition, ResourceLocation conditionFlag, IJsonPredicate<BlockState> block, IJsonPredicate<LivingEntity> holder) {
      this(enchantmentId, LevelingInt.eachLevel(level), condition, conditionFlag, block, holder);
    }

    @Override
    public void startHarvest(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context) {
      if (condition.matches(tool, modifier) && block.matches(context.getState()) && holder.matches(context.getLiving())) {
        tool.getPersistentData().putBoolean(conditionFlag, true);
      }
      BlockHarvestModifierHook.super.startHarvest(tool, modifier, context);
    }

    @Override
    public void finishHarvest(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context, int harvested) {
      tool.getPersistentData().remove(conditionFlag);
    }

    @Override
    public int updateEnchantmentLevel(IToolStackView tool, ModifierEntry modifier, Holder<Enchantment> enchantment, int level) {
      if (enchantment.is(enchantmentKey()) && tool.getPersistentData().getBoolean(conditionFlag)) {
        level += getLevel(modifier);
      }
      return level;
    }

    @Override
    public void updateEnchantments(IToolStackView tool, ModifierEntry modifier, Map<Enchantment,Integer> map) {}

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
      return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<MainHandHarvest> getLoader() {
      return LOADER;
    }
  }

  record ArmorHarvest(ResourceLocation enchantmentId, LevelingInt level, ModifierCondition<IToolStackView> condition, Set<EquipmentSlot> slots, IJsonPredicate<BlockState> block, IJsonPredicate<LivingEntity> holder) implements EnchantmentModule, HarvestEnchantmentsModifierHook {
    private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<ArmorHarvest>defaultHooks(ModifierHooks.HARVEST_ENCHANTMENTS);
    public static final RecordLoadable<ArmorHarvest> LOADER = RecordLoadable.create(ENCHANTMENT, LevelingIntModule.FIELD, ModifierCondition.TOOL_FIELD, TinkerLoadables.EQUIPMENT_SLOT_SET.requiredField("slots", ArmorHarvest::slots), BLOCK, HOLDER, ArmorHarvest::new);

    @Internal
    public ArmorHarvest {}

    /** @deprecated use {@link Builder#armorHarvest(EquipmentSlot...)} */
    @Deprecated(forRemoval = true)
    public ArmorHarvest(ResourceLocation enchantmentId, int level, ModifierCondition<IToolStackView> condition, Set<EquipmentSlot> slots, IJsonPredicate<BlockState> block, IJsonPredicate<LivingEntity> holder) {
      this(enchantmentId, LevelingInt.eachLevel(level), condition, slots, block, holder);
    }

    @Override
    public void updateHarvestEnchantments(IToolStackView tool, ModifierEntry modifier, ToolHarvestContext context, EquipmentContext equipment, EquipmentSlot slot, Map<Enchantment,Integer> map) {
      if (slots.contains(slot) && condition.matches(tool, modifier) && block.matches(context.getState()) && holder.matches(context.getLiving())) {
        // Enchantments are registry driven in 1.21+, so callers should prefer updateEnchantmentLevel().
      }
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
      return DEFAULT_HOOKS;
    }

    @Override
    public RecordLoadable<ArmorHarvest> getLoader() {
      return LOADER;
    }
  }
}