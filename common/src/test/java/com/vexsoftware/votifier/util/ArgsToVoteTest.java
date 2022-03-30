package com.vexsoftware.votifier.util;

import com.vexsoftware.votifier.model.Vote;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ArgsToVoteTest {
    @Test
    void handlesInvalidRequest() {
        assertThrows(IllegalArgumentException.class, () -> {
            ArgsToVote.parse(new String[0]);
        });
    }

    @Test
    void handlesSingleUsername() {
        Vote vote = ArgsToVote.parse(new String[] { "dummy_user" });
        assertEquals("dummy_user", vote.getUsername());
    }

    @Test
    void handlesSingleUsernameTooLong() {
        assertThrows(IllegalArgumentException.class, () -> {
            ArgsToVote.parse(new String[] { "AVeryVeryVeryLongUsername" });
        });
    }

    @Test
    void handleServiceNameGetter() {
        Vote vote = ArgsToVote.parse(new String[] { "username=abc123", "servicename=dummy" });
        assertEquals("abc123", vote.getUsername());
        assertEquals("dummy", vote.getServiceName());
    }
    @Test
    void handleAddressGetter() {
        Vote vote = ArgsToVote.parse(new String[] { "username=abc123", "address=dummy" });
        assertEquals("abc123", vote.getUsername());
        assertEquals("dummy", vote.getAddress());
    }
    @Test
    void handleTimestampGetter() {
        Vote vote = ArgsToVote.parse(new String[] { "username=abc123", "timestamp=dummy" });
        assertEquals("abc123", vote.getUsername());
        assertEquals("dummy", vote.getTimeStamp());
    }

    @Test
    void handlesUsernameKeywordArgument() {
        Vote vote = ArgsToVote.parse(new String[] { "username=dummy_user" });
        assertEquals("dummy_user", vote.getUsername());
    }
}
