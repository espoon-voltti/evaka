// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import java.time.LocalDate

interface HasDateOfBirth {
    val dateOfBirth: LocalDate
}

fun <P : Any, C : HasDateOfBirth> determineHeadOfFamily(
    firstParent: Pair<P, List<C>>,
    secondParent: Pair<P?, List<C>?>,
): Pair<P, P?> {
    val parent1 = firstParent.first
    val parent2 = secondParent.first
    val firstParentChildren = firstParent.second
    val secondParentChildren = secondParent.second
    val firstParentsYoungestChild = firstParentChildren.minByOrNull { it.dateOfBirth }
    val secondParentsYoungestChild = secondParentChildren?.minByOrNull { it.dateOfBirth }
    return when {
        parent2 == null -> parent1 to parent2
        firstParentsYoungestChild == null -> parent2 to parent1
        secondParentsYoungestChild == null -> parent1 to parent2
        // First parent has more fridge children
        firstParentChildren.size > secondParentChildren.size -> parent1 to parent2
        // Second parent has more fridge children
        firstParentChildren.size < secondParentChildren.size -> parent2 to parent1
        // First parent has the youngest fridge child
        firstParentsYoungestChild.dateOfBirth.isAfter(secondParentsYoungestChild.dateOfBirth) ->
            parent1 to parent2
        else -> parent2 to parent1
    }
}
