// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.mapper.PropagateNull

data class CustomerNumbers(@PropagateNull val number: String, val name: String)

/** Throws an IllegalStateException if Jamix returns an empty texture list. */
fun fetchAndUpdateNekkuCustomers(
    client: NekkuClient,
    db: Database.Connection,
    warner: (s: String) -> Unit = loggerWarner,
) {
    val customersFromNekku =
        client.getCustomers().map { it -> CustomerNumbers(it.number, it.name) }

    if (customersFromNekku.isEmpty())
        error("Refusing to sync empty Nekku customer list into database")
    db.transaction { tx ->
        val nulledCustomersCount = tx.resetNekkuCustomerNumbersNotContainedWithin(customersFromNekku)
        if (nulledCustomersCount != 0)
            warner(
                "Nekku custoner list update caused $nulledCustomersCount customer numbers to be set to null"
            )
        val deletedMealTexturesCount = tx.setCustomerNumbers(customersFromNekku)
        logger.info {
            "Deleted: $deletedMealTexturesCount Nekku customer numbers, inserted ${customersFromNekku.size}"
        }
    }
}

fun Database.Transaction.resetNekkuCustomerNumbersNotContainedWithin(
    nekkuCustomerNumbers: List<CustomerNumbers>
): Int {
    val newMealTextureIds = nekkuCustomerNumbers.map { it.number }
    val affectedRows = execute {
        sql(
            "UPDATE daycare_group SET nekku_customer_number = null WHERE nekku_customer_number != ALL (${bind(newMealTextureIds)})"
        )
    }
    return affectedRows
}


fun Database.Transaction.setCustomerNumbers(customerNumbers: List<CustomerNumbers>): Int {
    val newCustomerNumbers = customerNumbers.map { it.number }
    val deletedCustomerCount = execute {
        sql("DELETE FROM nekku_customer_number WHERE number != ALL (${bind(newCustomerNumbers)})")
    }
    executeBatch(newCustomerNumbers) {
        sql(
            """
INSERT INTO nekku_customer_number (number, name)
VALUES (
    ${bind{it.number}},
    ${bind{it.name}}
)
ON CONFLICT (number) DO UPDATE SET
  name = excluded.name
"""
        )
    }
    return deletedCustomerCount
}