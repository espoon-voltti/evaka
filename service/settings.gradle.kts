// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "evaka-service"

include("service-lib")

include("vtjclient")

include("sficlient")

include("evaka-bom")

include("codegen")

include("custom-ktlint-rules")

include("sarmamodel")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://build.shibboleth.net/maven/releases") {
            content {
                includeGroup("net.shibboleth")
                includeGroup("net.shibboleth.utilities")
                includeGroup("org.opensaml")
            }
        }
    }
}
