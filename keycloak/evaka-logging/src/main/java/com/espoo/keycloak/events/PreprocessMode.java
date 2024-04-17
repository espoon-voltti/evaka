// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package com.espoo.keycloak.events;

/**
 * Preprocessing mode for a log event property
 */
public enum PreprocessMode {
    /**
     * Keep the value if it exists
     */
    KEEP,
    /**
     * Hash the value using SHA-256
     */
    HASH,
    /**
     * Drop the value from the log event
     */
    DROP
}
