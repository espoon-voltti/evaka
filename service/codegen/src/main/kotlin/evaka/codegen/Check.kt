package evaka.codegen

import evaka.codegen.actionenum.cli.checkGeneratedActionEnumTypes
import evaka.codegen.apitypes.checkGeneratedApiTypes

fun main() {
    checkGeneratedActionEnumTypes()
    checkGeneratedApiTypes()
}
