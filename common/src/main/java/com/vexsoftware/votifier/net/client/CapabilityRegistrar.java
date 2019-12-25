package com.vexsoftware.votifier.net.client;

import com.google.gson.JsonObject;
import io.netty.channel.ChannelHandlerContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public final class CapabilityRegistrar {
    public interface Context {
        interface MessageDecoder<T> {
            T decode(JsonObject o);
        }

        interface OnMessage<T> {
            void onMessage(ChannelHandlerContext ctx, T message);
        }
        void onActivate(Callable<ChannelHandlerContext> onActivate);
        <T> void onMessage(String type, MessageDecoder<T> decoder, OnMessage<T> handler);
        void onClose(Callable<ChannelHandlerContext> onClose);
    }

    private static final class CapabilityEntry {
        private static final class CapabilityMessageHandler<T> {
            final Context.MessageDecoder<T> decoder;
            final Context.OnMessage<T> onMessage;

            private CapabilityMessageHandler(Context.MessageDecoder<T> decoder, Context.OnMessage<T> onMessage) {
                this.decoder = decoder;
                this.onMessage = onMessage;
            }

            public void onMessage(ChannelHandlerContext ctx, JsonObject o) {
                T obj = decoder.decode(o);
                onMessage.onMessage(ctx, obj);
            }
        }
        private Callable<ChannelHandlerContext> onActivate;
        private Callable<ChannelHandlerContext> onClose;
        private final Map<String, CapabilityMessageHandler> inboundHandlers = new HashMap<>();
    }

    private final Map<String, CapabilityEntry> capabilities = new HashMap<>();

    void registerCapability(String capabilityName, Capability c) {
        assert !capabilities.containsKey(capabilityName);

        CapabilityEntry e = new CapabilityEntry();

        c.onAttach(new Context() {
            @Override
            public void onActivate(Callable<ChannelHandlerContext> onActivate) {
                assert onActivate != null;
                e.onActivate = onActivate;
            }

            @Override
            public <T> void onMessage(String type, MessageDecoder<T> decoder, OnMessage<T> handler) {
                assert type != null;
                assert decoder != null;
                assert handler != null;
                assert !e.inboundHandlers.containsKey(type);
                e.inboundHandlers.put(type, new CapabilityEntry.CapabilityMessageHandler<>(decoder, handler));
            }

            @Override
            public void onClose(Callable<ChannelHandlerContext> onClose) {
                assert onClose != null;
                e.onClose = onClose;
            }
        });
    }
}
