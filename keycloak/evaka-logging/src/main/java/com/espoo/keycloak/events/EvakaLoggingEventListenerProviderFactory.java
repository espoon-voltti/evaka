// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package com.espoo.keycloak.events;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class EvakaLoggingEventListenerProviderFactory implements EventListenerProviderFactory {
    public static final String ID = "evaka-logging";

    private static final Logger logger = Logger.getLogger("org.keycloak.events");

    private Logger.Level successLevel;
    private Logger.Level errorLevel;
    private boolean sanitize;
    private Character quotes;

    private boolean hashUsername;
    private boolean dropUsername;

    private boolean hashSessionId;
    private boolean dropSessionId;

    private boolean hashIpAdderss;
    private boolean dropIpAdderss;

    private boolean hashIdentity;
    private boolean dropIdentity;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new EvakaLoggingEventListenerProvider(session, logger, successLevel, errorLevel, quotes, sanitize,
                hashUsername, dropUsername, hashSessionId, dropSessionId, hashIpAdderss, dropIpAdderss,
                hashIdentity, dropIdentity);
    }

    private boolean getBooleanEnvironment(String variable) {
        return System.getenv(variable) != null && System.getenv(variable).trim().equals("true");
    }

    @Override
    public void init(Config.Scope config) {
        successLevel = Logger.Level.valueOf(config.get("success-level", "debug").toUpperCase());
        errorLevel = Logger.Level.valueOf(config.get("error-level", "warn").toUpperCase());
        sanitize = config.getBoolean("sanitize", true);
        String quotesString = config.get("quotes", "\"");
        if (!quotesString.equals("none") && quotesString.length() > 1) {
            logger.warn("Invalid quotes configuration, it should be none or one character to use as quotes. Using default \" quotes");
            quotesString = "\"";
        }
        quotes = quotesString.equals("none")? null : quotesString.charAt(0);

        hashUsername = getBooleanEnvironment("LOG_HASH_USERNAME");
        dropUsername = getBooleanEnvironment("LOG_DROP_USERNAME");

        hashSessionId = getBooleanEnvironment("LOG_HASH_SESSION_ID");
        dropSessionId = getBooleanEnvironment("LOG_DROP_SESSION_ID");

        hashIpAdderss = getBooleanEnvironment("LOG_HASH_IP_ADDRESS");
        dropIpAdderss = getBooleanEnvironment("LOG_DROP_IP_ADDRESS");

        hashIdentity = getBooleanEnvironment("LOG_HASH_IDENTITY");
        dropIdentity = getBooleanEnvironment("LOG_DROP_IDENTITY");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

}
