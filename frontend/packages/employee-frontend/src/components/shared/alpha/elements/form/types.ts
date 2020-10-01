// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { faCheckCircle, faExclamationTriangle } from '@evaka/icons'

export const statusClassNames = {
  error: 'is-error',
  warning: 'is-warning',
  success: 'is-success'
}

export type FieldState = keyof typeof statusClassNames

export type FieldStateIcon = Record<FieldState, IconDefinition>

export const icons: FieldStateIcon = {
  success: faCheckCircle,
  error: faExclamationTriangle,
  warning: faExclamationTriangle
}
