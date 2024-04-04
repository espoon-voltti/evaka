// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package com.espoo.keycloak.events;

import org.junit.jupiter.api.Test;
import org.keycloak.events.Event;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PreprocessorTest {
    private static final String TEST_IP_ADDRESS = "0.0.0.0";
    private static final String TEST_SESSION_ID = "session-id";
    private static final String TEST_USERNAME = "user";
    private static final String TEST_IDENTITY = "identity";

    @Test
    public void testPreprocessor() {
        var env = new HashMap<String, String>();
        // no config for username -> keep
        env.put("LOG_HASH_SESSION_ID", "true");
        env.put("LOG_DROP_IP_ADDRESS", "true");
        env.put("LOG_HASH_IDENTITY", "true");
        var preprocessor = Preprocessor.fromEnvironment(env);

        var event = new Event();
        var details = new HashMap<String, String>();
        details.put("username", TEST_USERNAME);
        event.setSessionId(TEST_SESSION_ID);
        event.setIpAddress(TEST_IP_ADDRESS);
        details.put("identity_provider_identity", TEST_IDENTITY);
        event.setDetails(details);

        preprocessor.preprocess(event);
        assertEquals(TEST_USERNAME, event.getDetails().get("username"));
        assertEquals("4bdf1e15df716f27ff6ebcc119aa4b8863a221cd54e87772d824888f4aeac5c0", event.getSessionId());
        assertNull(event.getIpAddress());
        assertEquals("689f6a627384c7dcb2dcc1487e540223e77bdf9dcd0d8be8a326eda65b0ce9a4", event.getDetails().get("identity_provider_identity"));
    }
}
