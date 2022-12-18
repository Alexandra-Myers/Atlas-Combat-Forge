package net.alexandra.atlas.atlas_combat.mixin;

import net.minecraft.world.effect.AttackDamageMobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AttackDamageMobEffect.class)
public class AttackDamageMobEffectMixin {

    @Shadow
    @Final
    protected double multiplier;

    /**
     * @author zOnlyKroks
     * @reason because
     */
    @Overwrite
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return this.multiplier * (double)(amplifier + 1);
    }

}
