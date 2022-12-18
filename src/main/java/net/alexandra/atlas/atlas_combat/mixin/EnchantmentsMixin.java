package net.alexandra.atlas.atlas_combat.mixin;

import net.alexandra.atlas.atlas_combat.enchantment.CleavingEnchantment;
import net.alexandra.atlas.atlas_combat.extensions.IEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Enchantments.class)
public abstract class EnchantmentsMixin implements IEnchantments {
	@Shadow
	private static Enchantment register(String name, Enchantment enchantment) {
		return null;
	}

	private static final Enchantment CLEAVING_ENCHANTMENT = register("cleaving", new CleavingEnchantment());

	@Override
	public Enchantment getCleavingEnchantment() {
		return CLEAVING_ENCHANTMENT;
	}
}
