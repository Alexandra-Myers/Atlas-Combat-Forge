package net.alexandra.atlas.atlas_combat.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.alexandra.atlas.atlas_combat.AtlasCombat;
import net.alexandra.atlas.atlas_combat.extensions.IItemStack;
import net.alexandra.atlas.atlas_combat.item.NewAttributes;
import net.alexandra.atlas.atlas_combat.item.WeaponType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.text.DecimalFormat;
import java.util.*;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements IItemStack {
	@Shadow
	public abstract Component getHoverName();
	@Shadow
	public abstract Rarity getRarity();
	@Shadow
	public abstract boolean hasCustomHoverName();
	@Shadow
	public abstract boolean is(Item item);
	@Shadow
	protected abstract int getHideFlags();
	@Shadow
	public abstract Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot);
	@Shadow
	private static boolean shouldShowInTooltip(int flags, ItemStack.TooltipPart tooltipSection){
		return false;
	}
	@Shadow
	public abstract Item getItem();
	@Shadow
	public abstract boolean hasTag();
	@Shadow
	@Nullable
	private CompoundTag tag;
	@Shadow
	public abstract ListTag getEnchantmentTags();
	@Shadow
	public static void appendEnchantmentNames(List<Component> tooltip, ListTag enchantments) {
	}
	@Shadow
	@Final
	private static Style LORE_STYLE;
	@Shadow
	@Final
	public static DecimalFormat ATTRIBUTE_MODIFIER_FORMAT;
	@Shadow
	private static Collection<Component> expandBlockState(String tag) {
		return null;
	}
	@Shadow
	public abstract int getMaxDamage();
	@Shadow
	public abstract boolean isDamaged();
	@Shadow
	public abstract int getDamageValue();
	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public List<Component> getTooltipLines(@Nullable Player player, TooltipFlag context) {
		List<Component> list = Lists.<Component>newArrayList();
		MutableComponent mutableComponent = Component.empty().append(this.getHoverName()).withStyle(this.getRarity().color);
		if (this.hasCustomHoverName()) {
			mutableComponent.withStyle(ChatFormatting.ITALIC);
		}

		list.add(mutableComponent);
		if (!context.isAdvanced() && !this.hasCustomHoverName() && this.is(Items.FILLED_MAP)) {
			Integer integer = MapItem.getMapId(((ItemStack)(Object)this));
			if (integer != null) {
				list.add(Component.literal("#" + integer).withStyle(ChatFormatting.GRAY));
			}
		}

		int i = this.getHideFlags();
		if (shouldShowInTooltip(i, ItemStack.TooltipPart.ADDITIONAL)) {
			this.getItem().appendHoverText((ItemStack)(Object)this, player == null ? null : player.level, list, context);
		}

		if (this.hasTag()) {
			if (shouldShowInTooltip(i, ItemStack.TooltipPart.ENCHANTMENTS)) {
				appendEnchantmentNames(list, this.getEnchantmentTags());
			}

			if (this.tag.contains("display", 10)) {
				CompoundTag compoundTag = this.tag.getCompound("display");
				if (shouldShowInTooltip(i, ItemStack.TooltipPart.DYE) && compoundTag.contains("color", 99)) {
					if (context.isAdvanced()) {
						list.add(Component.translatable("item.color", String.format("#%06X", compoundTag.getInt("color"))).withStyle(ChatFormatting.GRAY));
					} else {
						list.add(Component.translatable("item.dyed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
					}
				}

				if (compoundTag.getTagType("Lore") == 9) {
					ListTag listTag = compoundTag.getList("Lore", 8);

					for(int j = 0; j < listTag.size(); ++j) {
						String string = listTag.getString(j);

						try {
							MutableComponent mutableComponent2 = Component.Serializer.fromJson(string);
							if (mutableComponent2 != null) {
								list.add(ComponentUtils.mergeStyles(mutableComponent2, LORE_STYLE));
							}
						} catch (Exception var19) {
							compoundTag.remove("Lore");
						}
					}
				}
			}
		}

		if (shouldShowInTooltip(i, ItemStack.TooltipPart.MODIFIERS)) {
			for(EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
				Multimap<Attribute, AttributeModifier> multimap = this.getAttributeModifiers(equipmentSlot);
				if (multimap != null) {
					if (!multimap.isEmpty()) {
						list.add(CommonComponents.EMPTY);
						list.add(Component.translatable("item.modifiers." + equipmentSlot.getName()).withStyle(ChatFormatting.GRAY));

						for (Map.Entry<Attribute, AttributeModifier> entry : multimap.entries()) {
							AttributeModifier attributeModifier = (AttributeModifier) entry.getValue();
							double d = attributeModifier.getAmount();
							boolean bl = false;
							if (player != null) {
								if (attributeModifier.getId() == WeaponType.BASE_ATTACK_DAMAGE_UUID) {
									d += player.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
									d += EnchantmentHelper.getDamageBonus((ItemStack) (Object) this, player.getMobType());
									bl = true;
								} else if (attributeModifier.getId() == WeaponType.BASE_ATTACK_SPEED_UUID) {
									d += player.getAttribute(Attributes.ATTACK_SPEED).getBaseValue() - 1.5;
									bl = true;
								} else if (attributeModifier.getId() == WeaponType.BASE_ATTACK_REACH_UUID) {
									d += player.getAttribute(NewAttributes.ATTACK_REACH).getBaseValue() + (AtlasCombat.CONFIG.attackReach.get() ? 2.5 : 3);
									bl = true;
								} else if (attributeModifier.getId() == WeaponType.BASE_BLOCK_REACH_UUID) {
									d += player.getAttribute(NewAttributes.BLOCK_REACH).getBaseValue() + 6.0;
									bl = true;
								} else if (((Attribute) entry.getKey()).equals(Attributes.KNOCKBACK_RESISTANCE)) {
									d += player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue();
								}
							}

							double e;
							if (attributeModifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE
									|| attributeModifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) {
								e = d * 100.0;
							} else if (((Attribute) entry.getKey()).equals(Attributes.KNOCKBACK_RESISTANCE)) {
								e = d * 10.0;
							} else {
								e = d;
							}

							if (attributeModifier.getId() == WeaponType.BASE_BLOCK_REACH_UUID || attributeModifier.getId() == WeaponType.BASE_ATTACK_REACH_UUID || attributeModifier.getId() == WeaponType.BASE_ATTACK_SPEED_UUID || attributeModifier.getId() == WeaponType.BASE_ATTACK_DAMAGE_UUID || bl) {
								list.add(
										Component.literal(" ")
												.append(
														Component.translatable(
																"attribute.modifier.equals." + attributeModifier.getOperation().toValue(),
																ATTRIBUTE_MODIFIER_FORMAT.format(e),
																Component.translatable(((Attribute) entry.getKey()).getDescriptionId())
														)
												)
												.withStyle(ChatFormatting.DARK_GREEN)
								);
							} else if (d > 0.0) {
								list.add(
										Component.translatable(
														"attribute.modifier.plus." + attributeModifier.getOperation().toValue(),
														ATTRIBUTE_MODIFIER_FORMAT.format(e),
														Component.translatable(((Attribute) entry.getKey()).getDescriptionId())
												)
												.withStyle(ChatFormatting.BLUE)
								);
							} else if (d < 0.0) {
								e *= -1.0;
								list.add(
										Component.translatable(
														"attribute.modifier.take." + attributeModifier.getOperation().toValue(),
														ATTRIBUTE_MODIFIER_FORMAT.format(e),
														Component.translatable(((Attribute) entry.getKey()).getDescriptionId())
												)
												.withStyle(ChatFormatting.RED)
								);
							}
						}
					}
				}
			}
		}

		if (this.hasTag()) {
			if (shouldShowInTooltip(i, ItemStack.TooltipPart.UNBREAKABLE) && this.tag.getBoolean("Unbreakable")) {
				list.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE));
			}

			if (shouldShowInTooltip(i, ItemStack.TooltipPart.CAN_DESTROY) && this.tag.contains("CanDestroy", 9)) {
				ListTag listTag2 = this.tag.getList("CanDestroy", 8);
				if (!listTag2.isEmpty()) {
					list.add(CommonComponents.EMPTY);
					list.add(Component.translatable("item.canBreak").withStyle(ChatFormatting.GRAY));

					for(int k = 0; k < listTag2.size(); ++k) {
						list.addAll(expandBlockState(listTag2.getString(k)));
					}
				}
			}

			if (shouldShowInTooltip(i, ItemStack.TooltipPart.CAN_PLACE) && this.tag.contains("CanPlaceOn", 9)) {
				ListTag listTag2 = this.tag.getList("CanPlaceOn", 8);
				if (!listTag2.isEmpty()) {
					list.add(CommonComponents.EMPTY);
					list.add(Component.translatable("item.canPlace").withStyle(ChatFormatting.GRAY));

					for(int k = 0; k < listTag2.size(); ++k) {
						list.addAll(expandBlockState(listTag2.getString(k)));
					}
				}
			}
		}

		if (context.isAdvanced()) {
			if (this.isDamaged()) {
				list.add(Component.translatable("item.durability", this.getMaxDamage() - this.getDamageValue(), this.getMaxDamage()));
			}

			list.add(Component.literal(BuiltInRegistries.ITEM.getKey(this.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));
			if (this.hasTag()) {
				list.add(Component.translatable("item.nbt_tags", this.tag.getAllKeys().size()).withStyle(ChatFormatting.DARK_GRAY));
			}
		}

		return list;
	}
	@Override
	public int getEnchantmentLevel(Enchantment enchantment) {
		Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments((ItemStack) (Object) this);
		return map.get(enchantment);
	}
}

