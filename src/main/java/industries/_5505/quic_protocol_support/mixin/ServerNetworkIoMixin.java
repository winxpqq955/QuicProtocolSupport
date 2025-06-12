package industries._5505.quic_protocol_support.mixin;

import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import net.minecraft.server.ServerNetworkIo;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerNetworkIo.class)
public class ServerNetworkIoMixin {
    @Shadow
    @Final
    private static Logger LOGGER = LogUtils.getLogger();

    @Shadow
    @Final
    private List<ChannelFuture> channels;

    @Inject(method = "stop", at = @At("TAIL"))
    private void stop(CallbackInfo callbackInfo) {
        for (ChannelFuture channelFuture : this.channels) {
            Channel channel = channelFuture.channel();
            if (channel instanceof QuicStreamChannel) {
                try {
                    channel.parent().close().sync();
                } catch (InterruptedException exception) {
                    LOGGER.error("Interrupted whilst closing channel");
                }
            }
        }
    }
}
