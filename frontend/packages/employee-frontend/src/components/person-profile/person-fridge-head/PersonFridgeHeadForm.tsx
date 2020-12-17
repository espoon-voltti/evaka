// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { useEffect } from 'react'
import { Result } from '~api'
import { fridgeHeadPerson, getPersonDetails } from '~api/person'
import { useContext } from 'react'
import { PersonContext, PersonState } from '~state/person'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import { UIContext, UiState } from '~state/ui'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import {
  isEmailValid,
  isPhoneValid,
  allPropertiesTrue
} from '~utils/validation/validations'
import { PersonContactInfo } from '~types/person'
import { FixedSpaceRow } from '~components/shared/layout/flex-helpers'

interface Props {
  id: UUID
  personFridgeHead: PersonContactInfo
}

function PersonFridgeHeadForm({ id, personFridgeHead }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext<UiState>(UIContext)
  const { setPerson } = useContext<PersonState>(PersonContext)

  const [headPersonForm, setHeadPersonForm] = useState<PersonContactInfo>()
  const [formValidation, setFormValidation] = useState<{
    valid: boolean
    fields: { email: boolean; phone: boolean }
  }>()

  const isFormValid = (fridgeHeadPerson: PersonContactInfo) => {
    const fields = {
      email: fridgeHeadPerson.email
        ? isEmailValid(fridgeHeadPerson.email)
        : true,
      phone: fridgeHeadPerson.phone
        ? isPhoneValid(fridgeHeadPerson.phone)
        : true
    }
    return {
      valid: allPropertiesTrue(fields),
      fields
    }
  }

  useEffect(() => {
    if (!headPersonForm) {
      setHeadPersonForm(personFridgeHead)
    } else setFormValidation(isFormValid(headPersonForm))
  }, [headPersonForm])

  const assignHeadPersonForm = (value: Partial<PersonContactInfo>) => {
    if (!headPersonForm) throw new Error(`Undefined form data`)
    const mergedHeadPerson = { ...headPersonForm, ...value }
    setHeadPersonForm(mergedHeadPerson)
  }

  const headPersonActions = () => {
    if (!headPersonForm) throw new Error(`Undefined form data`)
    void fridgeHeadPerson(id, headPersonForm).then(
      (res: Result<PersonContactInfo>) => {
        clearUiMode()
        if (res.isFailure) {
          setErrorMessage({
            type: 'error',
            title: i18n.personProfile.fridgeHead.error.edit.title,
            text: i18n.common.tryAgain
          })
        } else {
          void getPersonDetails(id).then((adult) => {
            setPerson(adult)
          })
        }
      }
    )
  }

  return (
    <>
      {headPersonForm && formValidation ? (
        <div className="wrapper">
          <div className="columns">
            <div className="column is-3 label-value-list-row-label">
              {i18n.common.form.email}
            </div>
            <div className="column is-5">
              <InputField
                value={headPersonForm.email ?? ''}
                onChange={(value) => assignHeadPersonForm({ email: value })}
              />
              {!formValidation.fields.email && (
                <div className="error">{i18n.validationError.email}</div>
              )}
            </div>
          </div>
          <div className="columns">
            <div className="column is-3 label-value-list-row-label">
              {i18n.common.form.phone}
            </div>
            <div className="column is-5">
              <InputField
                value={headPersonForm.phone ?? ''}
                onChange={(value) => assignHeadPersonForm({ phone: value })}
              />
              {!formValidation.fields.phone && (
                <div className="error">{i18n.validationError.phone}</div>
              )}
            </div>
          </div>
          <FixedSpaceRow>
            <Button
              onClick={() => {
                clearUiMode()
              }}
              text={i18n.common.cancel}
            />
            <Button
              primary
              disabled={!formValidation.valid}
              onClick={() => {
                headPersonActions()
              }}
              text={i18n.common.confirm}
            />
          </FixedSpaceRow>
        </div>
      ) : null}
    </>
  )
}

export default PersonFridgeHeadForm
