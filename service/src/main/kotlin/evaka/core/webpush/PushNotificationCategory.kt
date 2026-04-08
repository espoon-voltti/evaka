// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.ConstList
import evaka.core.shared.db.DatabaseEnum

@ConstList("pushNotificationCategories")
enum class PushNotificationCategory : DatabaseEnum {
    RECEIVED_MESSAGE,
    NEW_ABSENCE,
    CALENDAR_EVENT_RESERVATION;

    override val sqlType: String = "push_notification_category"
}
