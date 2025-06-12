package industries._5505.quic_protocol_support.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Shadow
    private PlayerManager playerManager;

    @Inject(method = "shutdown", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerNetworkIo;stop()V", shift = At.Shift.BEFORE))
    private void shutdown(CallbackInfo callbackInfo) {
        if (this.playerManager != null) {
            this.playerManager.disconnectAllPlayers();
        }
    }
}
