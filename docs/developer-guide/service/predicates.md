<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# SQL Predicates

A comprehensive guide to building dynamic, reusable WHERE clauses using the Predicate API.

## Purpose

The Predicate API provides a type-safe way to build complex SQL WHERE clauses that can be:
- **Reused** across multiple queries
- **Combined** using boolean logic (AND/OR)
- **Conditionally included** based on runtime values
- **Passed as parameters** to make functions more flexible

## Core Concepts

### Predicate vs PredicateSql

The API has two main types:

- **`Predicate`** - An unbound predicate template that can be applied to any table alias
- **`PredicateSql`** - A bound predicate ready to be embedded in SQL

You create a `Predicate` once, then call `.forTable("alias")` to get a `PredicateSql` bound to a specific table.

### The forTable() Pattern

To use a `Predicate` in SQL, you must bind it to a table alias using `.forTable()`:

```kotlin
val predicate = Predicate { where("$it.status = 'ACTIVE'") }
val boundPredicate: PredicateSql = predicate.forTable("u")
```

**Why this pattern?**
- Same predicate can be used with different table aliases
- Enables multi-table queries with predicates on different tables
- The `$it` placeholder is replaced with the table alias when you call `.forTable()`

## Basic Usage

**Note:** The examples use context-dependent functions:
- `where()` - Available inside `Predicate { ... }` lambda to build the predicate SQL
- `predicate()` - Available inside `createQuery { sql(...) }` block to embed a bound predicate

### Creating Predicates

```kotlin
// Simple predicate
val activeFilter = Predicate { where("$it.active = TRUE") }

// With parameter binding
val statusFilter = Predicate { where("$it.status = ${bind(status)}") }

// Use in query
tx.createQuery {
    sql("""
        SELECT * FROM users u
        WHERE ${predicate(statusFilter.forTable("u"))}
    """)
}.toList<User>()
```

### Combining Predicates (Same Table)

Use `Predicate.all()` for AND logic:

```kotlin
val activeFilter = Predicate { where("$it.active = TRUE") }
val statusFilter = Predicate { where("$it.status = ${bind(status)}") }

val combinedFilter = Predicate.all(activeFilter, statusFilter)

tx.createQuery {
    sql("""
        SELECT * FROM users u
        WHERE ${predicate(combinedFilter.forTable("u"))}
    """)
}.toList<User>()
```

Use `Predicate.any()` for OR logic:

```kotlin
val filters = Predicate.any(
    Predicate { where("$it.status = 'ACTIVE'") },
    Predicate { where("$it.status = 'PENDING'") }
)
```

Use `Predicate.allNotNull()` to include only non-null predicates:

```kotlin
val filters = Predicate.allNotNull(
    activeFilter,
    if (searchText != null) searchFilter else null,
    if (dateRange != null) dateFilter else null
)

tx.createQuery {
    sql("""
        SELECT * FROM users u
        WHERE ${predicate(filters.forTable("u"))}
    """)
}.toList<User>()
```

## Advanced Patterns

### Multi-Table Predicates

When working with JOINs, bind each predicate to its respective table alias and combine the `PredicateSql` instances:

```kotlin
val userFilter = Predicate { where("$it.active = TRUE") }
val orderFilter = Predicate { where("$it.status = 'COMPLETED'") }

tx.createQuery {
    sql("""
        SELECT u.*, o.*
        FROM users u
        JOIN orders o ON u.id = o.user_id
        WHERE ${predicate(userFilter.forTable("u").and(orderFilter.forTable("o")))}
    """)
}.toList<UserWithOrders>()
```

**Key insight:**
- `.forTable()` is called on each `Predicate` separately
- `.and()` combines the resulting `PredicateSql` instances
- Each predicate uses its own table alias

### Predicates as Function Parameters

Make query functions flexible by accepting optional predicates:

```kotlin
private fun Database.Read.getUsers(
    additionalFilter: Predicate = Predicate.alwaysTrue()
): List<User> {
    // Always exclude deleted users, then apply additional filter
    val baseFilter = Predicate { where("$it.deleted_at IS NULL") }
    val combined = Predicate.all(baseFilter, additionalFilter)

    return createQuery {
        sql("""
            SELECT * FROM users u
            WHERE ${predicate(combined.forTable("u"))}
        """)
    }.toList()
}

// Usage
val allUsers = tx.getUsers()
val activeUsers = tx.getUsers(Predicate { where("$it.active = TRUE") })
```

### Using Subqueries in Predicates

Use `subquery()` when you need to embed a pre-built `QuerySql` object (e.g., from another function).

```kotlin
private fun activeUsersQuery(minAge: Int) = QuerySql {
    sql("SELECT id FROM users WHERE active = TRUE AND age >= ${bind(minAge)}")
}

val filter = Predicate {
    where("$it.id IN (${subquery(activeUsersQuery(18))})")
}
```

## Quick Reference

| Concept | Usage | Example |
|---------|-------|---------|
| `Predicate { where(...) }` | Create reusable condition | `Predicate { where("$it.active = TRUE") }` |
| `.forTable("alias")` | Bind to table alias | `predicate.forTable("u")` |
| `${predicate(...)}` | Embed in SQL | `WHERE ${predicate(p.forTable("u"))}` |
| `Predicate.all()` | AND conditions | `Predicate.all(filter1, filter2)` |
| `Predicate.allNotNull()` | AND with nulls | `Predicate.allNotNull(f1, nullableF2)` |
| `Predicate.any()` | OR conditions | `Predicate.any(filter1, filter2)` |
| `Predicate.alwaysTrue()` | No-op filter | Default for optional filters |
| `.and()` on PredicateSql | Combine bound predicates | `p1.forTable("u").and(p2.forTable("o"))` |
| `.or()` on PredicateSql | OR bound predicates | `p1.forTable("u").or(p2.forTable("u"))` |

## Common Pitfalls

### ❌ Forgetting forTable()

```kotlin
// WRONG - predicate is not bound
sql("WHERE ${predicate(myPredicate)}")
```

```kotlin
// CORRECT
sql("WHERE ${predicate(myPredicate.forTable("u"))}")
```

### ❌ Using Wrong Table Alias

```kotlin
// WRONG - predicate bound to "u" but used with table "users"
sql("""
    SELECT * FROM users
    WHERE ${predicate(myPredicate.forTable("u"))}
""")
```

```kotlin
// CORRECT - alias matches
sql("""
    SELECT * FROM users u
    WHERE ${predicate(myPredicate.forTable("u"))}
""")
```

### ❌ Mixing Predicate and PredicateSql in Combinators

```kotlin
// WRONG - mixing bound and unbound
val combined = myPredicate.and(otherPredicate.forTable("u"))
```

```kotlin
// CORRECT - both bound before combining
val combined = myPredicate.forTable("u").and(otherPredicate.forTable("u"))
```

### ❌ Reusing PredicateSql Across Tables

```kotlin
val bound = myPredicate.forTable("u")

// WRONG - reusing bound predicate for different table
sql("""
    SELECT * FROM users u WHERE ${predicate(bound)}
    UNION
    SELECT * FROM admins a WHERE ${predicate(bound)}  -- Still uses "u" alias!
""")
```

```kotlin
// CORRECT - bind separately for each table
sql("""
    SELECT * FROM users u WHERE ${predicate(myPredicate.forTable("u"))}
    UNION
    SELECT * FROM admins a WHERE ${predicate(myPredicate.forTable("a"))}
""")
```

## When to Use Predicates

**Good use cases:**
- Building search/filter functionality with optional parameters
- Reusing common query conditions across multiple functions
- Complex boolean logic that benefits from composition
- Making query functions flexible with predicate parameters
