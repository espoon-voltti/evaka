// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.actionenum

import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesType
import fi.espoo.evaka.incomestatement.IncomeSource
import fi.espoo.evaka.incomestatement.OtherIncome
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
            generateEnum<Action.ApplicationNote>(),
            generateEnum<Action.AssistanceAction>(),
            generateEnum<Action.AssistanceNeed>(),
            generateEnum<Action.BackupCare>(),
            generateEnum<Action.BackupPickup>(),
            generateEnum<Action.Child>(),
            generateEnum<Action.ChildDailyNote>(),
            generateEnum<Action.Decision>(),
            generateEnum<Action.Group>(),
            generateEnum<Action.GroupPlacement>(),
            generateEnum<Action.MobileDevice>(),
            generateEnum<Action.Pairing>(),
            generateEnum<Action.Payment>(),
            generateEnum<Action.Person>(),
            generateEnum<Action.Placement>(),
            generateEnum<Action.ServiceNeed>(),
            generateEnum<Action.Unit>(),
            generateEnum<Action.VasuDocument>(),
            generateEnum<Action.VasuDocumentFollowup>(),
            generateEnum<Action.VasuTemplate>(),
        )
    ),
    defineFile(
        "enums.d.ts",
        generateEnum<DailyServiceTimesType>(),
        generateEnum<IncomeSource>(),
        generateEnum<OtherIncome>(),
    )
)

class FileDefinition(val name: String, private val generators: Array<out Generator>) {
    fun generate(): String = generateFileContents(*generators)
    fun generateTo(path: Path) = path.writeText(generate())
}

private fun defineFile(name: String, vararg generators: Generator): FileDefinition = FileDefinition(name, generators)
