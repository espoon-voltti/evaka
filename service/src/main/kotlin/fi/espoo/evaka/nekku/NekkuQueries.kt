// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.decision.logger
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.specialdiet.SpecialDiet
import org.jdbi.v3.core.mapper.PropagateNull

data class CustomerNumbers(
    @PropagateNull val number: String,
    val name: String,
    val group: String,
    val unit_size: String,
)

/** Throws an IllegalStateException if Nekku returns an empty customer list. */
fun fetchAndUpdateNekkuCustomers(client: NekkuClient, db: Database.Connection) {
    val customersFromNekku =
        client
            .getCustomers()
            .map { it -> CustomerNumbers(it.number, it.name, it.group, it.unit_size) }
            .filter { it.group.contains("Varhaiskasvatus") }

    if (customersFromNekku.isEmpty())
        error("Refusing to sync empty Nekku customer list into database")
    db.transaction { tx ->
        val nulledCustomersCount =
            tx.resetNekkuCustomerNumbersNotContainedWithin(customersFromNekku)
        if (nulledCustomersCount != 0)
            logger.warn {
                "Nekku customer list update caused $nulledCustomersCount customer numbers to be set to null"
            }
        val deletedCustomerCount = tx.setCustomerNumbers(customersFromNekku)
        logger.info {
            "Deleted: $deletedCustomerCount Nekku customer numbers, inserted ${customersFromNekku.size}"
        }
    }
}

fun Database.Transaction.resetNekkuCustomerNumbersNotContainedWithin(
    nekkuCustomerNumbers: List<CustomerNumbers>
): Int {
    val newNekkuCustomerNumbers = nekkuCustomerNumbers.map { it.number }
    val affectedRows = execute {
        sql(
            "UPDATE daycare_group SET nekku_customer_number = null WHERE nekku_customer_number != ALL (${bind(newNekkuCustomerNumbers)})"
        )
    }
    return affectedRows
}

fun Database.Transaction.setCustomerNumbers(customerNumbers: List<CustomerNumbers>): Int {
    val newCustomerNumbers = customerNumbers.map { it.number }
    val deletedCustomerCount = execute {
        sql("DELETE FROM nekku_customer WHERE number != ALL (${bind(newCustomerNumbers)})")
    }
    executeBatch(customerNumbers) {
        sql(
            """
INSERT INTO nekku_customer (number, name, customer_group, unit_size)
VALUES (
    ${bind{it.number}},
    ${bind{it.name}},
    ${bind{it.group}},
    ${bind{it.unit_size}}
)
ON CONFLICT (number) DO 
UPDATE SET
  name = excluded.name,
  customer_group = excluded.customer_group,
  unit_size = excluded.unit_size
WHERE
    nekku_customer.name <> excluded.name OR
    nekku_customer.customer_group <> excluded.customer_group OR
    nekku_customer.unit_size <> excluded.unit_size;
"""
        )
    }
    return deletedCustomerCount
}

fun Database.Transaction.getNekkuCustomers(): List<NekkuCustomer> {
    return createQuery {
            sql("SELECT number, name, customer_group AS \"group\", unit_size FROM nekku_customer")
        }
        .toList<NekkuCustomer>()
}

/** Throws an IllegalStateException if Nekku returns an empty special diet list. */
fun fetchAndUpdateNekkuSpecialDiets(client: NekkuClient, db: Database.Connection) {
    val specialDietsFromNekku =
        client.getSpecialDiets().map { NekkuSpecialDiet(it.id, it.name, it.fields) }

    if (specialDietsFromNekku.isEmpty())
        error("Refusing to sync empty Nekku special diet list into database")
    db.transaction { tx ->
//        val nulledSpecialDietsCount = tx.resetNekkuSpecialDiets(specialDietsFromNekku)
//        if (nulledSpecialDietsCount != 0)
//            logger.warn {
//                "Nekku special diet list update caused $nulledSpecialDietsCount special diets to be set to null"
//            }
        val deletedSpecialDietsCount = tx.setSpecialDiets(specialDietsFromNekku)
        logger.info {
            "Deleted: $deletedSpecialDietsCount Nekku special diets, inserted ${specialDietsFromNekku.size}"
        }
    }
}

fun Database.Transaction.setSpecialDiets(specialDiets: List<NekkuSpecialDiet>): Int {
    val newSpecialDiets = specialDiets.map { it.id }
    val deletedCustomerCount = execute {
        sql("DELETE FROM nekku_special_diet WHERE number != ALL (${bind(newSpecialDiets)})")
    }
    executeBatch(specialDiets) {
        sql(
            """
//INSERT INTO nekku_customer (number, name, customer_group, unit_size)
//VALUES (
//    ${bind{it.number}},
//    ${bind{it.name}},
//    ${bind{it.customergroup}},
//    ${bind{it.unit_size}}
//)
//ON CONFLICT (number) DO 
//UPDATE SET
//  name = excluded.name,
//  customer_group = excluded.customer_group,
//  unit_size = excluded.unit_size
//WHERE
//    nekku_customer.name <> excluded.name OR
//    nekku_customer.customer_group <> excluded.customer_group OR
//    nekku_customer.unit_size <> excluded.unit_size;
"""
        )
    }
    return deletedCustomerCount
}
