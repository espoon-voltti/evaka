package evaka.codegen

import evaka.codegen.actionenum.cli.generateActionEnumTypes
import evaka.codegen.apitypes.generateApiTypes

fun main() {
    generateActionEnumTypes()
    generateApiTypes()
}
