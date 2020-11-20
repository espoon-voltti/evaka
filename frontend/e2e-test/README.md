<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# EVAKA E2E tests

## Installing dependencies

```sh
yarn install
```

## Lint

### Run linter

```sh
yarn lint
```

### Fix linter errors

```sh
yarn lint-fix
```

## Running tests

**NOTE:** For all test scripts, you can override the target URL with:

```sh
BASE_URL=http://localhost:9999 yarn <cmd>
```

### Run all tests locally

```sh
yarn e2e
```

### Run certain project tests

```sh
yarn e2e-[PROJECT_NAME]
```

### Run tests by fixture or test meta data

```sh
yarn e2e --fixture-meta type=smoke
```

```sh
yarn e2e --test-meta type=regression,subType=application
```

Read more about test meta datas from [Testcafe documentation](https://devexpress.github.io/testcafe/documentation/test-api/test-code-structure.html#specifying-testing-metadata)

### Run tests in certain environment

```sh
yarn e2e-employee --env=local
```

#### Scheduled CircleCI run for wip branch

```yaml
e2e_test_wip:
  triggers:
    - schedule:
        # Test servers are up 6-20. Using UTC time, taking into account daylight saving time
        # your own timestamps here
        cron: "0,10,20,30,40,50 4-18 * * 1-5"
        filters:
          branches:
            only:
              - EVAKA-111-your-branch-name-here
  jobs:
      - lint
      - e2e-smoke:
          requires:
            - lint
      - e2e-regression:
          requires:
            - e2e-smoke
```

[The cron schedule expression editor](https://crontab.guru/)

## Creating new test

- Create new test under the right test folder (= enduser)
- Name test file clearly to represent test case
- Oraganize tests into categories ([Fixtures](https://devexpress.github.io/testcafe/documentation/test-api/test-code-structure.html#fixtures))
- Write test cases under the fixtures ([Test](https://devexpress.github.io/testcafe/documentation/test-api/test-code-structure.html#tests))
- Create [Page model](https://devexpress.github.io/testcafe/documentation/recipes/use-page-model.html) for test cases

## Automatic test cases

- **Smoke:** Used for test critical and simple service basic functionality (etc. login tests)
- **Regression:** Browser tests to test the largest functionality of the service (etc. creating and sending applications)

**HOX! Remember to include correct fixture type for tests (smoke and regression) depending on test case**

Example of simple smoke test case:

```typescript
fixture('Running application')
    .meta({ type: 'smoke', subType: 'start-application' })
    .page(https://www.example.com)

test('Application main page is visible', async (t) => {
    const application = Selector('#application-wrapper'),
    await t.expect(application.visible).ok()
})
```

## Known issues

- Some selectors still use IDs/classes instead of data-qa attributes
- Should replace for loops by selector's visibilitycheck option
- Tests aren't run against multiple different browsers
- Test data isn't cleared from local DB on test failure when running with `--stop-on-first-fail`
  - Testcafe issue: `Fixture.after` isn't called when using the option (unlike `.afterEach`)

## More information

- Tests are written using Typescript and Testcafe, see the [documentation](https://devexpress.github.io/testcafe/documentation/test-api/).
