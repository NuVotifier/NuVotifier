package com.vexsoftware.votifier.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.vexsoftware.votifier.model.Vote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals("dummy", vote.getServiceName());
    }
    @Test
    void handleAddressGetter() {
        Vote vote = ArgsToVote.parse(new String[] { "username=abc123", "address=dummy" });
        assertEquals("dummy", vote.getAddress());
    }
    @Test
    void handleTimestampGetter() {
        Vote vote = ArgsToVote.parse(new String[] { "username=abc123", "timestamp=dummy" });
        assertEquals("dummy", vote.getTimeStamp());
    }

    @Test
    void handlesUsernameKeywordArgument() {
        Vote vote = ArgsToVote.parse(new String[] { "username=dummy_user" });
        assertEquals("dummy_user", vote.getUsername());
    }

    private static Stream<Set<String>> powerSetArgs() {
        return Sets.powerSet(ImmutableSet.of("servicename", "address", "timestamp")).stream();
    }
}
