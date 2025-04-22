package industries._5505.quic_protocol_support.mixin.client;

import industries._5505.quic_protocol_support.client.QuicServerAddressProperties;
import net.minecraft.client.network.ServerAddress;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerAddress.class)
public abstract class ServerAddressMixin implements QuicServerAddressProperties {
	@Shadow
	@Final
	private static ServerAddress INVALID;

	@Unique
	private boolean isQuic = false;

	@Inject(method = "parse", at = @At("HEAD"), cancellable = true)
	private static void parse(String address, CallbackInfoReturnable<ServerAddress> callbackInfoReturnable) {
		var index = address.indexOf("://");
		if (index == -1) return;

		var scheme = address.substring(0, index);
		var newAddress = ServerAddress.parse(address.substring(index + 3));
		if (newAddress != INVALID) {
			((QuicServerAddressProperties) (Object) newAddress).setQuic(scheme.equals("quic"));
		}
		callbackInfoReturnable.setReturnValue(newAddress);
	}

	@Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
	private static void isValid(String address, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
		var index = address.indexOf("://");
		if (index == -1) return;

		var scheme = address.substring(0, index);
		if (!scheme.equals("quic") && !scheme.equals("tcp")) callbackInfoReturnable.setReturnValue(false);
	}

	@ModifyVariable(method = "isValid", at = @At(value = "LOAD"), argsOnly = true, index = 0)
	private static String isValidModifyAddress(String address) {
		var index = address.indexOf("://");
		if (index == -1) return address;

		return address.substring(index + 3);
	}

	public boolean isQuic() {
		return isQuic;
	}

	@Override
	public void setQuic(boolean isQuic) {
		this.isQuic = isQuic;
	}
}
