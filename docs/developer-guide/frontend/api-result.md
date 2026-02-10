<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Result Type & Async State

## Purpose

Quick reference for eVaka's `Result<T>` type - a functional wrapper for async state that makes loading, error, and success states explicit and type-safe. Instead of managing separate `isLoading`, `error`, and `data` flags, you get a single discriminated union that forces you to handle all states.

**Implementation**: `frontend/src/lib-common/api.ts` and `frontend/src/lib-components/async-rendering.tsx`

## The Result Type

```typescript
type Result<T> = Loading<T> | Failure<T> | Success<T>
```

All three classes have type guards: `isLoading`, `isFailure`, `isSuccess`

**Loading** - Async operation in progress:
```typescript
const loading = Loading.of<User>()
```

**Failure** - Operation failed with error details:
```typescript
const failure = Failure.of<User>({ message: "Network error", statusCode: 500 })
// Or from caught error:
const failure = Failure.fromError<User>(error)
```

**Success** - Operation succeeded with data:
```typescript
const success = Success.of({ id: 1, name: "Alice" })
success.value       // { id: 1, name: "Alice" }
success.isReloading // false (true during background refetch)
```

## Working with Results

### Transforming Data

**map()** - Transform the success value:
```typescript
const user: Result<User> = useQueryResult(userQuery())
const userName: Result<string> = user.map(u => u.name)
// Loading stays Loading, Failure stays Failure
```

**chain()** - Transform into another Result (flat map):
```typescript
const user: Result<User> = useQueryResult(userQuery())
const settings: Result<Settings> = user.chain(u =>
  useQueryResult(settingsQuery(u.id))
)
// Use for dependent queries (though useChainedQuery is often better)
```

**getOrElse()** - Extract value with fallback:
```typescript
const user: Result<User> = useQueryResult(userQuery())
const name = user.map(u => u.name).getOrElse("Unknown")
// Returns "Unknown" if Loading or Failure
```

### Combining Multiple Results

**combine()** - Wait for all results to succeed:
```typescript
import { combine } from 'lib-common/api'

const user = useQueryResult(userQuery())
const settings = useQueryResult(settingsQuery())

const both: Result<[User, Settings]> = combine(user, settings)
// Loading until both succeed, Failure if either fails
```

**map()** - Combine and transform (2-5 results):
```typescript
import { map } from 'lib-common/api'

const combined: Result<UserProfile> = map(
  user,
  settings,
  preferences,
  (u, s, p) => ({ user: u, settings: s, preferences: p })
)
```

### Pattern Matching

**mapAll()** - Handle all states explicitly:
```typescript
const message = result.mapAll({
  loading: () => "Loading...",
  failure: (f) => `Error: ${f.message}`,
  success: (data, isReloading) => `Loaded: ${data.name}`
})
```

## Rendering Results in Components

### renderResult()

Most common way to render Result in JSX - shows spinner while loading, error message on failure:

```typescript
import { renderResult } from 'lib-components/async-rendering'

function UserProfile() {
  const user = useQueryResult(userQuery())

  return renderResult(user, (data, isReloading) => (
    <div>
      <h1>{data.name}</h1>
      {isReloading && <SpinnerOverlay />}
    </div>
  ))
}
```

**Behavior:**
- Loading → Shows `<SpinnerSegment>`
- Failure → Shows `<ErrorSegment>` with message
- Success → Calls your renderer function
- Success + isReloading → Shows content with `<SpinnerOverlay>`

### UnwrapResult

Component form for custom loading/failure handling:

```typescript
import { UnwrapResult } from 'lib-components/async-rendering'

<UnwrapResult
  result={user}
  loading={() => <CustomSpinner />}
  failure={() => <CustomError />}
>
  {(data, isReloading) => (
    <UserCard user={data} />
  )}
</UnwrapResult>
```

**When to use:**
- Custom loading or error UI
- Conditional rendering based on result state
- Most cases: prefer `renderResult()` for consistency

### Manual State Handling

For full control, use type guards directly:

```typescript
const user = useQueryResult(userQuery())

if (user.isLoading) return <Spinner />
if (user.isFailure) return <Error message={user.message} />
return <UserCard user={user.value} />
```

## Deprecated Patterns

⚠️ **wrapResult**, **useApiState**, and **useRestApi** - Deprecated patterns for manual API state management. Refactor to use the [query framework](queries.md) instead.

## Common Patterns

### Conditional Rendering Based on Multiple Results

```typescript
const user = useQueryResult(userQuery())
const settings = useQueryResult(settingsQuery())

return renderResult(combine(user, settings), ([u, s]) => (
  <Profile user={u} settings={s} />
))
```

### Error Handling with Specific Error Codes

```typescript
const result = useQueryResult(sensitiveDataQuery())

if (result.isFailure && result.statusCode === 403) {
  return <PermissionDenied />
}

return renderResult(result, (data) => <SensitiveData data={data} />)
```

## See Also

- [Query Framework](queries.md) - Data fetching with automatic Result integration
- [TanStack Query Documentation](https://tanstack.com/query/latest) - Underlying query library
