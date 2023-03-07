package net.alexandra.atlas.atlas_combat.mixin;

import net.minecraft.network.protocol.status.ServerStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerStatus.Version.class)
public class VersionMixin {
	@Inject(method = "getName", at = @At(value = "RETURN"), cancellable = true)
	public void changeName(CallbackInfoReturnable<String> cir) {
		cir.setReturnValue("Combat Test 8c");
		cir.cancel();
	}
	@Inject(method = "getProtocol", at = @At(value = "RETURN"), cancellable = true)
	public void changeProtocol(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(800);
		cir.cancel();
	}
}