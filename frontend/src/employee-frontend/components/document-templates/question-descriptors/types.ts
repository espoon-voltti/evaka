// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ExoticComponent } from 'react'

import { BoundForm } from 'lib-common/form/hooks'
import { AnyForm, StateOf } from 'lib-common/form/types'
import { Question } from 'lib-common/generated/api-types/document'

export type QuestionType = Question['type']

export interface QuestionDescriptor<
  FORM extends AnyForm,
  QUESTION extends Question
> {
  form: FORM
  getInitialState: (question?: QUESTION) => StateOf<FORM>
  View: ExoticComponent<BoundViewProps<FORM>> // TODO: ExoticComponent sounds wrong, what is the correct type?
  ReadOnlyView: ExoticComponent<BoundViewProps<FORM>>
  TemplateView: ExoticComponent<BoundViewProps<FORM>>
  serialize: (q: StateOf<FORM>) => QUESTION
  deserialize: (q: QUESTION) => StateOf<FORM>
}

export interface BoundViewProps<F extends AnyForm> {
  bind: BoundForm<F>
}
