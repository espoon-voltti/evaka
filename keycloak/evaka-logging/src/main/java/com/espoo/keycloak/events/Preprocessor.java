// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package com.espoo.keycloak.events;

import org.keycloak.events.Event;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Preprocessor for log events
 */
public record Preprocessor(
    PreprocessMode username,
    PreprocessMode sessionId,
    PreprocessMode ipAddress,
    PreprocessMode identity
) {
    public void preprocess(Event event) {
        preprocessDetails(this.username, event, "username");
        preprocessDetails(this.identity, event, "identity_provider_identity");
        preprocessProperty(this.sessionId, event::getSessionId, event::setSessionId);
        preprocessProperty(this.ipAddress, event::getIpAddress, event::setIpAddress);
    }
    private static void preprocessProperty(PreprocessMode mode, Supplier<String> getter, Consumer<String> setter) {
        switch (mode) {
            case KEEP -> {}
            case HASH -> setter.accept(sha256(getter.get()));
            case DROP -> setter.accept(null);
        }
    }
    private static void preprocessDetails(PreprocessMode mode, Event event, String key) {
        switch (mode) {
            case KEEP -> {}
            case HASH -> {
                var input = event.getDetails();
                var value = input != null ? input.get(key) : null;
                if (value != null) {
                    var output = new HashMap<>(input);
                    output.put(key, sha256(value));
                    event.setDetails(output);
                }
            }
            case DROP -> {
                var input = event.getDetails();
                if (input != null && input.containsKey(key)) {
                    var output = new HashMap<>(input);
                    output.remove(key);
                    event.setDetails(output);
                }
            }
        }
    }

    private static final HexFormat HEX_FORMAT = HexFormat.of().withLowerCase();

    private static String sha256(String source) {
        if (source == null) return null;
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HEX_FORMAT.formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public static Preprocessor fromEnvironment(Map<String, String> env) {
        return new Preprocessor(
            fromEnv(env, "LOG_DROP_USERNAME", "LOG_HASH_USERNAME"),
            fromEnv(env, "LOG_DROP_SESSION_ID", "LOG_HASH_SESSION_ID"),
            fromEnv(env, "LOG_DROP_IP_ADDRESS", "LOG_HASH_IP_ADDRESS"),
            fromEnv(env, "LOG_DROP_IDENTITY", "LOG_HASH_IDENTITY")
        );
    }

    private static PreprocessMode fromEnv(Map<String, String> env, String dropVariable, String hashVariable) {
        if (getBoolean(env, dropVariable)) return PreprocessMode.DROP;
        else if (getBoolean(env, hashVariable)) return PreprocessMode.HASH;
        else return PreprocessMode.KEEP;
    }

    private static boolean getBoolean(Map<String, String> env, String key) {
        var value = env.get(key);
        return value != null && value.trim().equals("true");
    }
}
