<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# eVaka Developer Guide

This guide helps developers to discover and understand the custom frameworks, utilities, and conventions built into eVaka.

## Cross-Cutting Concerns

- **[Design Philosophy](design-philosophy.md)** - Core principles guiding technical decisions
- **[Date & Time Utilities](date-time-utilities.md)** - Date, time, and range types with cross-platform serialization
- **[Code Generation](codegen.md)** - Generating front-end types from back-end code. (TODO)

## Frontend

- **[General Conventions](frontend/conventions.md)** - Conventions and best practices for front-end development (TODO)
- **[Component Library](frontend/lib-components.md)** - Reusable UI components
- **[Form Framework](frontend/forms.md)** - Custom form handling utilities
- **[Query Framework](frontend/queries.md)** - Data fetching and caching patterns
- **[Result Type & Async State](frontend/api-result.md)** - Handling Loading/Failure/Success states
- **[E2E Testing](frontend/e2e-testing.md)** - End-to-end testing conventions (TODO)

## Service (Backend)

- **[General Conventions](service/conventions.md)** - Conventions and best practices for back-end development (TODO)
- **[Testing Conventions](service/testing.md)** - Unit and integration testing patterns (TODO)
- **[Database API](service/database.md)** - SQL queries and transactions
- **[SQL Predicates](service/predicates.md)** - Building dynamic WHERE clauses
- **[Database Schema & Migrations](service/migrations.md)** - Schema conventions and Flyway migrations
- **[ACL API](service/acl.md)** - Access control framework (TODO)
- **[Logging API](service/logging.md)** - Logging utilities (TODO)
- **[Async & Scheduled Jobs](service/async-jobs.md)** - Background task execution
