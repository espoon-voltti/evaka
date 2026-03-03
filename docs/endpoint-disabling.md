<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Endpoint Disabling — Quick Reference

Selectively disable API endpoints at runtime via Valkey. Changes take effect within 10 seconds.

## Commands

Connect to Valkey (adjust host/port for your environment):

    valkey-cli -h <host> -p <port>

### Disable an endpoint

    SADD disabled-endpoints "POST /citizen/absences/*"

### Disable all methods for a path

    SADD disabled-endpoints "* /employee/absences/**"

### List currently disabled endpoints

    SMEMBERS disabled-endpoints

### Re-enable an endpoint

    SREM disabled-endpoints "POST /citizen/absences/*"

### Re-enable all endpoints

    DEL disabled-endpoints

## Pattern Syntax

| Pattern | Meaning |
|---------|---------|
| `/citizen/absences/123` | Exact path match |
| `/citizen/absences/*` | `*` matches exactly one path segment |
| `/citizen/absences/**` | `**` matches zero or more path segments |
| `/citizen/**/decisions` | `**` works in any position |

## Entry Format

Each entry is `METHOD /path/pattern`:

- `METHOD` is an HTTP method (`GET`, `POST`, `PUT`, `DELETE`, `PATCH`) or `*` for all methods
- `/path/pattern` is the URL path with optional wildcards

## Response

Disabled endpoints return `503 Service Unavailable` with body:

    {"errorCode": "ENDPOINT_DISABLED", "matchedPattern": "POST /citizen/absences/*"}
