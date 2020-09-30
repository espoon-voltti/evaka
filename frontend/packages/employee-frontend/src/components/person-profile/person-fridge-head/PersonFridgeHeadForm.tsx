// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { useEffect } from 'react'
import { isFailure, Result } from '~api'
import { fridgeHeadPerson, getPersonDetails } from '~api/person'
import { useContext } from 'react'
import { PersonContext, PersonState } from '~state/person'
import { Buttons } from '~components/shared/alpha'
import { Button } from '~components/shared/alpha'
import { UIContext, UiState } from '~state/ui'
import { Input } from '~components/shared/alpha'
import {
  isEmailValid,
  isPhoneValid,
  allPropertiesTrue
} from '~utils/validation/validations'

import { PersonContactInfo } from '~types/person'

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
        if (isFailure(res)) {
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
              <Input
                value={headPersonForm.email ?? ''}
                onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                  assignHeadPersonForm({ email: event.target.value })
                }
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
              <Input
                value={headPersonForm.phone ?? ''}
                onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                  assignHeadPersonForm({ phone: event.target.value })
                }
              />
              {!formValidation.fields.phone && (
                <div className="error">{i18n.validationError.phone}</div>
              )}
            </div>
          </div>
          <Buttons>
            <Button
              onClick={() => {
                clearUiMode()
              }}
            >
              {i18n.common.cancel}
            </Button>
            <Button
              primary
              disabled={!formValidation.valid}
              onClick={() => {
                headPersonActions()
              }}
            >
              {i18n.common.confirm}
            </Button>
          </Buttons>
        </div>
      ) : null}
    </>
  )
}

export default PersonFridgeHeadForm
