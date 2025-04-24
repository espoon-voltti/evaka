// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.decision.logger
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import org.jdbi.v3.json.Json

/** Throws an IllegalStateException if Nekku returns an empty customer list. */
fun fetchAndUpdateNekkuCustomers(client: NekkuClient, db: Database.Connection) {
    val customersFromNekku =
        client.getCustomers().map { it.toEvaka() }.filter { it.group.contains("Varhaiskasvatus") }

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

        tx.setNekkuCustomerTypes(customersFromNekku.map { it.number to it.customerType })

        logger.info {
            "Deleted: $deletedCustomerCount Nekku customer numbers, inserted ${customersFromNekku.size}"
        }
    }
}

fun Database.Transaction.resetNekkuCustomerNumbersNotContainedWithin(
    nekkuCustomerNumbers: List<NekkuCustomer>
): Int {
    val newNekkuCustomerNumbers = nekkuCustomerNumbers.map { it.number }
    val affectedRows = execute {
        sql(
            "UPDATE daycare_group SET nekku_customer_number = null WHERE nekku_customer_number != ALL (${bind(newNekkuCustomerNumbers)})"
        )
    }
    return affectedRows
}

fun Database.Transaction.setCustomerNumbers(customerNumbers: List<NekkuCustomer>): Int {
    val newCustomerNumbers = customerNumbers.map { it.number }
    val deletedCustomerCount = execute {
        sql("DELETE FROM nekku_customer WHERE number != ALL (${bind(newCustomerNumbers)})")
    }
    executeBatch(customerNumbers) {
        sql(
            """
INSERT INTO nekku_customer (number, name, customer_group)
VALUES (
    ${bind{it.number}},
    ${bind{it.name}},
    ${bind{it.group}}
)
ON CONFLICT (number) DO 
UPDATE SET
  name = excluded.name,
  customer_group = excluded.customer_group
WHERE
    nekku_customer.name <> excluded.name OR
    nekku_customer.customer_group <> excluded.customer_group;
"""
        )
    }
    return deletedCustomerCount
}

fun Database.Transaction.setNekkuCustomerTypes(
    nekkuCustomerTypes: List<Pair<String, List<CustomerType>>>
) {
    val newNekkuCustomerNumbers = nekkuCustomerTypes.flatMap { it.second }.map { it.type }
    val deletedNekkuCustomerTypesCount = execute {
        sql(
            "DELETE FROM nekku_customer_type WHERE customer_number != ALL (${bind(newNekkuCustomerNumbers)})"
        )
    }

    val batchRows: Sequence<Pair<String, CustomerType>> =
        nekkuCustomerTypes.asSequence().flatMap { (customerNumber, customerTypes) ->
            customerTypes.map { customerType -> Pair(customerNumber, customerType) }
        }

    executeBatch(batchRows) {
        sql(
            """
INSERT INTO nekku_customer_type (
    customer_number,
    type,
    weekdays
) VALUES (
    ${bind { (customerNumber, _) -> customerNumber}},
    ${bind { (_, field) -> field.type }},
    ${bind { (_, field) -> field.weekdays }}
)
ON CONFLICT (customer_number) DO 
UPDATE SET
  type = excluded.type,
  weekdays = excluded.weekdays
WHERE
    nekku_customer_type.type <> excluded.type OR 
    nekku_customer_type.weekdays <> excluded.weekdays;

"""
        )
    }

    logger.info {
        "Deleted: $deletedNekkuCustomerTypesCount Nekku customer types, inserted ${nekkuCustomerTypes.size}"
    }
}

fun Database.Read.getNekkuDaycareCustomerMapping(
    groupId: GroupId,
    weekday: NekkuCustomerWeekday,
): NekkuDaycareCustomerMapping? =
    createQuery {
            sql(
                """
                SELECT 
                    dg.nekku_customer_number as customerNumber, 
                    dg.name as groupName, 
                    nct.type as customerType
                FROM daycare_group dg 
                    JOIN daycare d ON d.id = dg.daycare_id
                    JOIN nekku_customer nc ON nc.number = dg.nekku_customer_number
                    LEFT JOIN nekku_customer_type nct ON nc.number = nct.customer_number
                WHERE dg.id = ${bind(groupId)}
                AND ${bind(weekday)} = ANY(nct.weekdays)
            """
            )
        }
        .exactlyOneOrNull<NekkuDaycareCustomerMapping>()

fun Database.Transaction.getNekkuCustomers(): List<NekkuCustomer> {
    return createQuery {
            sql(
                """
    SELECT 
        nc.number, 
        nc.name, 
        nc.customer_group AS "group",
        JSON_AGG(JSON_BUILD_OBJECT('weekdays', nct.weekdays, 'type', nct.type)) AS customerType
    FROM nekku_customer nc
    LEFT JOIN nekku_customer_type nct ON nc.number = nct.customer_number
    GROUP BY nc.number, nc.name, nc.customer_group
    """
            )
        }
        .toList<NekkuCustomer>()
}

fun Database.Read.getNekkuUnitNumbers(): List<NekkuUnitNumber> {
    return createQuery { sql("SELECT number, name FROM nekku_customer ORDER BY name ASC") }
        .toList<NekkuUnitNumber>()
}

/** Throws an IllegalStateException if Nekku returns an empty special diet list. */
fun fetchAndUpdateNekkuSpecialDiets(client: NekkuClient, db: Database.Connection) {

    val specialDietsFromNekku = client.getSpecialDiets().map { it.toEvaka() }

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
    val productsFromNekku = client.getProducts().map { it.toEvaka() }

    if (productsFromNekku.isEmpty())
        error("Refusing to sync empty Nekku product list into database")
    db.transaction { tx ->
        val deletedProductCount = tx.setProductNumbers(productsFromNekku)
        logger.info {
            "Deleted: $deletedProductCount Nekku product numbers, inserted ${productsFromNekku.size}"
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
INSERT INTO nekku_product (sku, name, options_id, customer_types, meal_time, meal_type)
VALUES (
    ${bind{it.sku}},
    ${bind{it.name}},
    ${bind{it.optionsId}},
    ${bind{it.customerTypes}},
    ${bind{it.mealTime}},
    ${bind{it.mealType}}
)
ON CONFLICT (sku) DO 
UPDATE SET
  name = excluded.name,
  options_id = excluded.options_id,
  customer_types = excluded.customer_types,
  meal_time = excluded.meal_time,
  meal_type = excluded.meal_type
WHERE
    nekku_product.name <> excluded.name OR
    nekku_product.options_id <> excluded.options_id OR
    nekku_product.customer_types <> excluded.customer_types OR 
    nekku_product.meal_time <> excluded.meal_time OR 
    nekku_product.meal_type <> excluded.meal_type;
"""
        )
    }
    return deletedProductCount
}

fun Database.Read.getNekkuProducts(): List<NekkuProduct> {
    return createQuery {
            sql(
                "SELECT sku, name, options_id, customer_types, meal_time, meal_type FROM nekku_product"
            )
        }
        .toList<NekkuProduct>()
}

fun Database.Read.getNekkuChildData(nekkuGroupId: GroupId, date: LocalDate): List<NekkuChildData> =
    createQuery {
            sql(
                """
SELECT
    rp.child_id,
    rp.unit_id,
    rp.group_id,
    rp.placement_type,
    ch.nekku_eats_breakfast as eats_breakfast,
    (sn.shift_care IS NOT NULL AND sn.shift_care != 'NONE') AS has_shift_care,
    coalesce((
        SELECT jsonb_agg(jsonb_build_object('start', ar.start_time, 'end', ar.end_time))
        FROM attendance_reservation ar
        JOIN evaka_user eu ON ar.created_by = eu.id
        WHERE
            ar.child_id = rp.child_id AND
            ar.date = ${bind(date)} AND
            -- Ignore NO_TIMES reservations
            ar.start_time IS NOT NULL AND ar.end_time IS NOT NULL
    ), '[]'::jsonb) AS reservations,
    coalesce((
        SELECT array_agg(a.category)
        FROM absence a
        WHERE a.child_id = rp.child_id AND a.date = ${bind(date)}
    ), '{}'::absence_category[]) AS absences
FROM realized_placement_one(${bind(date)}) rp
JOIN daycare_group dg ON dg.id = rp.group_id
JOIN child ch ON ch.id = rp.child_id
LEFT JOIN service_need sn ON sn.placement_id = rp.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(date)}
WHERE dg.id = ${bind(nekkuGroupId)}
                    """
            )
        }
        .toList()

data class NekkuChildData(
    val childId: ChildId,
    val unitId: DaycareId,
    val groupId: GroupId,
    val placementType: PlacementType,
    val hasShiftCare: Boolean,
    val eatsBreakfast: Boolean,
    @Json val reservations: List<TimeRange>,
    val absences: Set<AbsenceCategory>,
)

data class NekkuDaycareCustomerMapping(
    val customerNumber: String,
    val groupName: String,
    val customerType: String,
)

fun Database.Read.getNekkuDaycareGroupId(range: FiniteDateRange): List<GroupId> =
    createQuery {
            sql(
                """
                    SELECT dg.id as groupId
                    FROM daycare_group dg 
                    JOIN daycare d ON d.id = dg.daycare_id
                    JOIN nekku_customer nc ON nc.number = dg.nekku_customer_number
                    WHERE dg.nekku_customer_number IS NOT NULL
                      AND daterange(d.opening_date, d.closing_date, '[]') && ${bind(range)}
                      AND daterange(dg.start_date, dg.end_date, '[]') && ${bind(range)}
                """
            )
        }
        .toList<GroupId>()

fun Database.Read.mealTypesForChildren(
    childIds: Set<ChildId>
): Map<ChildId, NekkuProductMealType?> =
    createQuery {
            sql(
                """
SELECT child.id as child_id, child.nekku_diet
FROM child
WHERE child.id = ANY (${bind(childIds)})
"""
            )
        }
        .toMap { column<ChildId>("child_id") to column<NekkuProductMealType?>("nekku_diet") }

fun Database.Read.getNekkuSpecialDiets(): List<NekkuSpecialDietWithoutFields> =
    createQuery { sql("SELECT id, name FROM nekku_special_diet") }
        .toList<NekkuSpecialDietWithoutFields>()

fun Database.Read.getNekkuSpecialDietFields(): List<NekkuSpecialDietsFieldWithoutOptions> =
    createQuery { sql("SELECT id, name, type, diet_id FROM nekku_special_diet_field") }
        .toList<NekkuSpecialDietsFieldWithoutOptions>()

fun Database.Read.getNekkuSpecialDietOptions(): List<NekkuSpecialDietOptionWithFieldId> =
    createQuery { sql("SELECT weight, key, value, field_id FROM nekku_special_diet_option") }
        .toList<NekkuSpecialDietOptionWithFieldId>()
