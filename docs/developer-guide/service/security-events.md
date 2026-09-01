<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Security Events

Certain audit events are also security events: they must be marked with `securityEvent = true` and a `securityLevel`, so that security monitoring can be built on filters applied to the audit log stream. `securityEvent` and `securityLevel` are constructor arguments on the `Audit` enum constant, and which event carries which marking lives in `Audit.kt`. For how audit events are produced, see [Audit logging](logging.md).

## Deciding the marking

Take the first branch that applies, so an action that fits two levels gets the higher one.

```mermaid
flowchart TD
    A["New or changed audit event"] --> B{"Does it create, delete, activate<br/>or deactivate a user account?"}
    B -- "yes" --> HIGH["securityEvent = true<br/>securityLevel = high"]
    B -- "no" --> C{"Does it grant or remove<br/>rights held by a user?"}
    C -- "yes" --> D{"Do those rights<br/>apply system-wide?"}
    D -- "yes" --> HIGH
    D -- "no: scoped to a unit,<br/>a group or one person" --> MED["securityEvent = true<br/>securityLevel = medium"]
    C -- "no" --> E{"Does it create, replace<br/>or revoke a credential?"}
    E -- "yes" --> MED
    E -- "no" --> F{"Is it a login,<br/>or an attempt at one?"}
    F -- "yes" --> LOW["securityEvent = true<br/>securityLevel = low"]
    F -- "no" --> N["Not a security event:<br/>set neither field"]
```

## Cases the tree does not settle

- **Reads are never security events**, sensitive ones included, and neither are ordinary data changes. Reads open only to a very narrow role would need their own marker; `securityEvent` is not it.
- **A `person` row is a data record, not an account.** What lets a citizen sign in is the credential attached to it.
- **In a flow spanning several events (e.g. employee mobile device pairing), the steps where an acting person is easily identifiable carry the level of what is being authorised**, while the steps a device or the gateway performs on its own stay `medium`.
- **An attempt should be logged at the level of what it attempts.**
- **Renaming a credential is not a security event at all** — it changes no authentication material.
- **The same operation carries the same marking wherever it is reached from**, and a scheduled job is marked like the same change made by a person.

These rules cover the Kotlin `Audit` enum; the API gateway hard-codes `securityEvent: true, securityLevel: 'low'` on every event it emits (`apigw/src/shared/logging.ts`).
