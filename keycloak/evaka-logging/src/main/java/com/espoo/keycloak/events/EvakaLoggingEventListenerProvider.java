// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package com.espoo.keycloak.events;

import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import org.keycloak.events.Event;

public class EvakaLoggingEventListenerProvider implements EventListenerProvider {
    private final EventListenerProvider inner;
    private final Preprocessor preprocessor;

    public EvakaLoggingEventListenerProvider(EventListenerProvider inner, Preprocessor preprocessor) {
        this.inner = inner;
        this.preprocessor = preprocessor;
    }

    @Override
    public void onEvent(Event event) {
        this.preprocessor.preprocess(event);
        this.inner.onEvent(event);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        this.inner.onEvent(event, includeRepresentation);
    }

    @Override
    public void close() {
        this.inner.close();
    }
}
