<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# MCP Server for Test Data (non-production only)

eVaka service contains an [MCP (Model Context Protocol)](https://modelcontextprotocol.io) server that lets AI assistants
such as Claude, GitHub Copilot or Cursor create and clean up test data in local, test and staging environments, e.g.
*"create test data into Turku staging so that I can test the placement desktop"*.

**The feature must never be enabled in production.** It is off by default and the service refuses to start if it is
enabled while `VOLTTI_ENV` is `prod` or `production`.

## Enabling

| Component | Setting | Notes |
|-----------|---------|-------|
| evaka-service | Spring profile `enable_mcp` | Active in the `local` profile group and when `VOLTTI_ENV` is `dev` or `test`. In staging add it to `SPRING_PROFILES_ACTIVE` (e.g. `production,enable_mcp`). |
| apigw | `ENABLE_MCP=true` | Mounts the `/api/mcp/**` proxy and the OAuth metadata routes. Enabled automatically in local development. |
| nginx proxy | (no config) | `/.well-known/oauth-authorization-server` and `/.well-known/oauth-protected-resource` always forward to apigw, which returns 404 when the feature is disabled. `/api/mcp/uploads/` allows bodies up to 100 MB. |

The public base URL in the OAuth metadata is `evaka.frontend.base_url.fi`. The employee frontend shows the
**Tekoälytyökalut (MCP)** page in the user menu only when the backend reports `mcpServerEnabled`.

## Connecting a client

The server URL is `https://<evaka-host>/api/mcp` (stateless Streamable HTTP, no SSE), e.g.
`claude mcp add --transport http evaka-local http://localhost:9099/api/mcp`. On first use the client gets a `401`
pointing at `/.well-known/oauth-protected-resource`, registers itself dynamically at `/api/mcp/oauth/register`
(RFC 7591, public client) and opens `/employee/mcp/authorize` in the browser. The user logs in with the normal employee
login and approves the request; **only `ADMIN` users can approve**, and they choose how long the authorization is valid
(1, 7, 30 or 90 days). The client then exchanges the code at `/api/mcp/oauth/token` (mandatory PKCE S256) for a bearer
token. Clients cache their registration, so after a database reset they come back with an unknown client id; the
client is then registered again with the same id (as a public client) when the user approves.

Every request runs as the approving employee, whose roles are re-read on every request, so removing the admin role or
deactivating the employee disables their tokens immediately. Every tool call is audit-logged as `McpToolCall`.

## How the assistant is expected to work

The assistant is assumed to also see the eVaka source code (a local checkout or a GitHub integration), so the MCP
tools don't document domain rules or validation. `get_environment_info` returns `appCommit`, so the assistant can
read the code at the deployed version.

| Tool | Purpose |
|------|---------|
| `get_environment_info` | Date, `appCommit`, reference data and the caller's batches |
| `search_units`, `search_persons`, `search_employees` | Find existing data |
| `insert_rows` | Insert rows of the dev-api `Dev*` fixture classes |
| `create_upload_url` | One-time URL for sending a large `insert_rows` request as a file |
| `advance_application` | Move an application forward with the real application workflow |
| `send_message` | Send a message from an employee's personal account with the real messaging logic |
| `list_test_data`, `delete_test_data` | List and delete batches (`dryRun`, `allowUntrackedRows`) |

`insert_rows` takes a batch name and groups of rows, e.g. `{"batch": "demo", "rows": [{"type": "care_area", "rows":
[...]}, {"type": "daycare", "rows": [...]}]}`. Each row is the JSON form of a Kotlin `Dev*` class
(`service/src/main/kotlin/evaka/core/shared/dev/`, the same shape as the generated E2E types used in
`frontend/src/e2e-test/dev-api/fixtures.ts`) and is inserted with the existing `tx.insert(...)` helper, so no business
validation is run. The row types are registered in `McpToolsRows.kt`, and the tool description lists them. The
assistant generates the ids of rows that later rows refer to. One call is one transaction: an error names the failing
row and includes the Jackson or PostgreSQL message verbatim, and nothing is inserted. Inserted persons are marked as
already fetched from VTJ, because the staging VTJ test service doesn't know generated SSNs; eVaka then uses the
inserted guardian and fridge relations.

### Bulk data

Large data sets (months of reservations and attendances for a whole municipality) should not pass through the model's
tokens. The assistant writes the `insert_rows` arguments into a file with a script, calls `create_upload_url` and
sends the file with `curl`:

```bash
python3 generate_rows.py > rows.json   # {"batch": "turku-demo", "rows": [{"type": "child", "rows": [...]}, ...]}
curl -X POST -H 'Content-Type: application/json' --data-binary @rows.json \
  https://staging-evaka.turku.fi/api/mcp/uploads/<token>
# {"batch": "turku-demo", "inserted": {"child": 400, "guardian": 400, ...}, "trackedDependentRows": {...}}
```

The upload runs through the same code path as an `insert_rows` tool call (same transaction, errors and `McpToolCall`
audit log, with `via=upload`).

## Batches and deletion

Every row created via MCP is tracked in `mcp_test_data_entity` under a batch named by the assistant, and a batch
belongs to the employee who authorized the client. After inserting, `insert_rows` follows foreign keys from the
inserted rows and also tracks the rows that the `Dev*` helpers or service code created alongside them (e.g. message
accounts), so a data set built with MCP deletes without surprises.

A batch is deleted with `delete_test_data` or on the **Tekoälytyökalut (MCP)** page, which also lists and revokes
the user's authorizations. `McpCascadeDeleter` follows foreign keys from the PostgreSQL catalogs recursively and deletes
everything that depends on the batch, including rows added later through the UI. Because that can reach data
someone else relies on:

- `ON DELETE SET NULL` foreign keys are cleared instead of followed (deleting a test employee doesn't delete the unit
  they handle finance decisions for).
- Deletion refuses if it would delete a row tracked in another batch.
- Rows that are reached but not tracked in the batch ("untracked rows") are shown in the dry run and the UI preview,
  and `delete_test_data` refuses to delete them unless called with `allowUntrackedRows: true`, which the assistant is
  told to do only after showing the dry run to the user. Link tables without a uuid `id` (e.g. `guardian`) are never
  reported.

## Security rules

- Non-production only (see above), and only `ADMIN` users can authorize a client or use the tools.
- `delete_test_data` only deletes the caller's own batches.
- `advance_application` only accepts applications created via MCP, and refuses to accept placement proposals while the
  unit has pending proposals for other applications.
- `send_message` only sends as employees created via MCP.
- `insert_rows` accepts `employee_pin` rows only for employees created via MCP, since a PIN allows logging in as the
  employee in the mobile app. Credential-like row types (mobile devices, citizen users) are not available at all.
- Upload URLs are single-use, expire in 10 minutes and are valid only while the authorization that created them is
  active. Upload tokens, access tokens and authorization codes are stored only as SHA-256 hashes.

## Implementation overview

- `evaka.core.mcp`: `McpServerController` (`POST /mcp`, JSON-RPC 2.0), `McpOAuthController` (metadata, registration,
  token), `McpEmployeeController` (consent and admin pages), `McpTools*` (tools), `McpUpload` (upload URLs),
  `McpTestDataService` and `McpCascadeDeleter`. All beans are behind `@Profile("enable_mcp")`.
- apigw `mcp/router.ts` forwards `/api/mcp/**` without a session or body parsing. The client's `Authorization` header
  is passed on as `X-Evaka-Mcp-Authorization`, because `Authorization` carries the apigw JWT.
- Employee frontend: `components/mcp/`; shared code only has the two routes and the user-menu link.
- Tables (`V614__mcp_server.sql`): `mcp_client`, `mcp_authorization`, `mcp_upload`, `mcp_test_data_batch`,
  `mcp_test_data_entity`.

In production the only active code is a set of small hooks in shared files: the profile check in `HttpAccessControl`,
the `mcpServerEnabled` flag, the `Action`, `Audit` and `Id` declarations, the empty `mcp_*` tables, the apigw
`ENABLE_MCP` flag, the nginx `/.well-known/oauth-*` and `/api/mcp/uploads/` locations and the frontend routes and link.
