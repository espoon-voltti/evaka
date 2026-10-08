// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.daycare.CareType
import evaka.core.daycare.domain.ProviderType
import evaka.core.placement.PlacementType
import evaka.core.shared.AreaId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.EmployeeId
import evaka.core.shared.GroupId
import evaka.core.shared.PersonId
import evaka.core.shared.PlacementId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate

object McpToolsSearch {
    data class SearchUnitsInput(
        @McpDoc("Case-insensitive substring of the unit name. Omit to list all units.")
        val query: String? = null,
        @McpDoc("Include units that have already closed") val includeClosed: Boolean = false,
        @McpDoc("Maximum number of results (default 50)") val limit: Int = 50,
    )

    data class SearchPersonsInput(
        @McpDoc(
            "Case-insensitive substring of the person's full name (first names and last name in either order), or an exact social security number"
        )
        val query: String,
        @McpDoc("Maximum number of results (default 20)") val limit: Int = 20,
    )

    data class SearchEmployeesInput(
        @McpDoc(
            "Case-insensitive substring of the employee's name or email. Omit to list the most recent employees."
        )
        val query: String? = null,
        @McpDoc("Only employees with a role in this unit") val unitId: DaycareId? = null,
        val includeInactive: Boolean = false,
        val limit: Int = 50,
    )

    fun tools(): List<McpToolDefinition<*>> =
        listOf(
            mcpTool<SearchUnitsInput>(
                name = "search_units",
                description =
                    "Lists daycare units (päiväkodit) with their ids, care area, types, provider type, capacity and groups. Use to find existing units to reference in placements and applications.",
                readOnly = true,
            ) { ctx, input ->
                searchUnits(ctx, input)
            },
            mcpTool<SearchPersonsInput>(
                name = "search_persons",
                description =
                    "Finds existing persons (citizens, children and adults) by name or SSN. For each match returns the guardians, the children (via guardianship, head of family or foster parent relations) and the current and future placements with unit and group, so that data can be created for existing test persons, e.g. calendar events for the children of a guardian.",
                readOnly = true,
            ) { ctx, input ->
                searchPersons(ctx, input)
            },
            mcpTool<SearchEmployeesInput>(
                name = "search_employees",
                description =
                    "Lists employees with their ids, global roles and unit/group roles. Use to find existing staff (including real users in staging) to link to test units.",
                readOnly = true,
            ) { ctx, input ->
                searchEmployees(ctx, input)
            },
        )

    private data class UnitInfo(
        val id: DaycareId,
        val name: String,
        val careAreaId: AreaId,
        val careAreaName: String,
        val types: List<CareType>,
        val providerType: ProviderType,
        val capacity: Int,
        val openingDate: LocalDate?,
        val closingDate: LocalDate?,
        val groups: List<String>,
        val mcpTestData: Boolean,
    )

    private fun searchUnits(ctx: McpToolContext, input: SearchUnitsInput): Any {
        val limit = input.limit.coerceIn(1, 500)
        val query = input.query?.trim()?.takeIf { it.isNotEmpty() }
        val today = ctx.today
        val units =
            ctx.tx
                .createQuery {
                    sql(
                        """
SELECT
    d.id, d.name, d.care_area_id, ca.name AS care_area_name, d.type AS types, d.provider_type, d.capacity,
    d.opening_date, d.closing_date,
    coalesce((SELECT array_agg(g.name ORDER BY g.name) FROM daycare_group g WHERE g.daycare_id = d.id), '{}') AS groups,
    EXISTS (SELECT FROM mcp_test_data_entity e WHERE e.table_name = 'daycare' AND e.entity_id = d.id) AS mcp_test_data
FROM daycare d
JOIN care_area ca ON ca.id = d.care_area_id
WHERE (${bind(query)}::text IS NULL OR d.name ILIKE '%' || ${bind(query)} || '%')
  AND (${bind(input.includeClosed)} OR d.closing_date IS NULL OR d.closing_date >= ${bind(today)})
ORDER BY d.name
LIMIT ${bind(limit)}
"""
                    )
                }
                .toList<UnitInfo>()
        return mapOf("count" to units.size, "units" to units)
    }

    private data class PersonSearchRow(
        val id: PersonId,
        val firstName: String,
        val lastName: String,
        val dateOfBirth: LocalDate,
        val ssn: String?,
        val streetAddress: String,
        val postalCode: String,
        val postOffice: String,
        val mcpTestData: Boolean,
    )

    private enum class PersonRelation {
        GUARDIAN,
        HEAD_OF_FAMILY,
        FOSTER_PARENT,
    }

    private data class PersonRelationRow(
        val parentId: PersonId,
        val parentName: String,
        val childId: ChildId,
        val childName: String,
        val childDateOfBirth: LocalDate,
        val relation: PersonRelation,
    )

    private data class PlacementSearchRow(
        val id: PlacementId,
        val childId: ChildId,
        val type: PlacementType,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val unitId: DaycareId,
        val unitName: String,
    )

    private data class GroupPlacementSearchRow(
        val placementId: PlacementId,
        val groupId: GroupId,
        val groupName: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
    )

    private fun searchPersons(ctx: McpToolContext, input: SearchPersonsInput): Any {
        val query = input.query.trim()
        if (query.isEmpty()) throw BadRequest("query must not be empty")
        val limit = input.limit.coerceIn(1, 100)
        val today = ctx.today
        val persons =
            ctx.tx
                .createQuery {
                    sql(
                        """
SELECT
    p.id, p.first_name, p.last_name, p.date_of_birth, p.social_security_number AS ssn,
    p.street_address, p.postal_code, p.post_office,
    EXISTS (SELECT FROM mcp_test_data_entity e WHERE e.table_name = 'person' AND e.entity_id = p.id) AS mcp_test_data
FROM person p
WHERE p.first_name || ' ' || p.last_name ILIKE '%' || ${bind(query)} || '%'
   OR p.last_name || ' ' || p.first_name ILIKE '%' || ${bind(query)} || '%'
   OR p.social_security_number = upper(${bind(query)})
ORDER BY p.last_name, p.first_name, p.date_of_birth
LIMIT ${bind(limit)}
"""
                    )
                }
                .toList<PersonSearchRow>()
        val personIds = persons.map { it.id }
        ctx.audit.add(personIds)
        val relations =
            ctx.tx
                .createQuery {
                    sql(
                        """
SELECT
    r.parent_id, parent.first_name || ' ' || parent.last_name AS parent_name,
    r.child_id, child.first_name || ' ' || child.last_name AS child_name, child.date_of_birth AS child_date_of_birth,
    r.relation
FROM (
    SELECT guardian_id AS parent_id, child_id, 'GUARDIAN' AS relation FROM guardian
    UNION ALL
    SELECT head_of_child, child_id, 'HEAD_OF_FAMILY' FROM fridge_child
    WHERE NOT conflict AND daterange(start_date, end_date, '[]') @> ${bind(today)}
    UNION ALL
    SELECT parent_id, child_id, 'FOSTER_PARENT' FROM foster_parent WHERE valid_during @> ${bind(today)}
) r
JOIN person parent ON parent.id = r.parent_id
JOIN person child ON child.id = r.child_id
WHERE r.parent_id = ANY(${bind(personIds)}) OR r.child_id = ANY(${bind(personIds)})
ORDER BY child.date_of_birth, r.relation
"""
                    )
                }
                .toList<PersonRelationRow>()
        val placementChildIds = (personIds + relations.map { it.childId }).distinct()
        val placements =
            ctx.tx
                .createQuery {
                    sql(
                        """
SELECT pl.id, pl.child_id, pl.type, pl.start_date, pl.end_date, d.id AS unit_id, d.name AS unit_name
FROM placement pl
JOIN daycare d ON d.id = pl.unit_id
WHERE pl.child_id = ANY(${bind(placementChildIds)}) AND pl.end_date >= ${bind(today)}
ORDER BY pl.start_date
"""
                    )
                }
                .toList<PlacementSearchRow>()
        val groupPlacements =
            ctx.tx
                .createQuery {
                    sql(
                        """
SELECT gp.daycare_placement_id AS placement_id, g.id AS group_id, g.name AS group_name, gp.start_date, gp.end_date
FROM daycare_group_placement gp
JOIN daycare_group g ON g.id = gp.daycare_group_id
WHERE gp.daycare_placement_id = ANY(${bind(placements.map { it.id })}) AND gp.end_date >= ${bind(today)}
ORDER BY gp.start_date
"""
                    )
                }
                .toList<GroupPlacementSearchRow>()
                .groupBy { it.placementId }

        fun placementsOf(childId: ChildId) =
            placements
                .filter { it.childId == childId }
                .map { pl ->
                    mapOf(
                        "placementId" to pl.id,
                        "type" to pl.type,
                        "startDate" to pl.startDate,
                        "endDate" to pl.endDate,
                        "unitId" to pl.unitId,
                        "unitName" to pl.unitName,
                        "groups" to
                            groupPlacements[pl.id].orEmpty().map {
                                mapOf(
                                    "groupId" to it.groupId,
                                    "groupName" to it.groupName,
                                    "startDate" to it.startDate,
                                    "endDate" to it.endDate,
                                )
                            },
                    )
                }

        return mapOf(
            "count" to persons.size,
            "persons" to
                persons.map { p ->
                    mapOf(
                        "id" to p.id,
                        "firstName" to p.firstName,
                        "lastName" to p.lastName,
                        "dateOfBirth" to p.dateOfBirth,
                        "ssn" to p.ssn,
                        "address" to "${p.streetAddress}, ${p.postalCode} ${p.postOffice}",
                        "mcpTestData" to p.mcpTestData,
                        "guardians" to
                            relations
                                .filter { it.childId == p.id }
                                .groupBy { it.parentId }
                                .map { (parentId, rows) ->
                                    mapOf(
                                        "id" to parentId,
                                        "name" to rows.first().parentName,
                                        "relations" to rows.map { it.relation },
                                    )
                                },
                        "children" to
                            relations
                                .filter { it.parentId == p.id }
                                .groupBy { it.childId }
                                .map { (childId, rows) ->
                                    mapOf(
                                        "id" to childId,
                                        "name" to rows.first().childName,
                                        "dateOfBirth" to rows.first().childDateOfBirth,
                                        "relations" to rows.map { it.relation },
                                        "placements" to placementsOf(childId),
                                    )
                                },
                        "placements" to placementsOf(p.id),
                    )
                },
        )
    }

    private data class EmployeeSearchRow(
        val id: EmployeeId,
        val firstName: String,
        val lastName: String,
        val email: String?,
        val active: Boolean,
        val lastLogin: HelsinkiDateTime?,
        val globalRoles: List<UserRole>,
        val unitRoles: List<String>,
        val groups: List<String>,
        val mcpTestData: Boolean,
    )

    private fun searchEmployees(ctx: McpToolContext, input: SearchEmployeesInput): Any {
        val query = input.query?.trim()?.takeIf { it.isNotEmpty() }
        val employees =
            ctx.tx
                .createQuery {
                    sql(
                        """
SELECT
    e.id, e.first_name, e.last_name, e.email, e.active, e.last_login, e.roles AS global_roles,
    coalesce((SELECT array_agg(d.name || ': ' || acl.role ORDER BY d.name) FROM daycare_acl acl JOIN daycare d ON d.id = acl.daycare_id WHERE acl.employee_id = e.id), '{}') AS unit_roles,
    coalesce((SELECT array_agg(d.name || ' / ' || g.name ORDER BY d.name, g.name) FROM daycare_group_acl gacl JOIN daycare_group g ON g.id = gacl.daycare_group_id JOIN daycare d ON d.id = g.daycare_id WHERE gacl.employee_id = e.id), '{}') AS groups,
    EXISTS (SELECT FROM mcp_test_data_entity t WHERE t.table_name = 'employee' AND t.entity_id = e.id) AS mcp_test_data
FROM employee e
WHERE (${bind(query)}::text IS NULL OR e.first_name || ' ' || e.last_name ILIKE '%' || ${bind(query)} || '%' OR e.email ILIKE '%' || ${bind(query)} || '%')
  AND (${bind(input.unitId)}::uuid IS NULL OR EXISTS (SELECT FROM daycare_acl acl WHERE acl.employee_id = e.id AND acl.daycare_id = ${bind(input.unitId)}))
  AND (${bind(input.includeInactive)} OR e.active)
ORDER BY e.last_login DESC NULLS LAST, e.last_name, e.first_name
LIMIT ${bind(input.limit.coerceIn(1, 200))}
"""
                    )
                }
                .toList<EmployeeSearchRow>()
        return mapOf("count" to employees.size, "employees" to employees)
    }
}
