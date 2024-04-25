// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.db.DatabaseEnum

enum class CreateSource : DatabaseEnum {
    USER,
    APPLICATION,
    DVV;

    override val sqlType = "create_source"
}

sealed class Creator {
    abstract val source: CreateSource

    data class User(val id: EvakaUserId) : Creator() {
        override val source: CreateSource
            get() = CreateSource.USER
    }

    data class Application(val id: ApplicationId) : Creator() {
        override val source: CreateSource
            get() = CreateSource.APPLICATION
    }

    data object DVV : Creator() {
        override val source: CreateSource
            get() = CreateSource.DVV
    }
}

enum class ModifySource : DatabaseEnum {
    USER,
    DVV;

    override val sqlType = "modify_source"
}

sealed class Modifier {
    abstract val source: ModifySource

    data class User(val id: EvakaUserId) : Modifier() {
        override val source: ModifySource
            get() = ModifySource.USER
    }

    data object DVV : Modifier() {
        override val source: ModifySource
            get() = ModifySource.DVV
    }
}
