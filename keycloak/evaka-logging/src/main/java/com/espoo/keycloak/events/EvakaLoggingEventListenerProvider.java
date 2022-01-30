// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package com.espoo.keycloak.events;

import org.keycloak.events.log.JBossLoggingEventListenerProvider;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.events.Event;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import java.util.Map;

public class EvakaLoggingEventListenerProvider extends JBossLoggingEventListenerProvider {
    private static MessageDigest digest;
    private boolean hashUsername = false;
    private boolean dropUsername = false;
    private boolean hashSessionId = false;
    private boolean dropSessionId = false;
    private boolean hashIpAdderss = false;
    private boolean dropIpAdderss = false;

    public EvakaLoggingEventListenerProvider(KeycloakSession session, Logger logger, Logger.Level successLevel,
            Logger.Level errorLevel, boolean hashUsername, boolean dropUsername, boolean hashSessionId,
            boolean dropSessionId, boolean hashIpAdderss, boolean dropIpAdderss) {
        super(session, logger, successLevel, errorLevel);
        this.hashUsername = hashUsername;
        this.dropUsername = dropUsername;
        this.hashSessionId = hashSessionId;
        this.dropSessionId = dropSessionId;
        this.hashIpAdderss = hashIpAdderss;
        this.dropIpAdderss = dropIpAdderss;

        try {
            if (digest == null) {
                digest = MessageDigest.getInstance("SHA-256");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String sha256(String source) {
        return source != null
                ? DatatypeConverter.printHexBinary(digest.digest(source.getBytes(StandardCharsets.UTF_8))).toLowerCase()
                : null;
    }

    @Override
    public void onEvent(Event event) {
        if (dropUsername) {
            Map<String, String> details = event.getDetails();
            if (details.containsKey("username")) {
                details.remove("username");
            }
            event.setDetails(details);
        } else if (hashUsername) {
            Map<String, String> details = event.getDetails();
            if (details.containsKey("username")) {
                details.put("username", sha256(details.get("username")));
            }
            event.setDetails(details);
        }

        if (dropSessionId) {
            event.setSessionId(null);
        } else if (hashSessionId) {
            event.setSessionId(sha256(event.getSessionId()));
        }
        if (dropIpAdderss) {
            event.setIpAddress(null);
        } else if (hashIpAdderss) {
            event.setIpAddress(sha256(event.getIpAddress()));
        }

        super.onEvent(event);
    }
}
