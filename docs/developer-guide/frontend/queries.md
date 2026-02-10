<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Query Framework

## Purpose

Quick reference for eVaka's query framework - a thin wrapper over [TanStack Query](https://tanstack.com/query/latest) that reduces boilerplate and enforces consistent patterns. Instead of manually managing query keys and invalidations, you define queries and mutations once, get automatic cache management, and type-safe hooks throughout your codebase.

**Implementation**: `frontend/src/lib-common/query.ts`

**TanStack Query Documentation**: For detailed information about query options, caching behavior, and advanced TanStack Query features, see the [official TanStack Query docs](https://tanstack.com/query/latest/docs/framework/react/overview).

## Basic Setup

Create a `Queries` instance in your feature's `queries.ts` file:

```typescript
// employee-frontend/components/invoices/queries.ts
import { Queries } from 'lib-common/query'
import { getInvoices, sendInvoices } from '../../generated/api-clients/invoicing'

const q = new Queries()

export const invoicesQuery = q.query(getInvoices)
export const sendInvoicesMutation = q.mutation(sendInvoices, [
  invoicesQuery.prefix
])
```

**Pattern**: One `Queries` instance per feature area (invoices, applications, etc.) in a `queries.ts` file that exports all queries and mutations for that feature.

## Basic Queries

### Defining Queries

```typescript
import { getInvoices, getInvoice } from '../../generated/api-clients/invoicing'

const q = new Queries()

// Simple query - no arguments
export const invoiceCodesQuery = q.query(getInvoiceCodes)

// Query with arguments
export const invoicesQuery = q.query(getInvoices)

// Query with options
export const invoiceDetailsQuery = q.query(getInvoice, {
  refetchOnWindowFocus: false,
  staleTime: 60000 // 1 minute
})
```

**Query keys are generated automatically**: `[commonPrefix, functionName, ...args]`

### Using Queries in Components

```typescript
import { useQueryResult } from 'lib-common/query'
import { invoicesQuery } from './queries'

function InvoicesPage() {
  const invoices = useQueryResult(
    invoicesQuery({
      searchTerms: '',
      page: 1
    })
  )

  // invoices is Result<Invoice[]> - see api-result.md
  return renderResult(invoices, (data) => (
    <InvoiceList invoices={data} />
  ))
}
```

**Returns**: `Result<T>` type (Loading | Failure | Success). See [api-result.md](api-result.md) for handling async state.

### Query Options

Common options: `enabled`, `refetchOnMount`, `refetchOnWindowFocus`, `staleTime`, `refetchInterval`

See [TanStack Query Options](https://tanstack.com/query/latest/docs/framework/react/reference/useQuery) for full list.

## Mutations

### Defining Mutations

Mutations modify data and automatically invalidate related queries when they succeed:

```typescript
import { createInvoice, sendInvoices, deleteInvoice } from '../../generated/api-clients/invoicing'

// Invalidate all invoices queries after creating
export const createInvoiceMutation = q.mutation(createInvoice, [
  invoicesQuery.prefix
])

// Invalidate multiple query families
export const sendInvoicesMutation = q.mutation(sendInvoices, [
  invoicesQuery.prefix,
  invoiceDetailsQuery.prefix
])

// Conditional invalidation based on mutation argument
export const deleteInvoiceMutation = q.mutation(deleteInvoice, [
  ({ invoiceId }) => invoiceDetailsQuery({ id: invoiceId })
])
```

**Invalidations**: Array of query prefixes or functions that return query configs. Invalidated queries automatically refetch.

### Using Mutations

```typescript
import { useMutation } from 'lib-common/query'
import { sendInvoicesMutation } from './queries'

function SendInvoicesButton() {
  const { mutate, isPending } = useMutation(sendInvoicesMutation)

  return (
    <Button
      onClick={() => mutate({ invoiceIds: [1, 2, 3] })}
      disabled={isPending}
    >
      Send Invoices
    </Button>
  )
}
```

⚠️ **In practice, use `MutateButton` instead** (see below) - this example shows the underlying hook for understanding.

**For error handling**, use `useMutationResult` which returns `Result<T>`:

```typescript
const { mutateAsync } = useMutationResult(sendInvoicesMutation)

const result = await mutateAsync({ invoiceIds: [1, 2, 3] })
// result is Result<void>
```

### UI Integration with Mutations

**MutateButton** - Recommended way to trigger mutations from buttons:

```typescript
import MutateButton from 'lib-components/atoms/buttons/MutateButton'

<MutateButton
  mutation={deleteInvoiceMutation}
  onClick={() => ({ invoiceId: invoice.id })}
  text="Delete"
  onSuccess={() => navigate('/invoices')}
/>
```

**MutateFormModal** - Form modal integrated with mutations:

```typescript
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'

<MutateFormModal
  mutation={createInvoiceMutation}
  onSuccess={() => setModalOpen(false)}
  resolveAction={(form) => ({ ...form })}
  renderForm={() => <InvoiceForm />}
/>
```

⚠️ **Deprecated**: `AsyncButton` and `AsyncFormModal` are deprecated. Use `MutateButton` and `MutateFormModal` instead for better integration with the query framework.

## Advanced Patterns

### Prefixed Queries

Use when you need to invalidate queries based on a partial key (e.g., invalidate all queries for a specific unit):

```typescript
export const getPlacementDesktopDaycareQuery = q.prefixedQuery(
  getPlacementDesktopDaycare,
  ({ unitId }) => unitId,  // Extract prefix from args
  {
    refetchOnMount: false,
    refetchOnWindowFocus: false
  }
)

// Later, invalidate all queries for a specific unit:
export const updateUnitMutation = q.mutation(updateUnit, [
  ({ unitId }) => getPlacementDesktopDaycareQuery.prefix(unitId)
])
```

**Use case**: When mutations affect a subset of cached queries identified by a common parameter (unit ID, user ID, etc.).

### Parametric Queries and Mutations

Pass extra arguments for cache keys or invalidations without passing them to the API function:

**Parametric Query** - Separate cache entries when API doesn't take identifying parameters:

```typescript
// API uses session/device info, doesn't take employeeId as parameter
export const messagingAccountsQuery = q.parametricQuery<
  EmployeeId | undefined
>()(getAccountsByDevice)

// Usage: employeeId is added to cache key but NOT sent to API
const accounts = useQueryResult(messagingAccountsQuery(currentEmployeeId))
```

**Why**: The `employeeId` is added to the query key, so when employees change (logout/login), the cache key changes and forces a refetch. This prevents leaking data from the previous logged-in employee while the API endpoint itself uses session/device authentication.

**Parametric Mutation** - Pass extra data needed for invalidations but not for the API call:

```typescript
export const upsertPlacementDraftMutation = q.parametricMutation<{
  previousUnitId: DaycareId | null
}>()(upsertApplicationPlacementDraft, [
  // Invalidate new unit
  ({ body: { unitId } }) =>
    unitId ? getPlacementDesktopDaycareQuery({ unitId }) : undefined,
  // Also invalidate previous unit if it changed
  ({ previousUnitId, body }) =>
    previousUnitId && previousUnitId !== body.unitId
      ? getPlacementDesktopDaycareQuery({ unitId: previousUnitId })
      : undefined
])

// Usage: pass extra invalidation data along with API args
mutate({
  previousUnitId: oldUnit,  // For invalidation only
  body: { unitId: newUnit, ... }  // Passed to API
})
```

**Why**: You need to invalidate the old unit's cache, but the API only receives the new unit data.

### Chained Queries

Execute queries that depend on the result of previous queries:

```typescript
import { useChainedQuery } from 'lib-common/query'

const user = useQueryResult(userQuery())
const settings = useChainedQuery(
  user.map(u => settingsQuery({ userId: u.id }))
)

// settings will be Loading until user succeeds, then fetch settings
```

**Use case**: Dependent data fetching where the second query needs data from the first.

### Paged Infinite Queries

For pagination with "load more" behavior:

```typescript
export const messagesInfiniteQuery = q.pagedInfiniteQuery(
  (accountId: MessageAccountId) => (page: number) =>
    getReceivedMessages({ accountId, page }),
  (message) => message.id  // Unique ID for deduplication
)

// Usage
import { usePagedInfiniteQueryResult } from 'lib-common/query'

const { data, hasNextPage, fetchNextPage } = usePagedInfiniteQueryResult(
  messagesInfiniteQuery(accountId)
)

return renderResult(data, (messages) => (
  <>
    <MessageList messages={messages} />
    {hasNextPage && <Button onClick={fetchNextPage}>Load More</Button>}
  </>
))
```

**Use case**: Large lists loaded incrementally (infinite scroll, "load more" buttons).

### Select Mutations

Choose between two mutations at runtime based on user selection:

```typescript
import { useSelectMutation, first, second } from 'lib-common/query'

const [mutation, onClick] = useSelectMutation(
  () => isEditMode ? first(editData) : second(createData),
  [editMutation, (data) => ({ id: data.id, ...data })],
  [createMutation, (data) => ({ ...data })]
)

const { mutate } = useMutation(mutation)
const args = onClick()  // Returns appropriate args based on selection
if (args !== cancelMutation) {
  mutate(args)
}
```

**Use case**: Forms that can either create or update based on context, with different mutations and invalidation logic.

## See Also

- [Result Type & Rendering](api-result.md) - Handling Loading/Failure/Success states
- [TanStack Query Documentation](https://tanstack.com/query/latest) - Full reference for query options and behavior
