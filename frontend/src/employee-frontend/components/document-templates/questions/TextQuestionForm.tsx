// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { BoundForm, useFormFields } from 'lib-common/form/hooks'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import { Label } from 'lib-components/typography'

import { textQuestionForm } from '../forms'

interface Props {
  bind: BoundForm<typeof textQuestionForm>
}

export default React.memo(function QuestionModal({ bind }: Props) {
  const { label } = useFormFields(bind)

  return (
    <>
      <Label>Otsikko</Label>
      <InputFieldF bind={label} hideErrorsBeforeTouched />
    </>
  )
})
