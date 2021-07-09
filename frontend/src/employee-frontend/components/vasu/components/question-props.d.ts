// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  CheckboxQuestion,
  MultiSelectQuestion,
  RadioGroupQuestion,
  TextQuestion
} from '../vasu-content'

export interface QuestionProps<
  T extends
    | TextQuestion
    | CheckboxQuestion
    | RadioGroupQuestion
    | MultiSelectQuestion
> {
  questionNumber: string
  question: T
}
