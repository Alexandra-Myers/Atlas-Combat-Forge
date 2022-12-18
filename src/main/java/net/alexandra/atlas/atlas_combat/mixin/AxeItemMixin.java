package net.alexandra.atlas.atlas_combat.mixin;

import net.alexandra.atlas.atlas_combat.extensions.IAxeItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tier;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AxeItem.class)
public class AxeItemMixin extends DiggerItemMixin implements IAxeItem {
	public AxeItemMixin(Tier tier, Properties properties) {
		super(tier, properties);
	}
	@Override
	public float getShieldCooldownMultiplier(int SHIELD_DISABLE) {
		return 1.6F+SHIELD_DISABLE * 0.5F;
	}
}
