package com.vexsoftware.votifier.support.forwarding;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.vexsoftware.votifier.model.Vote;

import java.io.CharArrayReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class AbstractPluginMessagingForwardingSink implements ForwardingVoteSink {

    public AbstractPluginMessagingForwardingSink(ForwardedVoteListener listener) {
        this.listener = listener;
    }

    private final ForwardedVoteListener listener;

    public void handlePluginMessage(byte[] message) {
        String strMessage = new String(message, StandardCharsets.UTF_8);
        try (CharArrayReader reader = new CharArrayReader(strMessage.toCharArray())) {
            JsonReader r = new JsonReader(reader);
            r.setLenient(true);
            while (r.peek() != JsonToken.END_DOCUMENT) {
                r.beginObject();
                JsonObject o = new JsonObject();

                while (r.hasNext()) {
                    String name = r.nextName();
                    if (r.peek() == JsonToken.NUMBER)
                        o.add(name, new JsonPrimitive(r.nextLong()));
                    else
                        o.add(name, new JsonPrimitive(r.nextString()));
                }
                r.endObject();

                Vote v = new Vote(o);
                listener.onForward(v);
            }
        } catch (IOException e) {
            e.printStackTrace(); // Should never happen.
        }
    }
}
