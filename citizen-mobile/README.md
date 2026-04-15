<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# citizen-mobile (PoC)

React Native (Expo) mobile app for eVaka citizens. Proves out weak login with long-lived mobile sessions and push notifications on new messages.

## Scope

Login, inbox, thread view + reply, push notifications. Espoo-only. Android-primary (iOS push requires an Apple Developer account — not part of the PoC).

See design: `../docs/superpowers/specs/2026-04-14-citizen-mobile-poc-design.md`.

## Running locally (Android)

1. Start the local evaka stack:
   ```bash
   cd ../compose && docker compose up -d
   cd ../service && ./gradlew bootRun
   cd ../apigw && yarn dev
   ```
2. Seed a weak-login test citizen via `dev-api` (see `frontend/src/e2e-test/dev-api`).
3. Boot an Android emulator with a **Google Play Services** image.
4. Build + install the dev client (first time): `eas build --profile development --platform android` and `adb install <apk>`.
5. Start Metro: `npx expo start --dev-client`.

The default `apiBaseUrl` points to `http://10.0.2.2:3000/api` (Android emulator → host loopback). Override via `app.json` `expo.extra.apiBaseUrl` if needed.

## Push notifications

Expo Push Service. Requires FCM credentials to be configured once in your Expo project via `eas credentials`. Payloads are minimal (generic title/body + `threadId` only).

## Tests

- Maestro flow: see `maestro/README.md`.
- Type check: `npx tsc --noEmit`.

## Known limitations / follow-ups

- Weak login only; strong auth (Suomi.fi) not in scope.
- Messaging: read + reply only. No new threads, no attachments.
- iOS push untested.
- Multi-municipality not supported.
