package industries._5505.quic_protocol_support.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import industries._5505.quic_protocol_support.client.QuicServerAddressProperties;
import net.minecraft.client.network.RedirectResolver;
import net.minecraft.client.network.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

@Mixin(RedirectResolver.class)
public interface RedirectResolverMixin {
	@Redirect(method = "method_36911", at = @At(value = "INVOKE", target = "Ljavax/naming/directory/DirContext;getAttributes(Ljava/lang/String;[Ljava/lang/String;)Ljavax/naming/directory/Attributes;"))
	private static Attributes method_36911(DirContext dirContext, String name, String[] attrIds, @Local(argsOnly = true) ServerAddress address) throws NamingException {
		if (((QuicServerAddressProperties) (Object) address).isQuic()) {
			return dirContext.getAttributes(name.replaceFirst("tcp", "udp"), attrIds);
		} else {
			return dirContext.getAttributes(name, attrIds);
		}
	}
}
