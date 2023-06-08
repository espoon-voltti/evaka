// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'

import { sentryEventFilter } from './sentry'

// Stack traces in this file are from Espoo's production Sentry. They are
// available from the three dots menu -> Raw stack trace

describe('Sentry event filter', () => {
  it("Should filter out events whose stack doesn't contain a real filename", () => {
    const stacktraceStrings = [
      `TypeError: undefined is not an object (evaluating 'a.h')
  at ? (/calendar:19:340)
  at ? (/calendar:20:189)
  at bo(https://espoonvarhaiskasvatus.fi/:168:420)
  at tq(https://espoonvarhaiskasvatus.fi/:212:396)
  at ? (https://espoonvarhaiskasvatus.fi/:209:29)
  at ? (https://espoonvarhaiskasvatus.fi/:203:338)
  at ge(https://espoonvarhaiskasvatus.fi/:518:108)
  at b(https://espoonvarhaiskasvatus.fi/:515:477)
  at sentryWrapped(../../node_modules/@sentry/browser/esm/helpers.js:90:17)
`,
      `EvalError: Refused to evaluate a string as JavaScript because 'unsafe-eval' is not an allowed source of script in the following Content Security Policy directive: "script-src 'self' 'sha256-z1vaAvxob9VDuw7klCB049Y2Xr6lf7KjhDrsLvsvcPU='".

  at Function([native code])
  at ? (/calendar:173:128)
  at tk(/calendar:41:267)
  at ? (/calendar:42:93)
  at ? (/calendar:42:489)
  at Promise([native code])
  at wk(/calendar:42:389)
  at ? (/calendar:406:116)
  at tk(/calendar:41:267)
  at ? (/calendar:42:93)
  at ? (/calendar:42:489)
  at Promise([native code])
  at wk(/calendar:42:389)
  at qw(/calendar:402:1140)
  at ? (/calendar:412:380)
  at ? (/calendar:423:200)
  at ? (/calendar:393:156)
  at ? ([native code])
  at ? (/calendar:390:93)
  at ? ([native code])
  at sentryWrapped(../../node_modules/@sentry/browser/esm/helpers.js:90:17)`,
      `TypeError: undefined is not an object (evaluating 'response.reputation')
  at ? (webkit-masked-url://hidden/:15:17)`
    ]

    stacktraceStrings.forEach((stacktraceString) => {
      const stackframes = Sentry.defaultStackParser(stacktraceString)
      expect(
        sentryEventFilter({
          exception: { values: [{ stacktrace: { frames: stackframes } }] }
        })
      ).toBeNull()
    })
  })

  it('Should keep events from eVaka source files', () => {
    const stacktraceStrings = [
      `TypeError: undefined is not an object (evaluating 'e.date.toString')
  at buildQueryString(./calendar/CalendarPage.tsx:364:27)
  at openModal(./calendar/CalendarPage.tsx:278:29)
  at Rb(../../node_modules/react-dom/cjs/react-dom.production.min.js:52:317)
  at Xb(../../node_modules/react-dom/cjs/react-dom.production.min.js:52:471)
  at Yb(../../node_modules/react-dom/cjs/react-dom.production.min.js:53:35)
  at Ze(../../node_modules/react-dom/cjs/react-dom.production.min.js:100:68)
  at se(../../node_modules/react-dom/cjs/react-dom.production.min.js:101:380)
  at a(../../node_modules/react-dom/cjs/react-dom.production.min.js:113:65)
  at Jb(../../node_modules/react-dom/cjs/react-dom.production.min.js:292:189)
  at Nb(../../node_modules/react-dom/cjs/react-dom.production.min.js:50:57)
  at jd(../../node_modules/react-dom/cjs/react-dom.production.min.js:105:469)
  at yc(../../node_modules/react-dom/cjs/react-dom.production.min.js:75:265)
  at hd(../../node_modules/react-dom/cjs/react-dom.production.min.js:74:124)
  at Kt([native code])
  at exports.unstable_runWithPriority(../../node_modules/scheduler/cjs/scheduler.production.min.js:18:343)
  at Hb(../../node_modules/react-dom/cjs/react-dom.production.min.js:292:48)
  at gd(../../node_modules/react-dom/cjs/react-dom.production.min.js:73:352)
  at Gt([native code])
  at sentryWrapped(../../node_modules/@sentry/browser/esm/helpers.js:90:17)`,
      `React ErrorBoundary Error: Failed to execute 'removeChild' on 'Node': The node to be removed is not a child of this node.
  at ? (p)
  at O(../../node_modules/styled-components/dist/styled-components.browser.esm.js:1:18447)
  at ? (div)
  at Hc(./applications/editor/service-need/PreferredStartSubSection.tsx:35:3)
  at ? (div)
  at O(../../node_modules/styled-components/dist/styled-components.browser.esm.js:1:18447)
  at ? (section)
  at O(../../node_modules/styled-components/dist/styled-components.browser.esm.js:1:18447)
  at CollapsibleContentArea(../lib-components/layout/Container.tsx:109:5)
  at ? (div)
  at Cl(./applications/editor/EditorSection.tsx:26:16)
  at td(./applications/editor/service-need/ServiceNeedSection.tsx:44:13)
  at ? (div)
  at O(../../node_modules/styled-components/dist/styled-components.browser.esm.js:1:18447)
  at Ld(./applications/editor/ApplicationFormDaycare.tsx:23:3)
  at ExpandingInfoGroup(../lib-components/molecules/ExpandingInfo.tsx:334:3)
  at ? (main)
  at O(../../node_modules/styled-components/dist/styled-components.browser.esm.js:1:18447)
  at bi(../lib-components/atoms/Main.tsx:13:3)
  at ? (div)
  at O(../../node_modules/styled-components/dist/styled-components.browser.esm.js:1:18447)
  at ApplicationEditorContent(./applications/editor/ApplicationEditor.tsx:84:3)
  at ? (div)
  at O(../../node_modules/styled-components/dist/styled-components.browser.esm.js:1:18447)
  at LoadableContent(../lib-components/atoms/state/Spinner.tsx:95:3)
  at RenderResult(../lib-components/async-rendering.tsx:69:5)
  at ? (div)
  at O(../../node_modules/styled-components/dist/styled-components.browser.esm.js:1:18447)
  at vm(./applications/editor/ApplicationEditor.tsx:471:29)
  at Uo(./ScrollToTop.tsx:11:3)
  at Do(./RequireAuth.tsx:17:3)
  at Routes(../../node_modules/react-router/index.js:885:5)
  at <object>.UnwrapResult(../lib-components/async-rendering.tsx:35:5)
  at ? (div)
  at O(../../node_modules/styled-components/dist/styled-components.browser.esm.js:1:18447)
  at MainContainer(./App.tsx:313:3)
  at ? (div)
  at O(../../node_modules/styled-components/dist/styled-components.browser.esm.js:1:18447)
  at Content(./App.tsx:91:13)
  at MessageContextProvider(./messages/state.tsx:83:37)
  at NotificationsContextProvider(../lib-components/Notifications.tsx:54:5)
  at OverlayContextProvider(./overlay/state.tsx:60:37)
  at AuthContextProvider(./auth/state.tsx:50:3)
  at new ErrorBoundary(../../node_modules/@sentry/react/esm/errorboundary.js:52:5)
  at ComponentLocalizationContextProvider(../lib-components/i18n.tsx:48:5)
  at LocalizationContextProvider(./localization/state.tsx:55:42)
  at Fe(../../node_modules/styled-components/dist/styled-components.browser.esm.js:1:17333)
  at Router(../../node_modules/react-router/index.js:819:15)
  at BrowserRouter(../../node_modules/react-router-dom/index.js:75:5)
  at QueryClientProvider(../../node_modules/@tanstack/react-query/build/lib/QueryClientProvider.mjs:41:3)
NotFoundError: Failed to execute 'removeChild' on 'Node': The node to be removed is not a child of this node.
  at cj(../../node_modules/react-dom/cjs/react-dom.production.min.js:231:64)
  at dk(../../node_modules/react-dom/cjs/react-dom.production.min.js:256:468)
  at exports.unstable_runWithPriority(../../node_modules/scheduler/cjs/scheduler.production.min.js:18:343)
  at gg(../../node_modules/react-dom/cjs/react-dom.production.min.js:122:325)
  at Uj(../../node_modules/react-dom/cjs/react-dom.production.min.js:252:279)
  at Lj(../../node_modules/react-dom/cjs/react-dom.production.min.js:243:371)
  at b(../../node_modules/react-dom/cjs/react-dom.production.min.js:123:115)
  at exports.unstable_runWithPriority(../../node_modules/scheduler/cjs/scheduler.production.min.js:18:343)
  at gg(../../node_modules/react-dom/cjs/react-dom.production.min.js:122:325)
  at jg(../../node_modules/react-dom/cjs/react-dom.production.min.js:123:61)
  at ig(../../node_modules/react-dom/cjs/react-dom.production.min.js:122:428)
  at Hb(../../node_modules/react-dom/cjs/react-dom.production.min.js:292:101)
  at gd(../../node_modules/react-dom/cjs/react-dom.production.min.js:73:352)
  at sentryWrapped(../../node_modules/@sentry/browser/esm/helpers.js:90:17)`
    ]

    stacktraceStrings.forEach((stacktraceString) => {
      const stackframes = Sentry.defaultStackParser(stacktraceString)
      const event = {
        exception: { values: [{ stacktrace: { frames: stackframes } }] }
      }
      expect(sentryEventFilter(event)).toBe(event)
    })
  })
})
