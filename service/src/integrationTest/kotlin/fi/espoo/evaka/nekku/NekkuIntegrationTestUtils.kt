// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals

fun assertOrdersListEquals(
    expected: List<NekkuClient.NekkuOrders>,
    actual: List<NekkuClient.NekkuOrders>,
) {
    assertEquals(expected.size, actual.size)
    expected.zip(actual).forEach { (expected, actual) -> assertNekkuOrdersEquals(expected, actual) }
}

fun assertNekkuOrdersEquals(expected: NekkuClient.NekkuOrders, actual: NekkuClient.NekkuOrders) {
    assertEquals(expected.dryRun, actual.dryRun)
    expected.orders.zip(actual.orders).forEach { (expected, actual) ->
        assertOrderEquals(expected, actual)
    }
}

fun assertOrderEquals(expected: NekkuClient.NekkuOrder, actual: NekkuClient.NekkuOrder) {
    assertEquals(expected.deliveryDate, actual.deliveryDate)
    assertEquals(expected.customerNumber, actual.customerNumber)
    assertEquals(expected.groupId, actual.groupId)
    assertEquals(expected.description, actual.description)
    assertEquals(expected.items.toSet(), actual.items.toSet())
}

fun getAuthenticatedEmployee(db: Database.Connection): AuthenticatedUser.Employee {
    val employee =
        DevEmployee(id = EmployeeId(UUID.randomUUID()), firstName = "Test", lastName = "Employee")
    db.transaction { tx -> tx.insert(employee) }
    return AuthenticatedUser.Employee(employee.id, setOf(UserRole.ADMIN))
}

fun getClock(): MockEvakaClock =
    MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2025, 4, 15), LocalTime.of(12, 0)))

fun insertCustomerNumbers(db: Database.Connection) =
    db.transaction { tx ->
        tx.createUpdate {
                sql(
                    "INSERT INTO nekku_customer VALUES" +
                        "('1234', 'Lönnrotinkadun päiväkoti', 'Varhaiskasvatus')," +
                        "('5678', 'Rubeberginkadun päiväkoti', 'Varhaiskasvatus')"
                )
            }
            .execute()
    }

fun insertSpecialDiets(db: Database.Connection) {
    db.transaction { tx ->
        tx.createUpdate { sql("INSERT INTO nekku_special_diet VALUES ('2', 'Päiväkodit ER')") }
            .execute()

        tx.createUpdate {
                sql(
                    "INSERT INTO nekku_special_diet_field VALUES" +
                        "('12345678-9ABC-DEF0-1234-56789ABCDEF0', 'Erityisruokavaliot', 'CHECKBOXLIST', '2')," +
                        "('56789ABC-DEF0-1234-5678-9ABCDEF01234', 'Muu erityisruokavalio', 'TEXT', '2')"
                )
            }
            .execute()

        tx.createUpdate {
                sql(
                    "INSERT INTO nekku_special_diet_option VALUES" +
                        "(1, 'Kananmunaton', 'Kananmunaton', '12345678-9ABC-DEF0-1234-56789ABCDEF0')," +
                        "(2, 'Sianlihaton', 'Sianlihaton', '12345678-9ABC-DEF0-1234-56789ABCDEF0')"
                )
            }
            .execute()
    }
}

fun insertNekkuSpecialDietChoice(
    db: Database.Connection,
    childId: ChildId,
    dietId: String,
    dietField: String,
    dietValue: String,
) {
    db.transaction { tx ->
        tx.createUpdate {
                sql(
                    "INSERT INTO nekku_special_diet_choices VALUES (" +
                        "${bind(childId)}," +
                        "${bind(dietId)}," +
                        "${bind(dietField)}," +
                        "${bind(dietValue)}" +
                        ")"
                )
            }
            .execute()
    }
}

fun getNekkuJobs(db: Database.Connection) =
    db.read { tx ->
        tx.createQuery { sql("SELECT payload FROM async_job WHERE type = 'SendNekkuOrder'") }
            .map { jsonColumn<AsyncJob.SendNekkuOrder>("payload") }
            .toList()
    }

class TestNekkuClient(
    private val customers: List<NekkuApiCustomer> = emptyList(),
    private val specialDiets: List<NekkuApiSpecialDiet> = emptyList(),
    private val nekkuProducts: List<NekkuApiProduct> = emptyList(),
) : NekkuClient {
    val orders = mutableListOf<NekkuClient.NekkuOrders>()

    override fun getCustomers(): List<NekkuCustomer> {
        return customers.map { it.toEvaka() }
    }

    override fun getSpecialDiets(): List<NekkuSpecialDiet> {
        return specialDiets.map { it.toEvaka() }
    }

    override fun getProducts(): List<NekkuProduct> {
        return nekkuProducts.map { it.toEvaka() }
    }

    override fun createNekkuMealOrder(nekkuOrders: NekkuClient.NekkuOrders): NekkuOrderResult {
        orders.add(nekkuOrders)

        return NekkuOrderResult(
            message = "Input ok, 1 orders would be created.",
            created = listOf("12345"),
            cancelled = listOf("65432"),
        )
    }
}

class DeserializingTestNekkuClient(
    private val jsonMapper: JsonMapper,
    private val customers: String = "",
    private val specialDiets: String = "",
    private val nekkuProducts: String = "",
) : NekkuClient {
    private val orders = mutableListOf<NekkuClient.NekkuOrders>()

    override fun getCustomers(): List<NekkuCustomer> {
        return jsonMapper.readValue<List<NekkuApiCustomer>>(customers).map { it.toEvaka() }
    }

    override fun getSpecialDiets(): List<NekkuSpecialDiet> {
        return jsonMapper.readValue<List<NekkuApiSpecialDiet>>(specialDiets).map { it.toEvaka() }
    }

    override fun getProducts(): List<NekkuProduct> {
        return jsonMapper.readValue<List<NekkuApiProduct>>(nekkuProducts).map { it.toEvaka() }
    }

    override fun createNekkuMealOrder(nekkuOrders: NekkuClient.NekkuOrders): NekkuOrderResult {
        orders.add(nekkuOrders)

        return NekkuOrderResult(
            message = "Input ok, 5 orders would be created.",
            created = listOf("12345", "65432"),
            cancelled = emptyList(),
        )
    }
}

class FailingNekkuClient(
    private val customers: List<NekkuApiCustomer> = emptyList(),
    private val specialDiets: List<NekkuApiSpecialDiet> = emptyList(),
    private val nekkuProducts: List<NekkuApiProduct> = emptyList(),
) : NekkuClient {
    val orders = mutableListOf<NekkuClient.NekkuOrders>()

    override fun getCustomers(): List<NekkuCustomer> {
        return customers.map { it.toEvaka() }
    }

    override fun getSpecialDiets(): List<NekkuSpecialDiet> {
        return specialDiets.map { it.toEvaka() }
    }

    override fun getProducts(): List<NekkuProduct> {
        return nekkuProducts.map { it.toEvaka() }
    }

    override fun createNekkuMealOrder(nekkuOrders: NekkuClient.NekkuOrders): NekkuOrderResult {
        throw RuntimeException("Test failure")
    }
}
