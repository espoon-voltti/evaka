<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Code Generation

Quick reference for eVaka's code generation tool - generates TypeScript types and API clients from Kotlin backend code, maintaining type safety across the full stack.

**Module location:** `service/codegen/`

## Running Codegen

```bash
# Generate TypeScript files
cd service
./gradlew codegen

# Validate generated files match repository
./gradlew codegenCheck
```

**When to run:** After adding endpoints, changing data classes, or modifying sealed hierarchies.

## Common Tasks

### Adding Type Mappings

**File:** `service/codegen/src/main/kotlin/evaka/codegen/api/Config.kt`

Type mappings define how Kotlin types convert to TypeScript. Add entries to the `defaultMetadata` map using `TsRepresentation` types:

- **Primitives:** `TsPlain(type = "string")` — direct TS type, no transformation
- **External types with serialization:** `TsExternalTypeRef(...)` — for types that need custom serialization/deserialization (dates, UUIDs, etc.)
- **Generic wrappers:** `GenericWrapper(default = ...)` — wrapper disappears in TS, inner type is used (e.g., `Id<T>` becomes `string`)

**Examples from `Config.kt`:**
```kotlin
// Primitives — map directly to TS types
String::class to TsPlain(type = "string"),
Int::class to TsPlain(type = "number"),
Boolean::class to TsPlain(type = "boolean"),

// External type with serialization — needs parse/format for JSON, URLs, query params
LocalDate::class to TsExternalTypeRef(
    "LocalDate",
    keyRepresentation = TsCode("string"),
    deserializeJson = { json ->
        TsCode { "${ref(Imports.localDate)}.parseIso(${inline(json)})" }
    },
    serializePathVariable = { value -> value + ".formatIso()" },
    serializeRequestParam = { value, nullable ->
        value + if (nullable) "?.formatIso()" else ".formatIso()"
    },
    Imports.localDate,
),

// Generic wrapper — Id<T> disappears, becomes the inner type (default: string)
Id::class to GenericWrapper(default = String::class),
```

### Excluding Endpoints from Generation

**Method 1:** `@ExcludeCodeGen` annotation on controller methods
- Use for permanent, deliberate exclusions
- Documents intent directly in code

**Method 2:** Add to `endpointExcludes` list in `Config.kt`
- Use for temporary or deprecated endpoints
- Centralized view of excluded endpoints

### Custom Serialization for New Types

**File:** `service/codegen/src/main/kotlin/evaka/codegen/api/Config.kt`

When adding types that need custom formatting, use `TsExternalTypeRef` in `defaultMetadata`. It has three optional serialization hooks:

- **`serializePathVariable`** `(valueExpr: TsCode) -> TsCode` — format for URL path segments (e.g., `.formatIso()`)
- **`serializeRequestParam`** `(valueExpr: TsCode, nullable: Boolean) -> TsCode` — format for query string parameters
- **`deserializeJson`** `(jsonExpr: TsCode) -> TsCode` — parse JSON responses into TypeScript objects (e.g., `LocalDate.parseIso(json)`)

Set a hook to `null` if the type can't be used in that context (e.g., complex objects can't be path variables).

### Adding Frontend Targets

**File:** `service/codegen/src/main/kotlin/evaka/codegen/api/ApiFiles.kt`

To generate API clients for a new frontend project:

1. Add enum value to `TsProject` in `TsCode.kt` (if not already present)
2. Add endpoint filtering and grouping logic in `generateApiFiles()`
3. Configure output paths using `TsProject.{Name} / "generated/api-clients/{file}.ts"`

**Current projects:** citizen-frontend, employee-frontend, employee-mobile-frontend, e2e-test

**Output structure:**
- **Shared types:** `lib-common/generated/api-types/{package}.ts`
- **Client functions:** `{project}/generated/api-clients/{package}.ts`

**Example pattern from existing code:**
```kotlin
val employeeApiClients = endpoints
    .filter { it.path.startsWith("/employee/") }
    .groupBy { TsProject.EmployeeFrontend / "generated/api-clients/${getBasePackage(it.controllerClass)}.ts" }
    .mapValues { (file, endpoints) -> generateApiClients(...) }
```

## Critical Files

| File | Purpose |
|------|---------|
| `service/codegen/src/main/kotlin/evaka/codegen/api/Config.kt` | Type mappings, exclusions, imports |
| `service/codegen/src/main/kotlin/evaka/codegen/api/TsRepresentation.kt` | `TsPlain`, `TsExternalTypeRef`, `GenericWrapper`, and all other representation types |
| `service/codegen/src/main/kotlin/evaka/codegen/api/TsCode.kt` | `TsProject` enum, `TsFile`, `TsImport` |
| `service/codegen/src/main/kotlin/evaka/codegen/api/ApiFiles.kt` | Frontend targets, output paths |
| `service/codegen/src/main/kotlin/evaka/codegen/Generate.kt` | Generation entry point |
| `service/codegen/src/main/kotlin/evaka/codegen/Check.kt` | Validation entry point |

