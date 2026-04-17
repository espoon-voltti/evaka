// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing

import evaka.core.FullApplicationTest
import evaka.core.attachment.Attachment
import evaka.core.attachment.AttachmentParent
import evaka.core.attachment.AttachmentsController
import evaka.core.attachment.getAttachment
import evaka.core.invoicing.controller.FeeAlterationController
import evaka.core.invoicing.data.upsertFeeAlteration
import evaka.core.invoicing.domain.FeeAlteration
import evaka.core.invoicing.domain.FeeAlterationType
import evaka.core.shared.AttachmentId
import evaka.core.shared.FeeAlterationId
import evaka.core.shared.PersonId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.RealEvakaClock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class FeeAlterationIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var feeAlterationController: FeeAlterationController
    @Autowired private lateinit var attachmentsController: AttachmentsController

    private val employee = DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN))
    private val child = DevPerson()

    private fun assertEqualEnough(expected: List<FeeAlteration>, actual: List<FeeAlteration>) {
        val nullId = FeeAlterationId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        assertEquals(
            expected.map { it.copy(id = nullId, modifiedAt = null) }.toSet(),
            actual.map { it.copy(id = nullId, modifiedAt = null) }.toSet(),
        )
    }

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
        }
    }

    private val testFeeAlterationId = FeeAlterationId(UUID.randomUUID())
    private val user = employee.user
    private val personId = child.id
    private val clock = MockEvakaClock(2019, 1, 10, 12, 0)

    private val testFeeAlteration =
        FeeAlteration(
            id = testFeeAlterationId,
            personId = personId,
            type = FeeAlterationType.DISCOUNT,
            amount = 50,
            isAbsolute = false,
            validFrom = LocalDate.of(2019, 1, 1),
            validTo = LocalDate.of(2019, 1, 31),
            notes = "",
            modifiedBy = employee.evakaUser,
        )

    @Test
    fun `getFeeAlterations works with no data in DB`() {
        val result = getFeeAlterations(personId)
        assertEquals(0, result.size)
    }

    @Test
    fun `getFeeAlterations works with single fee alteration in DB`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, user.evakaUserId, testFeeAlteration) }

        val result = getFeeAlterations(personId)
        assertEqualEnough(listOf(testFeeAlteration), result.map { it.data })
    }

    @Test
    fun `getFeeAlterations works with multiple fee alterations in DB`() {
        val feeAlterations =
            listOf(
                testFeeAlteration.copy(
                    id = FeeAlterationId(UUID.randomUUID()),
                    validFrom = testFeeAlteration.validFrom.plusYears(1),
                    validTo = testFeeAlteration.validTo!!.plusYears(1),
                ),
                testFeeAlteration,
            )
        db.transaction { tx ->
            feeAlterations.forEach { tx.upsertFeeAlteration(clock, user.evakaUserId, it) }
        }

        val result = getFeeAlterations(personId)
        assertEqualEnough(feeAlterations, result.map { it.data })
    }

    @Test
    fun `createFeeAlterations works with valid fee alteration`() {
        createFeeAlteration(testFeeAlteration)

        val result = getFeeAlterations(personId)
        assertEqualEnough(
            listOf(testFeeAlteration.copy(modifiedBy = employee.evakaUser)),
            result.map { it.data },
        )
    }

    @Test
    fun `createFeeAlterations throws with invalid date range`() {
        val feeAlteration =
            testFeeAlteration.copy(validTo = testFeeAlteration.validFrom.minusDays(1))
        assertThrows<BadRequest> { createFeeAlteration(feeAlteration) }
    }

    @Test
    fun `updateFeeAlteration works with valid fee alteration`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, user.evakaUserId, testFeeAlteration) }

        val updated = testFeeAlteration.copy(amount = 100)
        updateFeeAlteration(testFeeAlteration.id!!, updated)

        val result = getFeeAlterations(personId)
        assertEqualEnough(
            listOf(updated.copy(modifiedBy = employee.evakaUser)),
            result.map { it.data },
        )
    }

    @Test
    fun `updateFeeAlteration throws with invalid date rage`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, user.evakaUserId, testFeeAlteration) }

        val updated = testFeeAlteration.copy(validTo = testFeeAlteration.validFrom.minusDays(1))
        assertThrows<BadRequest> { updateFeeAlteration(testFeeAlteration.id!!, updated) }
    }

    @Test
    fun `delete works with existing fee alteration`() {
        val deletedId = FeeAlterationId(UUID.randomUUID())
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, user.evakaUserId, testFeeAlteration)
            tx.upsertFeeAlteration(clock, user.evakaUserId, testFeeAlteration.copy(id = deletedId))
        }

        deleteFeeAlteration(deletedId)

        val result = getFeeAlterations(personId)
        assertEquals(1, result.size)
        assertFalse(result.any { it.data.id == deletedId })
    }

    @Test
    fun `delete does nothing with non-existent id`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, user.evakaUserId, testFeeAlteration) }

        deleteFeeAlteration(FeeAlterationId(UUID.randomUUID()))

        val result = getFeeAlterations(personId)
        assertEqualEnough(listOf(testFeeAlteration), result.map { it.data })
    }

    @Test
    fun `add an attachment`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, user.evakaUserId, testFeeAlteration) }

        val attachmentId = uploadAttachment(testFeeAlterationId)

        val result = getFeeAlterations(personId)
        assertEqualEnough(
            listOf(
                testFeeAlteration.copy(
                    attachments = listOf(Attachment(attachmentId, "evaka-logo.png", "image/png"))
                )
            ),
            result.map { it.data },
        )
    }

    @Test
    fun `attachment gets orphaned on fee alteration deletion`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, user.evakaUserId, testFeeAlteration) }
        val attachmentId = uploadAttachment(testFeeAlterationId)

        val result = getFeeAlterations(personId)
        assertEqualEnough(
            listOf(
                testFeeAlteration.copy(
                    attachments = listOf(Attachment(attachmentId, "evaka-logo.png", "image/png"))
                )
            ),
            result.map { it.data },
        )

        deleteFeeAlteration(testFeeAlterationId)
        val attachment = db.read { tx -> tx.getAttachment(attachmentId) }
        assertEquals(AttachmentParent.None, attachment?.second)
    }

    private fun createFeeAlteration(body: FeeAlteration) {
        feeAlterationController.createFeeAlteration(dbInstance(), user, RealEvakaClock(), body)
    }

    private fun updateFeeAlteration(id: FeeAlterationId, body: FeeAlteration) {
        feeAlterationController.updateFeeAlteration(dbInstance(), user, RealEvakaClock(), id, body)
    }

    private fun getFeeAlterations(
        personId: PersonId
    ): List<FeeAlterationController.FeeAlterationWithPermittedActions> {
        return feeAlterationController.getFeeAlterations(dbInstance(), user, clock, personId)
    }

    private fun deleteFeeAlteration(id: FeeAlterationId) {
        feeAlterationController.deleteFeeAlteration(dbInstance(), user, RealEvakaClock(), id)
    }

    private fun uploadAttachment(id: FeeAlterationId): AttachmentId {
        return attachmentsController.uploadFeeAlterationAttachment(
            dbInstance(),
            user,
            clock,
            id,
            MockMultipartFile("file", "evaka-logo.png", "image/png", pngFile.readBytes()),
        )
    }
}
