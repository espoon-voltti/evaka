// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ApplicationEditorTexts } from 'lib-components/application-editor/translations'
import { translations } from 'lib-customizations/citizen'

// Temporary compile-time proof that the citizen texts satisfy the shared
// editor texts interface; replaced by the deps assembly (Task 6).
export const check: ApplicationEditorTexts = translations.fi.applications.editor
