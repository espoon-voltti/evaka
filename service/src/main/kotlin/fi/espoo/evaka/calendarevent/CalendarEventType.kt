// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.shared.db.DatabaseEnum

enum class CalendarEventType : DatabaseEnum {
    DAYCARE_EVENT,
    DISCUSSION_SURVEY;

    override val sqlType: String = "calendar_event_type"
}
