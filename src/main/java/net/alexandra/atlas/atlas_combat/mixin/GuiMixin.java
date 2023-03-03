package net.alexandra.atlas.atlas_combat.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.alexandra.atlas.atlas_combat.config.ShieldIndicatorStatus;
import net.alexandra.atlas.atlas_combat.extensions.*;
import net.minecraft.Util;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(Gui.class)
public abstract class GuiMixin extends GuiComponent {
	@Shadow
	@Final
	protected Minecraft minecraft;

	@Shadow
	protected abstract boolean canRenderCrosshairForSpectator(HitResult hitResult);

	@Shadow
	protected int screenWidth;

	@Shadow
	protected int screenHeight;

	@Shadow
	protected abstract void renderSlot(int par1, int par2, float par3, Player par4, ItemStack par5, int par6);

	@Shadow
	protected abstract Player getCameraPlayer();

	@Shadow
	@Final
	protected static ResourceLocation WIDGETS_LOCATION;

	@Shadow
	protected long healthBlinkTime;

	@Shadow
	protected int tickCount;

	@Shadow
	@Final
	protected Random random;

	@Shadow
	protected int lastHealth;

	@Shadow
	protected long lastHealthTime;

	@Shadow
	protected int displayHealth;

	@Shadow
	protected abstract void renderHearts(PoseStack matrices, Player player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking);

	@Shadow
	protected abstract LivingEntity getPlayerVehicleWithHealth();

	@Shadow
	protected abstract int getVehicleMaxHearts(LivingEntity entity);

	@Shadow
	protected abstract int getVisibleVehicleHeartRows(int heartCount);

	/**
	 * @author
	 * @reason
	 */
	@Inject(method = "renderCrosshair", at = @At(value = "HEAD"), cancellable = true)
	private void renderCrosshair(PoseStack matrices, CallbackInfo ci) {
		Options options = this.minecraft.options;
		((IMinecraft)minecraft).redirectResult(minecraft.hitResult);
		if (options.getCameraType().isFirstPerson()) {
			if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR || this.canRenderCrosshairForSpectator(this.minecraft.hitResult)) {
				if (options.renderDebug && !options.hideGui && !this.minecraft.player.isReducedDebugInfo() && !options.reducedDebugInfo) {
					Camera camera = this.minecraft.gameRenderer.getMainCamera();
					PoseStack poseStack = RenderSystem.getModelViewStack();
					poseStack.pushPose();
					poseStack.translate((double)(screenWidth / 2), (double)(screenHeight / 2), (double)getBlitOffset());
					poseStack.mulPose(Vector3f.XN.rotationDegrees(camera.getXRot()));
					poseStack.mulPose(Vector3f.YP.rotationDegrees(camera.getYRot()));
					poseStack.scale(-1.0F, -1.0F, -1.0F);
					RenderSystem.applyModelViewMatrix();
					RenderSystem.renderCrosshair(10);
					poseStack.popPose();
					RenderSystem.applyModelViewMatrix();
				} else {
					RenderSystem.blendFuncSeparate(
							GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
							GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
							GlStateManager.SourceFactor.ONE,
							GlStateManager.DestFactor.ZERO
					);
					int i = 15;
					this.blit(matrices, (this.screenWidth - 15) / 2, (this.screenHeight - 15) / 2, 0, 0, 15, 15);

					int j = this.screenHeight / 2 - 7 + 16;
					int k = this.screenWidth / 2 - 8;
					ItemStack offHandStack = this.minecraft.player.getItemInHand(InteractionHand.OFF_HAND);
					ItemStack mainHandStack = this.minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
					boolean offHandShieldCooldown = offHandStack.getItem() instanceof IShieldItem && this.minecraft.player.getCooldowns().isOnCooldown(offHandStack.getItem());
					boolean mainHandShieldCooldown = mainHandStack.getItem() instanceof IShieldItem && this.minecraft.player.getCooldowns().isOnCooldown(mainHandStack.getItem());
					boolean isShieldCooldown = offHandShieldCooldown || mainHandShieldCooldown;
					boolean var7 = ((IOptions)this.minecraft.options).shieldIndicator() == ShieldIndicatorStatus.CROSSHAIR;
					if (var7 && isShieldCooldown) {
						this.blit(matrices, k, j, 52, 112, 16, 16);
					} else if (var7 && this.minecraft.player.isBlocking()) {
						this.blit(matrices, k, j, 36, 112, 16, 16);
					}else if (this.minecraft.options.attackIndicator == AttackIndicatorStatus.CROSSHAIR) {
						float maxIndicator =  ((IOptions)options).attackIndicatorValue().floatValue();
						float f = this.minecraft.player.getAttackStrengthScale(0.0F);
						boolean bl = false;
						EntityHitResult hitResult = minecraft.hitResult instanceof EntityHitResult ? (EntityHitResult) minecraft.hitResult : null;
						minecraft.crosshairPickEntity = hitResult != null ? hitResult.getEntity() : minecraft.crosshairPickEntity;
						if (this.minecraft.crosshairPickEntity != null && this.minecraft.crosshairPickEntity instanceof LivingEntity && f >= maxIndicator) {
							bl = (minecraft.hitResult).distanceTo(this.minecraft.crosshairPickEntity) <= ((PlayerExtensions)minecraft.player).getAttackRange(minecraft.player, 2.5);
							bl &= this.minecraft.crosshairPickEntity.isAlive();
						}
						if (bl) {
							this.blit(matrices, k, j, 68, 94, 16, 16);
						} else if (f > maxIndicator - 0.7 && f < maxIndicator) {
							int l = (int)((f - (maxIndicator - 0.7F)) / 0.70000005F * 17.0F);
							this.blit(matrices, k, j, 36, 94, 16, 4);
							this.blit(matrices, k, j, 52, 94, l, 4);
						}
					}

					RenderSystem.disableBlend();

				}

			}
		}
		ci.cancel();
	}
	@Inject(method = "renderHotbar", at = @At(value = "HEAD"), cancellable = true)
	private void renderHotbar(float tickDelta, PoseStack matrices, CallbackInfo ci) {
		Player player = getCameraPlayer();
		((IMinecraft)minecraft).redirectResult(minecraft.hitResult);
		if (player != null) {
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
			ItemStack itemStack = player.getOffhandItem();
			HumanoidArm humanoidArm = player.getMainArm().getOpposite();
			int i = this.screenWidth / 2;
			int j = this.getBlitOffset();
			int k = 182;
			int l = 91;
			this.setBlitOffset(-90);
			this.blit(matrices, i - 91, this.screenHeight - 22, 0, 0, 182, 22);
			this.blit(matrices, i - 91 - 1 + player.getInventory().selected * 20, this.screenHeight - 22 - 1, 0, 22, 24, 22);
			if (!itemStack.isEmpty()) {
				if (humanoidArm == HumanoidArm.LEFT) {
					this.blit(matrices, i - 91 - 29, this.screenHeight - 23, 24, 22, 29, 24);
				} else {
					this.blit(matrices, i + 91, this.screenHeight - 23, 53, 22, 29, 24);
				}
			}

			this.setBlitOffset(j);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			int m = 1;

			int n;
			int o;
			for(n = 0; n < 9; ++n) {
				o = i - 90 + n * 20 + 2;
				int p = this.screenHeight - 16 - 3;
				renderSlot(o, p, tickDelta, player, player.getInventory().items.get(n), m++);
			}

			if (!itemStack.isEmpty()) {
				n = this.screenHeight - 16 - 3;
				if (humanoidArm == HumanoidArm.LEFT) {
					this.renderSlot(i - 91 - 26, n, tickDelta, player, itemStack, m++);
				} else {
					this.renderSlot(i + 91 + 10, n, tickDelta, player, itemStack, m++);
				}
			}

			RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
			n = this.screenHeight - 20;
			o = i + 91 + 6;
			if (humanoidArm == HumanoidArm.RIGHT) {
				o = i - 91 - 22;
			}
			boolean var7 = ((IOptions)this.minecraft.options).shieldIndicator() == ShieldIndicatorStatus.HOTBAR;
			ItemStack mainHandStack = this.minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
			boolean offHandShieldCooldown = itemStack.getItem() instanceof IShieldItem && this.minecraft.player.getCooldowns().isOnCooldown(itemStack.getItem());
			boolean mainHandShieldCooldown = mainHandStack.getItem() instanceof IShieldItem && this.minecraft.player.getCooldowns().isOnCooldown(mainHandStack.getItem());
			boolean isShieldCooldown = offHandShieldCooldown || mainHandShieldCooldown;
			if (var7 && isShieldCooldown) {
				this.blit(matrices, o, n, 18, 112, 18, 18);
			} else if (var7 && this.minecraft.player.isBlocking()) {
				this.blit(matrices, o, n, 0, 112, 18, 18);
			} else if (this.minecraft.options.attackIndicator == AttackIndicatorStatus.HOTBAR) {
				float maxIndicator =  ((IOptions)minecraft.options).attackIndicatorValue().floatValue();
				float f = this.minecraft.player.getAttackStrengthScale(0.0F);
				boolean bl = false;
				EntityHitResult hitResult = minecraft.hitResult instanceof EntityHitResult ? (EntityHitResult) minecraft.hitResult : null;
				minecraft.crosshairPickEntity = hitResult != null ? hitResult.getEntity() : minecraft.crosshairPickEntity;
				if (this.minecraft.crosshairPickEntity != null && this.minecraft.crosshairPickEntity instanceof LivingEntity && f >= maxIndicator) {
					bl = (minecraft.hitResult).distanceTo(this.minecraft.crosshairPickEntity) <= ((PlayerExtensions)minecraft.player).getAttackRange(minecraft.player, 2.5);
					bl &= this.minecraft.crosshairPickEntity.isAlive();
				}
				if (bl) {
					this.blit(matrices, o, n, 0, 130, 18, 18);
				} else if (f > maxIndicator - 0.7F && f < maxIndicator) {

					int var16 = (int) ((f - (maxIndicator - 0.7F)) / 0.70000005F * 19.0F);
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
					this.blit(matrices, o, n, 0, 94, 18, 18);
					this.blit(matrices, o, n + 18 - var16, 18, 112 - var16, 18, var16);
				}
			}
			RenderSystem.disableBlend();
		}
		ci.cancel();
	}
	@Inject(method = "renderPlayerHealth", at = @At(value = "HEAD"), cancellable = true)
	private void renderPlayerHealth(PoseStack matrices, CallbackInfo ci) {
		Gui gui = (Gui)(Object)this;
		Player player = this.getCameraPlayer();
		if (player != null) {
			int i = Mth.ceil(player.getHealth());
			boolean bl = healthBlinkTime > (long)tickCount && (healthBlinkTime - (long)tickCount) / 3L % 2L == 1L;
			long l = Util.getMillis();
			if (i < lastHealth && player.invulnerableTime > 0) {
				lastHealthTime = l;
				healthBlinkTime = (long)(tickCount + 20);
			} else if (i > lastHealth && player.invulnerableTime > 0) {
				lastHealthTime = l;
				healthBlinkTime = (long)(tickCount + 10);
			}

			if (l - lastHealthTime > 1000L) {
				lastHealth = i;
				displayHealth = i;
				lastHealthTime = l;
			}

			lastHealth = i;
			int j = displayHealth;
			random.setSeed((long)(tickCount * 312871));
			FoodData foodData = player.getFoodData();
			int k = foodData.getFoodLevel();
			int m = this.screenWidth / 2 - 91;
			int n = this.screenWidth / 2 + 91;
			int o = this.screenHeight - 39;
			float f = Math.max((float)player.getAttributeValue(Attributes.MAX_HEALTH), (float)Math.max(j, i));
			int p = Mth.ceil(player.getAbsorptionAmount());
			int q = Mth.ceil((f + (float)p) / 2.0F / 10.0F);
			int r = Math.max(10 - (q - 2), 3);
			int s = o - (q - 1) * r - 10;
			int t = o - 10;
			int u = player.getArmorValue();
			int v = -1;
			if (player.hasEffect(MobEffects.REGENERATION)) {
				v = tickCount % Mth.ceil(f + 5.0F);
			}
			EnchantmentHelper helper = new EnchantmentHelper();
			int fire_prot = ((IEnchantmentHelper)helper).getFullEnchantmentLevel(Enchantments.FIRE_PROTECTION, player);
			int blast_prot = ((IEnchantmentHelper)helper).getFullEnchantmentLevel(Enchantments.BLAST_PROTECTION, player);
			int proj_prot = ((IEnchantmentHelper)helper).getFullEnchantmentLevel(Enchantments.PROJECTILE_PROTECTION, player);
			float prot = ((IEnchantmentHelper)helper).getFullEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, player);
			prot += fire_prot > 1 ? (fire_prot + Mth.ceil(fire_prot / 4.0F)) : 0;
			prot += blast_prot > 1 ? (blast_prot + Mth.ceil(blast_prot / 4.0F)) : 0;
			prot += proj_prot > 1 ? (proj_prot + Mth.ceil(proj_prot / 4.0F)) : 0;

			this.minecraft.getProfiler().push("armor");

			for(int w = 0; w < 10; ++w) {
				if (u > 0) {
					int x = m + w * 8;
					if (w * 2 + 1 < u) {
						this.blit(matrices, x, s, 34, 9, 9, 9);
					}

					if (w * 2 + 1 == u) {
						this.blit(matrices, x, s, 25, 9, 9, 9);
					}

					if (w * 2 + 1 > u) {
						this.blit(matrices, x, s, 16, 9, 9, 9);
					}

					if (w * 2 + 1 < prot && ((IOptions)minecraft.options).protIndicator()) {
						this.blit(matrices, x, s, 43, 18, 9, 9);
					}

					if (w * 2 + 1 == prot && ((IOptions)minecraft.options).protIndicator()) {
						this.blit(matrices, x, s, 34, 18, 9, 9);
					}
				}
			}

			this.minecraft.getProfiler().popPush("health");
			renderHearts(matrices, player, m, o, r, v, f, i, j, p, bl);
			LivingEntity livingEntity = getPlayerVehicleWithHealth();
			int x = getVehicleMaxHearts(livingEntity);
			if (x == 0) {
				this.minecraft.getProfiler().popPush("food");

				for(int y = 0; y < 10; ++y) {
					int z = o;
					int aa = 16;
					int ab = 0;
					if (player.hasEffect(MobEffects.HUNGER)) {
						aa += 36;
						ab = 13;
					}

					if (player.getFoodData().getSaturationLevel() <= 0.0F && tickCount % (k * 3 + 1) == 0) {
						z = o + (random.nextInt(3) - 1);
					}

					int ac = n - y * 8 - 9;
					this.blit(matrices, ac, z, 16 + ab * 9, 27, 9, 9);
					if (y * 2 + 1 < k) {
						this.blit(matrices, ac, z, aa + 36, 27, 9, 9);
					}

					if (y * 2 + 1 == k) {
						this.blit(matrices, ac, z, aa + 45, 27, 9, 9);
					}
				}

				t -= 10;
			}

			this.minecraft.getProfiler().popPush("air");
			int y = player.getMaxAirSupply();
			int z = Math.min(player.getAirSupply(), y);
			if (player.isEyeInFluid(FluidTags.WATER) || z < y) {
				int aa = getVisibleVehicleHeartRows(x) - 1;
				t -= aa * 10;
				int ab = Mth.ceil((double)(z - 2) * 10.0 / (double)y);
				int ac = Mth.ceil((double)z * 10.0 / (double)y) - ab;

				for(int ad = 0; ad < ab + ac; ++ad) {
					if (ad < ab) {
						this.blit(matrices, n - ad * 8 - 9, t, 16, 18, 9, 9);
					} else {
						this.blit(matrices, n - ad * 8 - 9, t, 25, 18, 9, 9);
					}
				}
			}

			this.minecraft.getProfiler().pop();
		}
		ci.cancel();
	}
}
