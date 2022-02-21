// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen

import evaka.codegen.actionenum.cli.generateActionEnumTypes
import evaka.codegen.apitypes.generateApiTypes

fun main() {
    generateActionEnumTypes()
    generateApiTypes()
}
