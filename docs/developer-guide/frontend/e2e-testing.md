<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# E2E Testing Conventions

## Test Isolation

Each test runs against a clean database. Call `resetServiceState()` in `test.beforeEach` before setting up any fixtures:

```typescript
import { resetServiceState } from '../../generated/api-clients'

test.beforeEach(async () => {
  await resetServiceState()
  // fixture setup follows...
})
```

## Fixture Builders

Test data is created through dev-API endpoints using `Fixture` builder methods. Each builder is configured with a fluent API and persisted with `.save()`, which returns the created object with its generated `id`:

```typescript
import { Fixture } from 'e2e-test/dev-api/fixtures'

const area = await Fixture.careArea().save()
const daycare = await Fixture.daycare({ areaId: area.id }).save()
const child = await Fixture.person().saveChild()
await Fixture.placement({ childId: child.id, unitId: daycare.id }).save()

// Employees with roles
const admin = await Fixture.employee().admin().save()
const supervisor = await Fixture.employee().unitSupervisor(daycare.id).save()
```

## Mocked Time

Set `mockedTime` via `test.use()` to control what time the browser and frontend see for all tests in a describe block:

```typescript
const mockedNow = HelsinkiDateTime.of(2024, 3, 15, 10, 0)
test.use({ evakaOptions: { mockedTime: mockedNow } })
```

In multi-page tests, pass `mockedTime` when creating additional pages via the `newEvakaPage` fixture:

```typescript
test('multi-page scenario', async ({ newEvakaPage }) => {
  const page = await newEvakaPage({ mockedTime: mockedNow })
})
```

Dev-api functions also accept `mockedTime` to stamp server-side operations at the correct time:

```typescript
await execSimpleApplicationActions(
  applicationId,
  ['MOVE_TO_WAITING_PLACEMENT'],
  mockedNow,
)
```

Open a new page with a different time when a test needs to observe how the UI looks at multiple points in time.

## Login

```typescript
import { employeeLogin, enduserLogin } from 'e2e-test/utils/user'

await employeeLogin(page, employee)
await enduserLogin(page, citizen)
```

## Selectors

Use `data-qa` attributes for most element targeting. The `Page` and `Element` classes provide convenience methods:

```typescript
page.findByDataQa('submit-button')        // single element
page.findAllByDataQa('application-row')   // collection

// Nested finding
const row = table.findByDataQa('application-row')
row.findByDataQa('approve-button')
```

For cases where `data-qa` alone isn't enough (e.g. waiting for a loading state to clear), use CSS selectors directly:

```typescript
page.find('[data-qa="person-details"][data-isloading="false"]')
```

Avoid selecting by class names, element types, or text content — these are brittle and couple tests to implementation details.

## Page Models

Encapsulate page structure in page model classes. Store locators as fields in the constructor and expose them publicly so tests can drive interactions directly — avoid wrapping every interaction in a method unless the same sequence is genuinely repeated across many tests:

```typescript
export class ApplicationListPage {
  readonly sendButton: Element
  readonly applicationRows: ElementCollection

  constructor(private readonly page: Page) {
    this.sendButton = page.findByDataQa('send-button')
    this.applicationRows = page.findAllByDataQa('application-row')
  }
}

// In the test — direct access to selectors
const listPage = new ApplicationListPage(page)
await listPage.sendButton.click()
await expect(listPage.applicationRows).toHaveCount(3)
```

Add a method only when a sequence of steps is meaningfully reused:

```typescript
async waitUntilLoaded() {
  await expect(
    this.page.find('[data-qa="application-list"][data-isloading="false"]')
  ).toBeVisible()
}
```

Page models live in `frontend/src/e2e-test/pages/`, organized by user type (`employee/`, `citizen/`, `mobile/`).

## Component Wrappers

Use typed wrapper classes instead of raw locators for interactive elements. They handle correct interaction patterns and provide type-appropriate assertions:

```typescript
// In a page model constructor:
this.nameInput = new TextInput(page.findByDataQa('name-input'))
this.activeCheckbox = new Checkbox(page.findByDataQa('active-checkbox'))
this.startDate = new DatePicker(page.findByDataQa('start-date'))
this.typeSelect = new Select(page.findByDataQa('type-select'))
```

Key wrappers and their main methods:

| Wrapper              | Key methods                                                                 |
| -------------------- | --------------------------------------------------------------------------- |
| `TextInput`          | `.fill()`, `.type()`, `.clear()`                                            |
| `Checkbox` / `Radio` | `.check()`, `.uncheck()`, `.waitUntilChecked()`                             |
| `DatePicker`         | `.fill(localDate \| string)`, `.clear()`                                    |
| `DateRangePicker`    | `.fill(start, end)`, `.start`, `.end`                                       |
| `Select`             | `.selectOption()`, `.assertOptions()`                                       |
| `Combobox`           | `.fill()`, `.fillAndSelectFirst()`, `.fillAndSelectItem()`, `.selectItem()` |
| `MultiSelect`        | `.fill()`, `.selectItem()`, `.fillAndSelectFirst()`, `.assertOptions()`     |
| `FileUpload`         | `.upload(path)`, `.deleteUploadedFile(index?)`                              |
| `AsyncButton`        | `.waitUntilIdle()`, `.waitUntilSuccess()`                                   |
| `Modal`              | `.submit()`, `.close()`, `.submitButton`, `.closeButton`                    |
| `Collapsible`        | `.isOpen()`, `.open()`                                                      |

All wrappers are defined in `frontend/src/e2e-test/utils/page.ts`.

## Async Jobs

When a test action triggers background processing (e.g. sending decisions, generating documents), call `runPendingAsyncJobs` before asserting on the result:

```typescript
import { runPendingAsyncJobs } from 'e2e-test/dev-api'

await feeDecisionsPage.sendFeeDecisions(mockedNow)
await runPendingAsyncJobs(mockedNow)
await feeDecisionsPage.assertSentDecisionsCount(1)
```

Pass the same mocked time used by the test so scheduled jobs evaluate at the correct point in time.

## Assertions

Use native Playwright `expect` assertions for all element checks. Import the custom `expect` from `playwright.ts` — it auto-unwraps `Element` and `ElementCollection` to their underlying `locator`:

```typescript
import { test, expect } from '../../playwright'

// Element assertions
await expect(element).toBeVisible()
await expect(element).toBeHidden()
await expect(element).toHaveText('expected')
await expect(element).toContainText('partial')
await expect(element).toBeDisabled()
await expect(element).toBeEnabled()
await expect(element).toHaveAttribute('data-status', 'active')
await expect(element).toHaveValue('input value')

// Collection assertions
await expect(collection).toHaveCount(5)
await expect(collection).toHaveText(['A', 'B', 'C'])

// Custom assertion on Element for predicate-based text checks
await element.assertText((t) => t.includes('partial'))

// Unordered text comparison on ElementCollection
await collection.assertTextsEqualAnyOrder(['C', 'A', 'B'])
```

Do **not** import `expect` from `@playwright/test` — the custom proxy is required for `Element`/`ElementCollection` unwrapping.

## Common Gotchas

**Asserting something isn't visible too early.** If you assert that an element is *not* visible, the assertion may pass for the wrong reason — the element might simply not exist yet because the page or its data is still loading. Always first assert something that confirms the relevant content has fully loaded:

```typescript
// ❌ May pass for the wrong reason — data might still be loading
await expect(errorBanner).toBeHidden()

// ✅ First confirm something that should be visible, then check
await expect(listPage.titleRow).toBeVisible()  // data is loaded
await expect(listPage.errorBanner).toBeHidden() // now safe
```

The same applies to `expect(collection).toHaveCount(0)` and similar "nothing here" assertions.
