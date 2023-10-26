// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import fi.espoo.evaka.shared.db.DatabaseEnum

enum class PushNotificationCategory : DatabaseEnum {
    RECEIVED_MESSAGE;

    override val sqlType: String = "push_notification_category"
}
