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

Type mappings define how Kotlin types convert to TypeScript. Add entries to the `defaultMetadata` map:

- **Simple types:** Direct string mapping (e.g., `String::class to "string"`)
- **Complex types:** Use `MetadataItem` with custom serializers for special formatting
- **Common scenarios:** Custom date/time types, ID wrappers, sealed classes

**Example pattern:**
```kotlin
// Simple mapping
UUID::class to "UUID"

// Complex mapping with serializers
ChildDocumentId::class to MetadataItem(
    tsType = "ChildDocumentId",
    serializePathVariable = { "childDocumentId" },
    serializeRequestParam = { "childDocumentId.toString()" },
    deserializeJson = { "ChildDocumentId($it)" }
)
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

When adding types that require special formatting, define a `MetadataItem` in `defaultMetadata` with three serializers:

- **`serializePathVariable`** - Format for URL path segments (e.g., `/api/documents/{id}`)
- **`serializeRequestParam`** - Format for query string parameters (e.g., `?startDate=2024-01-01`)
- **`deserializeJson`** - Parse API responses into TypeScript objects

**Use cases:**
- Custom date formats that need string conversion
- Wrapper types that need unwrapping/wrapping
- Types requiring URL encoding or special escaping

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
| `service/codegen/src/main/kotlin/evaka/codegen/api/ApiFiles.kt` | Frontend targets, output paths |
| `service/codegen/src/main/kotlin/evaka/codegen/Generate.kt` | Generation entry point |
| `service/codegen/src/main/kotlin/evaka/codegen/Check.kt` | Validation entry point |

### Debugging Generation Failures

| Error                 | Cause                  | Quick Fix                                   |
|-----------------------|------------------------|---------------------------------------------|
| -                     | -                      | -                                           |

