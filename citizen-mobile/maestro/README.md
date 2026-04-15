<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Maestro smoke tests

## Setup

1. Install Maestro: `curl -Ls "https://get.maestro.mobile.dev" | bash`
2. Start the local evaka stack and seed a test citizen with a known weak-login password using `dev-api` (mirror the helpers in `frontend/src/e2e-test/dev-api`).
3. Build + install the Android dev APK (see the main `README.md`).
4. Ensure the emulator is booted: `adb devices` shows a device.

## Run

```bash
maestro test maestro/login-and-reply.yaml
```

The flow expects the seeded citizen's credentials to be `test.citizen@example.com` / `test-password`; adjust the YAML or the seed if you use different values.
