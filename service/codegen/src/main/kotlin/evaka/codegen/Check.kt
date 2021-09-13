// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen

import evaka.codegen.actionenum.cli.checkGeneratedActionEnumTypes
import evaka.codegen.apitypes.checkGeneratedApiTypes

fun main() {
    checkGeneratedActionEnumTypes()
    checkGeneratedApiTypes()
}
