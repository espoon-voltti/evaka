<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Design Philosophy

## Purpose

This document captures the core principles and design philosophies that guide technical decisions in eVaka.

## Core Principles

**Minimize Technical Complexity**: We prioritize rapid development with great developer experience, low technical debt, and easy maintenance.

- Simplicity over clever abstractions
- Explicit code over implicit magic
- Explicit dependencies over hidden ones
- Pragmatism over purity

**Type Safety**: If the compiler can catch an error, it should. Strict type safety should be retained even across system boundaries. Code generation produces TypeScript types and API clients from the backend Kotlin code.

**Raw SQL**: We value the explicitness of pure SQL and avoid any complex abstractions over it, even though this creates a rare exception to type safety.

**Selective Dependencies**: Prefer small, focused libraries that do one thing well. Choose simple custom solutions over large frameworks with unused features.

## Architectural Patterns

**API paradigm**: Endpoints may serve specific frontend needs and use RPC style. No need to follow strict REST. Constant improvement and ease of refactoring is preferred over strict backwards compatibility.

**Domain-Driven Organization**: Code is organized by business domain rather than technical layers.

**Database for Structure, Code for Logic**: Database schema (defined in migrations) is the source of truth. JSONB columns may be used selectively, but we prefer relational structures. DB constraints are used heavily for data integrity. Business logic lives in application code - we avoid stored procedures and complex triggers.

**Security at API Boundaries**: Each controller endpoint must first authorize the request. Every successful request must be logged.

## Testing Philosophy

**Test behavior, not implementation**: Focus on high-level, black-box integration and end-to-end tests that execute realistic user flows. Unit tests are reserved for complex and naturally pure functions. Mock only external third-party integrations, never our own code.

**Test all SQL**: Because database queries lack type safety, they must be sufficiently covered by integration tests. 

**Balance coverage with practicality** - Test all features, but not excessively. Focus on realistic scenarios rather than exhaustive corner cases.

## Development Practices

**Documentation**: Code should be self-explanatory through good naming and clear structure. Add documentation only for complex areas, non-obvious business rules, and tricky algorithms. **Comments explain "why," not "what."** Never add trivial comments that simply restate what the code does.

**State Management**: Server-synced state for data (using query framework), local state for UI concerns, global context or query params for user input that must persist during navigation.

**Performance**: Avoid premature performance optimization, but design with performance in mind when creating complex and obviously heavy queries. Take care of low-hanging fruit such as adding necessary indexes.

## See Also

- [Code Generation](codegen.md)
