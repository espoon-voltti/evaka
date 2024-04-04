// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package com.espoo.keycloak.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.log.JBossLoggingEventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;

public class EvakaLoggingEventListenerProviderFactory extends JBossLoggingEventListenerProviderFactory {
    public static final String ID = "evaka-logging";

    private Preprocessor preprocessor;
    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new EvakaLoggingEventListenerProvider(super.create(session), preprocessor);
    }

    @Override
    public void init(Config.Scope config) {
        super.init(config);
        preprocessor = Preprocessor.fromEnvironment(System.getenv());
    }

    @Override
    public String getId() {
        return ID;
    }
}
