// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Result } from 'lib-common/api'
import type { ActiveQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'

import type { User } from '../auth/state'

export type QuestionnaireAvailability = boolean | 'with-strong-auth'

export function isQuestionnaireAvailable(
  activeQuestionnaires: Result<ActiveQuestionnaire | null>,
  user: User | undefined
): QuestionnaireAvailability {
  return activeQuestionnaires
    .map<QuestionnaireAvailability>((val) =>
      !val || !user
        ? false
        : val.questionnaire.requiresStrongAuth && user.authLevel !== 'STRONG'
          ? 'with-strong-auth'
          : true
    )
    .getOrElse(false)
}
