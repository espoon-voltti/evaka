// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.security.Action
import java.nio.file.Path
import kotlin.io.path.writeText

val generatedFiles = listOf(
    defineFile(
        "action.d.ts",
        generateNamespace(
            "Action",
            generateEnum<Action.Global>(),
            generateEnum<Action.Application>(),
            generateEnum<Action.AssistanceAction>(),
            generateEnum<Action.AssistanceNeed>(),
            generateEnum<Action.BackupCare>(),
            generateEnum<Action.BackupPickup>(),
            generateEnum<Action.Child>(),
            generateEnum<Action.DailyNote>(),
            generateEnum<Action.Decision>(),
            generateEnum<Action.Group>(),
            generateEnum<Action.GroupPlacement>(),
            generateEnum<Action.MobileDevice>(),
            generateEnum<Action.Pairing>(),
            generateEnum<Action.Placement>(),
            generateEnum<Action.ServiceNeed>(),
            generateEnum<Action.Unit>(),
            generateEnum<Action.VasuDocument>(),
            generateEnum<Action.VasuTemplate>(),
        )
    ),
    defineFile(
        "enums.d.ts",
        generateEnum<PlacementType>(),
        generateEnum<ProviderType>(),
    )
)

class FileDefinition(val name: String, private val generators: Array<out Generator>) {
    fun generate(): String = generateFileContents(*generators)
    fun generateTo(path: Path) = path.writeText(generate())
}

private fun defineFile(name: String, vararg generators: Generator): FileDefinition = FileDefinition(name, generators)
