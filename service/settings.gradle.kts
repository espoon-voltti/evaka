// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

rootProject.name = "evaka-service"
include("service-lib")
include("vtjclient")
include("sficlient")
include("evaka-bom")
include("codegen")
include("holidays")

project(":service-lib").projectDir = file("../service-lib")
project(":evaka-bom").projectDir = file("../evaka-bom")
