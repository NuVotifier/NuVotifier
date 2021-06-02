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

    @ParameterizedTest
    @ValueSource(strings = { "servicename", "address", "timestamp" })
    void handlesAdditionalKeywordArguments(String key) throws Exception {
        Vote vote = ArgsToVote.parse(new String[] { "username=abc123", key + "=dummy" });

        // Find the getter on the Vote object
        for (Method method : Vote.class.getDeclaredMethods()) {
            if (method.getName().toLowerCase(Locale.ENGLISH).equals("get" + key) && method.getParameterCount()==0) {
                assertEquals("dummy", method.invoke(vote));
                return;
            }
        }

        fail("Unable to find appropriate getter - this is a bug");
    }

    @Test
    void handlesUsernameKeywordArgument() {
        Vote vote = ArgsToVote.parse(new String[] { "username=dummy_user" });
        assertEquals("dummy_user", vote.getUsername());
    }

    @ParameterizedTest
    @ValueSource(strings = { "servicename", "address", "timestamp" })
    void handlesMixedUsernameAndKeywordArgument(String key) throws Exception {
        Vote vote = ArgsToVote.parse(new String[] { "abc123", key + "=dummy" });

        assertEquals("abc123", vote.getUsername());

        // Find the getter on the Vote object
        for (Method method : Vote.class.getDeclaredMethods()) {
            if (method.getName().toLowerCase(Locale.ENGLISH).equals("get" + key) && method.getParameterCount() == 0) {
                assertEquals("dummy", method.invoke(vote));
                return;
            }
        }

        fail("Unable to find appropriate getter - this is a bug");
    }

    @ParameterizedTest
    @MethodSource("powerSetArgs")
    void handlesMixedKeywordArguments(Set<String> keys) throws Exception {
        String[] args = new String[keys.size() + 1];
        args[0] = "abc123";
        for (int i = 0; i < keys.size(); i++) {
            args[i + 1] = ImmutableList.copyOf(keys).get(i) + "=dummy";
        }
        Vote vote = ArgsToVote.parse(args);

        assertEquals("abc123", vote.getUsername());

        // Find the getters on the Vote object
        outer: for (String key : keys) {
            for (Method method : Vote.class.getDeclaredMethods()) {
                if (method.getName().toLowerCase(Locale.ENGLISH).equals("get" + key) && method.getParameterCount() == 0) {
                    assertEquals("dummy", method.invoke(vote));
                    continue outer;
                }
            }

            fail("Unable to find appropriate getter - this is a bug");
        }
    }

    private static Stream<Set<String>> powerSetArgs() {
        return Sets.powerSet(ImmutableSet.of("servicename", "address", "timestamp")).stream();
    }
}
