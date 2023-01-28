package net.alexandra.atlas.atlas_combat.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

public class ShieldUtils {
    public static float getShieldBlockDamageValue(ItemStack blockingItem) {
        if(blockingItem.getItem() instanceof SwordItem swordItem) {
            Tier var2 = swordItem.getTier();
            return var2.getAttackDamageBonus() <= 1.0F ? -1F : 0.0F;
        }
        return blockingItem.getTagElement("BlockEntityTag") != null ? 10.0F : 5.0F;
    }

    public static float getShieldKnockbackResistanceValue(ItemStack blockingItem) {
        if(blockingItem.getItem() instanceof SwordItem swordItem) {
            return swordItem.getTier() == Tiers.NETHERITE || swordItem.getTier().getLevel() == 4 ? 0.1F : 0.0F;
        }
        return blockingItem.getTagElement("BlockEntityTag") != null ? 0.8F : 0.5F;
    }
}
