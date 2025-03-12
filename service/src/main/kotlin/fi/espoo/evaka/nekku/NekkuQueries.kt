// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.decision.logger
import fi.espoo.evaka.invoicing.domain.DraftInvoiceRow
import fi.espoo.evaka.shared.db.Database
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
        client.getSpecialDiets().map {
            NekkuSpecialDiet(
                it.id,
                it.name,
                it.fields.map { field ->
                    NekkuSpecialDietsField(
                        field.id,
                        field.name,
                        field.type,
                        field.options?.map { option ->
                            NekkuSpecialDietOption(option.weight, option.key, option.value)
                        },
                    )
                },
            )
        }

    if (specialDietsFromNekku.isEmpty())
        error("Refusing to sync empty Nekku special diet list into database")

    db.transaction { tx ->

        tx.setSpecialDiets(specialDietsFromNekku)
        // Save nekku fields
        tx.setSpecialDietFields(specialDietsFromNekku.map { it.id to it.fields })

        // Save nekku options

    }
}

fun Database.Transaction.setSpecialDiets(specialDiets: List<NekkuSpecialDiet>) {
    val newSpecialDiets = specialDiets.map { it.id }
    val deletedSpecialDietsCount = execute {
        sql("DELETE FROM nekku_special_diet WHERE id != ALL (${bind(newSpecialDiets)})")
    }
    executeBatch(specialDiets) {
        sql(
            """
    INSERT INTO nekku_special_diet (
    id,
    name
) VALUES (
    ${bind { it.id }},
    ${bind { it.name }}
)
"""
        )
    }

    logger.info {
        "Deleted: $deletedSpecialDietsCount Nekku special diets, inserted ${specialDiets.size}"
    }
}

fun Database.Transaction.getNekkuSpecialDiets(): Int {
    val count = execute { sql("SELECT count(*) FROM nekku_special_diet") }
    return count
}

 fun Database.Transaction.setSpecialDietFields(specialDietFields: List<Pair<String, List<NekkuSpecialDietsField>>>){

     val batchRows: Sequence<Pair<String, NekkuSpecialDietsField>> =
         specialDietFields.asSequence().flatMap { (dietId, fields) ->
             fields.map { field -> Pair(dietId, field) }
         }


    executeBatch(batchRows) {
        sql(
            """
INSERT INTO nekku_special_diet_field (
    diet_id,
    id,
    name,
    type
) VALUES (
    ${bind { (dietId, _) ->  dietId}},
    ${bind { (_, field) ->  field.id }},
    ${bind { (_, field) ->  field.name }},
    ${bind { (_, field) ->  field.type }}
)
 """
        )
    }

    logger.info {
        "Inserted Nekku special diet fields ${specialDietFields.size}"
    }
 }
