// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  CheckboxQuestion,
  DateQuestion,
  Followup,
  MultiFieldListQuestion,
  MultiFieldQuestion,
  MultiSelectQuestion,
  RadioGroupQuestion,
  TextQuestion
} from 'lib-common/api-types/vasu'

export interface QuestionProps<
  T extends
    | TextQuestion
    | CheckboxQuestion
    | RadioGroupQuestion
    | MultiSelectQuestion
    | Followup
    | DateQuestion
    | MultiFieldListQuestion
    | MultiFieldQuestion
> {
  questionNumber: string
  question: T
}
