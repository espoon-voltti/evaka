// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.actionenum

import fi.espoo.evaka.shared.security.Action
import java.nio.file.Path
import kotlin.io.path.writeText

val generatedFiles =
    listOf(
        defineFile(
            "action.d.ts",
            generateNamespace(
                "Action",
                generateEnum<Action.Global>(),
                generateEnum<Action.Application>(),
                generateEnum<Action.ApplicationNote>(),
                generateEnum<Action.AssistanceAction>(),
                generateEnum<Action.AssistanceFactor>(),
                generateEnum<Action.AssistanceNeedDecision>(),
                generateEnum<Action.AssistanceNeedPreschoolDecision>(),
                generateEnum<Action.AssistanceNeedVoucherCoefficient>(),
                generateEnum<Action.BackupCare>(),
                generateEnum<Action.BackupPickup>(),
                generateEnum<Action.Child>(),
                generateEnum<Action.ChildDailyNote>(),
                generateEnum<Action.ChildDocument>(),
                generateEnum<Action.DailyServiceTime>(),
                generateEnum<Action.DaycareAssistance>(),
                generateEnum<Action.Decision>(),
                generateEnum<Action.FeeAlteration>(),
                generateEnum<Action.Group>(),
                generateEnum<Action.GroupPlacement>(),
                generateEnum<Action.Income>(),
                generateEnum<Action.Invoice>(),
                generateEnum<Action.InvoiceCorrection>(),
                generateEnum<Action.MobileDevice>(),
                generateEnum<Action.OtherAssistanceMeasure>(),
                generateEnum<Action.Pairing>(),
                generateEnum<Action.Parentship>(),
                generateEnum<Action.Partnership>(),
                generateEnum<Action.Payment>(),
                generateEnum<Action.Person>(),
                generateEnum<Action.Placement>(),
                generateEnum<Action.PreschoolAssistance>(),
                generateEnum<Action.ServiceNeed>(),
                generateEnum<Action.Unit>(),
                generateEnum<Action.VasuDocument>(),
                generateEnum<Action.VasuTemplate>(),
                generateNamespace(
                    "Citizen",
                    generateEnum<Action.Citizen.Application>(),
                    generateEnum<Action.Citizen.Child>()
                ),
            )
        )
    )

class FileDefinition(val name: String, private val generators: Array<out Generator>) {
    fun generate(): String = generateFileContents(*generators)

    fun generateTo(path: Path) = path.writeText(generate())
}

private fun defineFile(name: String, vararg generators: Generator): FileDefinition =
    FileDefinition(name, generators)
