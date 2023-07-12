package fi.espoo.evaka.childdiscussion

import fi.espoo.evaka.shared.ChildDiscussionId
import fi.espoo.evaka.shared.ChildId
import java.time.LocalDate

data class ChildDiscussion (
    val id: ChildDiscussionId,
    val childId: ChildId,
    val offeredDate: LocalDate?,
    val heldDate: LocalDate?,
    val counselingDate: LocalDate?
)

data class ChildDiscussionBody(
    val offeredDate: LocalDate?,
    val heldDate: LocalDate?,
    val counselingDate: LocalDate?
)

