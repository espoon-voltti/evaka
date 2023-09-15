// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("$rootDir/../gradle/libs.versions.toml"))
        }
    }
}
