// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  CheckboxQuestion,
  MultiSelectQuestion,
  RadioGroupQuestion,
  TextQuestion,
  Followup
} from '../vasu-content'

export interface QuestionProps<
  T extends
    | TextQuestion
    | CheckboxQuestion
    | RadioGroupQuestion
    | MultiSelectQuestion
    | Followup
> {
  questionNumber: string
  question: T
}
