package com.vexsoftware.votifier.support.forwarding;

import com.google.common.collect.Lists;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PMForwardingSinkTest {

    @Test
    public void testSuccessfulMultiDecode() throws Exception {
        List<Vote> receivedVotes = Lists.newArrayList();
        ForwardedVoteListener vl = receivedVotes::add;

        List<Vote> sentVotes = Lists.newArrayList(
                new Vote("serviceA", "usernameA", "1.1.1.1", "1546300800"),
                new Vote("serviceB", "usernameBBBBBBB", "1.2.23.4", "1514764800", System.currentTimeMillis())
        );

        StringBuilder message = new StringBuilder();

        for (Vote v : sentVotes) {
            message.append(v.serialize());
        }

        byte[] messageBytes = message.toString().getBytes();
        System.out.println(message.toString());

        AbstractPluginMessagingForwardingSink sink = new AbstractPluginMessagingForwardingSink(vl) {
            @Override
            public void halt() {

            }
        };

        sink.handlePluginMessage(messageBytes);

        assertEquals(sentVotes.size(), receivedVotes.size());

        for (int i = 0; i < receivedVotes.size(); i++) {
            assertEquals(sentVotes.get(i), receivedVotes.get(i));
        }
    }
}
