// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  ApplicationAction,
  SimpleApplicationMutationAction
} from './ApplicationActions'

export function isSimpleApplicationMutationAction(
  action: ApplicationAction
): action is SimpleApplicationMutationAction {
  return 'actionType' in action
}
