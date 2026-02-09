<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Database API

## Purpose

Quick reference for eVaka's database API - executing raw SQL queries with safe parameter binding, managing transactions, and building predicates.

**Implementation**: `service/src/main/kotlin/fi/espoo/evaka/shared/db/Database.kt` and `Predicate.kt`

## Transaction Types

Your function signature determines what you can do:

```kotlin
fun getConnection(db: Database) { }           // Can open database connections
fun maybeTransaction(db: Database.Connection) { }  // Can start transactions
fun readData(tx: Database.Read) { }           // Can only read
fun writeData(tx: Database.Transaction) { }   // Can read and write
```

### Typical controller pattern

Controllers receive a `Database` object, open a connection, start a transaction, and pass it down:

```kotlin
@PostMapping("/users")
fun createUser(
    db: Database,
    @RequestBody request: CreateUserRequest
): UserId {
    return db.connect { conn ->
        conn.transaction { tx ->
            createUserWithMessagingAccount(tx, request)
        }
    }
}
```

## Basic Query Execution

### Simple queries as extension functions
```kotlin
fun Database.Read.getUser(userId: UUID): User =
    createQuery {
        sql("SELECT * FROM users WHERE id = ${bind(userId)}")
    }.exactlyOne()
```

### Functions combining queries and logic take a Transaction parameter
```kotlin
fun enrollUserInGroup(tx: Database.Transaction, userId: UUID, groupId: UUID) {
    val capacity = tx.getGroupCapacity(groupId)
    val currentCount = tx.getUserCountInGroup(groupId)

    require(currentCount < capacity) { "Group is full" }

    tx.execute {
        sql("""
            INSERT INTO group_members (user_id, group_id, enrolled_at)
            VALUES (${bind(userId)}, ${bind(groupId)}, NOW())
        """)
    }
}
```

### Query with manual mapping (avoid for simple cases)
```kotlin
fun Database.Read.getUsers(): List<User> =
    createQuery {
        sql("SELECT name, age FROM users")
    }.map { row ->
        User(
            name = row.column("name"),
            age = row.column("age")
        )
    }.toList()
```

### Query returning optional result
```kotlin
fun Database.Read.findUser(userId: UUID): User? =
    createQuery {
        sql("SELECT * FROM users WHERE id = ${bind(userId)}")
    }.exactlyOneOrNull<User>()
```

## Inserts and Updates

### Simple insert
```kotlin
tx.execute {
    sql("""
        INSERT INTO users (id, name, email)
        VALUES (${bind(id)}, ${bind(name)}, ${bind(email)})
    """)
}
```

### Update with row count check
```kotlin
tx.createUpdate {
    sql("UPDATE users SET name = ${bind(name)} WHERE id = ${bind(id)}")
}.updateExactlyOne()  // Throws if 0 or >1 rows affected
```

### Insert returning generated ID
```kotlin
val id = tx.createUpdate {
    sql("INSERT INTO users (name) VALUES (${bind(name)})")
}.executeAndReturnGeneratedKeys()
  .exactlyOne<UUID>()
```

### Insert with RETURNING clause
```kotlin
// Use createQuery (not createUpdate) to map RETURNING results
val user = tx.createQuery {
    sql("""
        INSERT INTO users (name, email, created_at)
        VALUES (${bind(name)}, ${bind(email)}, NOW())
        RETURNING id, name, email, created_at
    """)
}.exactlyOne<User>()
```

### JSON column binding
```kotlin
tx.execute {
    sql("UPDATE settings SET data = ${bindJson(settingsData)} WHERE id = ${bind(id)}")
}
```

## Batch Operations

Use for inserting/updating multiple rows unless complexity outweighs performance benefit:

```kotlin
tx.executeBatch(users) {
    sql("""
        INSERT INTO users (id, name, email)
        VALUES (${bind { it.id }}, ${bind { it.name }}, ${bind { it.email }})
    """)
}
```

## Common Pitfalls

❌ **Don't concatenate values into SQL**
```kotlin
sql("SELECT * FROM users WHERE name = '$name'")  // SQL injection!
```

✅ **Always use parameter binding**
```kotlin
sql("SELECT * FROM users WHERE name = ${bind(name)}")
```

❌ **Using Transaction when Read would suffice**
```kotlin
fun getUsers(tx: Database.Transaction): List<User> { }  // Unnecessarily requires write access
```

✅ **Use Read for read-only operations**
```kotlin
fun getUsers(tx: Database.Read): List<User> { }
```

## Result Mapping Annotations

JDBI and Jackson annotations for mapping SQL results to Kotlin data classes.

### @Json - JSON Column Mapping

Use when mapping **JSONB database columns** or **PostgreSQL JSON aggregation results** to Kotlin types.

**When to use:**
- Database column is type JSONB
- Query returns JSON through e.g. `jsonb_agg()` and `jsonb_build_object()`

**Example: JSON aggregation with subquery**
```kotlin
data class PedagogicalDocumentCitizen(
    val id: PedagogicalDocumentId,
    @Json val attachments: List<Attachment> = emptyList()
)

fun Database.Read.getChildPedagogicalDocuments(
    childId: ChildId
): List<PedagogicalDocumentCitizen> {
    return createQuery {
        sql("""
            SELECT
                pd.id,
                (
                    SELECT coalesce(jsonb_agg(jsonb_build_object(
                        'id', a.id,
                        'name', a.name,
                        'contentType', a.content_type
                    )), '[]'::jsonb)
                    FROM attachment a
                    WHERE a.pedagogical_document_id = pd.id
                ) AS attachments
            FROM pedagogical_document pd
            WHERE pd.child_id = ${bind(childId)}
        """)
    }.toList<PedagogicalDocumentCitizen>()
}
```

The `@Json` annotation tells JDBI to deserialize the JSONB array into `List<Attachment>`.

**Example: Direct JSONB column**
```sql
CREATE TABLE calendar_event (
    id UUID PRIMARY KEY,
    groups JSONB NOT NULL,
    times JSONB NOT NULL
);
```

```kotlin
data class CalendarEvent(
    val id: CalendarEventId,
    @Json val groups: Set<GroupInfo>,
    @Json val times: Set<CalendarEventTime>
)

fun Database.Read.getCalendarEvent(id: CalendarEventId): CalendarEvent =
    createQuery {
        sql("SELECT id, groups, times FROM calendar_event WHERE id = ${bind(id)}")
    }.exactlyOne<CalendarEvent>()
```

### @Nested and @PropagateNull - Nested Object Mapping

Maps columns with a common prefix to a nested object. Use `@PropagateNull` on a field to make the entire nested object null when that field is null.

```kotlin
data class Placement(
    val id: PlacementId,
    val startDate: LocalDate,
    @Nested("modified_by") val modifiedBy: EvakaUser?
)

data class EvakaUser(
    @PropagateNull val id: EvakaUserId,
    val name: String,
    val type: EvakaUserType
)

tx.createQuery {
    sql("""
        SELECT
            p.id,
            p.start_date,
            u.id AS modified_by_id,
            u.name AS modified_by_name,
            u.type AS modified_by_type
        FROM placement p
        LEFT JOIN evaka_user u ON p.modified_by = u.id
    """)
}.toList<Placement>()
```

**How it works:**
- `@Nested("modified_by")` tells JDBI to map all `modified_by_*` columns to the nested `EvakaUser` object
- `@PropagateNull` on `id` makes the entire `modifiedBy` object `null` when `modified_by_id` is NULL (e.g., from LEFT JOIN where no user exists)
- Without `@PropagateNull`, you'd get an `EvakaUser` with nullable fields instead of a nullable `EvakaUser`

### @JsonTypeInfo / @JsonTypeName - Polymorphic JSON

For deserializing polymorphic types (sealed classes/interfaces) stored as JSON:

```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed interface Gross {
    @JsonTypeName("INCOME")
    data class Income(
        val incomeSource: IncomeSource,
        val estimatedMonthlyIncome: Int
    ) : Gross

    @JsonTypeName("NO_INCOME")
    data class NoIncome(val noIncomeDescription: String) : Gross
}
```

**JSON structure** with discriminator field:
```json
{
  "type": "INCOME",
  "incomeSource": "SALARY",
  "estimatedMonthlyIncome": 5000
}
```

The `type` field determines which subtype to deserialize. Used for JSONB columns containing variant data.

## SQL Predicates

For building complex, reusable WHERE clauses. See [Predicates Guide](predicates.md) for full documentation.

**When to use**: Complex boolean logic composition, conditional filters, or reusable query conditions.

**Basic example:**
```kotlin
val predicate = Predicate { where("$it.status = ${bind(status)}") }

tx.createQuery {
    sql("""
        SELECT * FROM users u
        WHERE ${predicate(predicate.forTable("u"))}
    """)
}
```

## See Also

- [SQL Predicates Guide](predicates.md)
- [Database Schema & Migrations](migrations.md)
- [Service Conventions](conventions.md)
- [Testing Conventions](testing.md)
