<!--
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# lib-customizations

Package to enable customizing eVaka frontend(s) in forks without requiring
merges or upstreaming all possible configurations.

## Usage

**NOTE:** No new library dependencies can be installed as all the customization
files are included in the main eVaka lib-customizations builds as is, and don't
form a separate package/project.

**NOTE 2:** Currently `employee-mobile-frontend` isn't included but can be added
later on.

Assuming directory layout like:

```sh
myfork/
├── frontend/
│   └── mytown/ # <- Your customizations, matches espoo/ contents
│          ├── citizen.tsx
│          └── ...
└── evaka/ (git submodule or similar)
    └── frontend/
        └── src/
            └── lib-customizations/
                ├── espoo/      # <- Default customizations for Espoo
                ├── citizen.tsx # <- "root modules" that expose customizations
                └── ...
```

1. Link your own directory to `./<your customization>`
    - E.g. with symbolic or hard links:

        ```sh
        # Running in evaka/frontend.
        # Assuming your customizations name is "mytown" and is placed in ../../frontend/mytown.
        ln -v -s $(readlink -f ../../frontend/mytown) src/lib-customizations/mytown
        ```

1. Run the build / dev server with `EVAKA_CUSTOMIZATIONS=<your customization>`, e.g.:

    ```sh
    # Build
    EVAKA_CUSTOMIZATIONS=mytown yarn build

    # Dev
    EVAKA_CUSTOMIZATIONS=mytown yarn dev
    ```

    - Applies to all Vite actions
1. Customizations can freely be split into multiple files but everything must be
  reachable by following the import chain from the main files:
    - [`citizen.tsx`](./citizen.tsx)
    - [`employee.tsx`](./employee.tsx)
    - [`common.tsx`](./common.tsx)

### Creating customizable modules

Customizations generally consist of five basic parts:

1. Type definitions in [`types.d.ts`](./types.d.ts)
1. Import & export in one of the "root" modules,
  e.g. [`citizen.tsx`](./citizen.tsx), using the `@evaka/customizations/<module>`
  style of import paths (replaced by Vite dynamically at build time)
1. Actual implementation and export in `<customization>/<module>`,
  e.g. `mytown/citizen.tsx`
1. Default implementation in `espoo/<module>`, e.g. `espoo/citizen.tsx`
1. Import and usage of customized module in a project:

    ```tsx
    // Note the import path: nothing about "espoo"
    import { featureFlags } from 'lib-customizations/citizen'

    if (featureFlags.urgencyAttachmentsEnabled) {
      doStuff()
    }
    ```

**NOTE:** You might need to update `frontend/.gitignore` to add some new files.
