// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.decision.logger
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

    val specialDietsFromNekku = client.getSpecialDiets()

    if (specialDietsFromNekku.isEmpty())
        error("Refusing to sync empty Nekku special diet list into database")

    db.transaction { tx ->
        tx.setSpecialDiets(specialDietsFromNekku)

        tx.setSpecialDietFields(specialDietsFromNekku.map { it.id to it.fields })

        tx.setSpecialDietOptions(
            specialDietsFromNekku
                .flatMap { it.fields }
                .mapNotNull { if (it.options == null) null else it.id to it.options }
        )
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
ON CONFLICT (id) DO 
UPDATE SET
  name = excluded.name
WHERE
    nekku_special_diet.name <> excluded.name;
"""
        )
    }

    logger.info {
        "Deleted: $deletedSpecialDietsCount Nekku special diets, inserted ${specialDiets.size}"
    }
}

fun Database.Transaction.getNekkuSpecialOptions(): List<NekkuSpecialDietOption> {
    return createQuery { sql("SELECT weight, key, value FROM nekku_special_diet_option") }
        .toList<NekkuSpecialDietOption>()
}

fun Database.Transaction.setSpecialDietFields(
    specialDietFields: List<Pair<String, List<NekkuSpecialDietsField>>>
) {
    val newSpecialDietFieldIds = specialDietFields.flatMap { it.second }.map { it.id }
    val deletedSpecialDietFieldsCount = execute {
        sql(
            "DELETE FROM nekku_special_diet_field WHERE id != ALL (${bind(newSpecialDietFieldIds)})"
        )
    }

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
    ${bind { (dietId, _) -> dietId}},
    ${bind { (_, field) -> field.id }},
    ${bind { (_, field) -> field.name }},
    ${bind { (_, field) -> field.type }}
)
ON CONFLICT (id) DO 
UPDATE SET
  name = excluded.name,
  type = excluded.type
WHERE
    nekku_special_diet_field.name <> excluded.name OR 
    nekku_special_diet_field.type <> excluded.type;

"""
        )
    }

    logger.info {
        "Deleted: $deletedSpecialDietFieldsCount Nekku special diet fields, inserted ${specialDietFields.size}"
    }
}

fun Database.Transaction.setSpecialDietOptions(
    specialDietOptions: List<Pair<String, List<NekkuSpecialDietOption>>>
) {

    val deletedSpecialOptionsCount =
        executeBatch(specialDietOptions) {
            sql(
                """
DELETE FROM nekku_special_diet_option 
WHERE field_id = ${bind { (fieldId, _) -> fieldId}} 
AND value != ALL(${bind { (_, option) -> option.map { it.value } }});
            """
                    .trimIndent()
            )
        }

    val batchRows: Sequence<Pair<String, NekkuSpecialDietOption>> =
        specialDietOptions.asSequence().flatMap { (fieldId, options) ->
            options.map { option -> Pair(fieldId, option) }
        }

    executeBatch(batchRows) {
        sql(
            """
INSERT INTO nekku_special_diet_option (
    field_id,
    weight,
    key,
    value
) VALUES (
    ${bind { (fieldId, _) -> fieldId}},
    ${bind { (_, option) -> option.weight }},
    ${bind { (_, option) -> option.key }},
    ${bind { (_, option) -> option.value }}
)
ON CONFLICT (field_id, value) DO
UPDATE SET
weight = excluded.weight,
key = excluded.key
WHERE
nekku_special_diet_option.weight <> excluded.weight OR
nekku_special_diet_option.key <> excluded.key;
 """
        )
    }

    logger.info {
        "Deleted: ${deletedSpecialOptionsCount.size} Nekku special diet options, inserted ${specialDietOptions.size}"
    }
}

/** Throws an IllegalStateException if Nekku returns an empty product list. */
fun fetchAndUpdateNekkuProducts(client: NekkuClient, db: Database.Connection) {
    val productsFromNekku = client.getProducts()

    if (productsFromNekku.isEmpty())
        error("Refusing to sync empty Nekku product list into database")
    db.transaction { tx ->
        val deletedProductCount = tx.setProductNumbers(productsFromNekku)
        logger.info {
            "Deleted: $deletedProductCount Nekku customer numbers, inserted ${productsFromNekku.size}"
        }
    }
}

fun Database.Transaction.setProductNumbers(productNumbers: List<NekkuProduct>): Int {
    val newProductNumbers = productNumbers.map { it.sku }
    val deletedProductCount = execute {
        sql("DELETE FROM nekku_product WHERE sku != ALL (${bind(newProductNumbers)})")
    }
    executeBatch(productNumbers) {
        sql(
            """
INSERT INTO nekku_product (sku, name, options_id, unit_size, meal_time, meal_type)
VALUES (
    ${bind{it.sku}},
    ${bind{it.name}},
    ${bind{it.options_id}},
    ${bind{it.unit_size}},
    ${bind{it.meal_time}},
    ${bind{it.meal_type}}
)
ON CONFLICT (sku) DO 
UPDATE SET
  name = excluded.name,
  options_id = excluded.options_id,
  unit_size = excluded.unit_size,
  meal_time = excluded.meal_time,
  meal_type = excluded.meal_type
WHERE
    nekku_product.name <> excluded.name OR
    nekku_product.options_id <> excluded.options_id OR
    nekku_product.unit_size <> excluded.unit_size OR 
    nekku_product.meal_time <> excluded.meal_time OR 
    nekku_product.meal_type <> excluded.meal_type;
"""
        )
    }
    return deletedProductCount
}

fun Database.Transaction.getNekkuProducts(): List<NekkuProduct> {
    return createQuery {
            sql("SELECT sku, name, options_id, unit_size, meal_time, meal_type FROM nekku_product")
        }
        .toList<NekkuProduct>()
}
