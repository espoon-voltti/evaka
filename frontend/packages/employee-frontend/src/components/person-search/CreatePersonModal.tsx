// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import { set } from 'lodash/fp'
import { faPlus } from '@evaka/icons'
import LocalDate from '@evaka/lib-common/src/local-date'
import { getAge } from '@evaka/lib-common/src/utils/local-date'
import ListGrid from '~components/shared/layout/ListGrid'
import { Gap } from '~components/shared/layout/white-space'
import { Label } from '~components/shared/Typography'
import InputField from '~components/shared/atoms/form/InputField'
import FormModal from '~components/common/FormModal'
import { DatePicker } from '~components/common/DatePicker'
import { useTranslation } from '~state/i18n'
import { isSuccess, isFailure } from '~api'
import { createPerson, CreatePersonBody } from '~api/person'
import { CHILD_AGE } from '~constants'

export default React.memo(function CreatePersonModal({
  closeModal
}: {
  closeModal: () => void
}) {
  const { i18n } = useTranslation()
  const [form, setForm] = useState<Partial<CreatePersonBody>>({})
  const [requestInFlight, setRequestInFlight] = useState(false)
  const [saveError, setSaveError] = useState(false)

  const onConfirm = () => {
    if (formIsValid(form)) {
      setRequestInFlight(true)
      setSaveError(false)
      createPerson(form)
        .then((result) => {
          if (isSuccess(result)) {
            closeModal()
          }
          if (isFailure(result)) {
            setSaveError(true)
          }
        })
        .catch(() => {
          setRequestInFlight(false)
        })
    }
  }

  return (
    <FormModal
      iconColour={'blue'}
      icon={faPlus}
      title={i18n.personSearch.createNewPerson.title}
      resolve={onConfirm}
      reject={closeModal}
      resolveLabel={i18n.personSearch.createNewPerson.modalConfirmLabel}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={requestInFlight || !formIsValid(form)}
    >
      <ModalContent>
        <ListGrid labelWidth="min-content">
          <Label>{i18n.personSearch.createNewPerson.form.firstName}*</Label>
          <InputField
            value={form.firstName ?? ''}
            onChange={(value) => setForm(set('firstName', value))}
            width="full"
            dataQa="first-name-input"
          />
          <Label>{i18n.personSearch.createNewPerson.form.lastName}*</Label>
          <InputField
            value={form.lastName ?? ''}
            onChange={(value) => setForm(set('lastName', value))}
            width="full"
            dataQa="last-name-input"
          />
          <Label>{i18n.personSearch.createNewPerson.form.dateOfBirth}*</Label>
          <DatePicker
            type="full-width"
            date={form.dateOfBirth}
            onChange={(value) => {
              if (getAge(value) < CHILD_AGE) {
                setForm(set('phone', undefined))
                setForm(set('email', undefined))
              }
              setForm(set('dateOfBirth', value))
            }}
            maxDate={LocalDate.today()}
          />
          <Label>{i18n.personSearch.createNewPerson.form.address}*</Label>
          <AddressContainer>
            <InputField
              placeholder={i18n.personSearch.createNewPerson.form.streetAddress}
              value={form.streetAddress ?? ''}
              onChange={(value) => setForm(set('streetAddress', value))}
              width="full"
              dataQa="street-address-input"
            />
            <Gap size="xs" />
            <PostalCodeAndOfficeContainer>
              <PostalCodeContainer>
                <InputField
                  placeholder={
                    i18n.personSearch.createNewPerson.form.postalCode
                  }
                  value={form.postalCode ?? ''}
                  onChange={(value) => setForm(set('postalCode', value))}
                  width="full"
                  dataQa="postal-code-input"
                />
              </PostalCodeContainer>
              <PostOfficeContainer>
                <InputField
                  placeholder={
                    i18n.personSearch.createNewPerson.form.postOffice
                  }
                  value={form.postOffice ?? ''}
                  onChange={(value) => setForm(set('postOffice', value))}
                  width="full"
                  dataQa="post-office-input"
                />
              </PostOfficeContainer>
            </PostalCodeAndOfficeContainer>
          </AddressContainer>
          {form.dateOfBirth && getAge(form.dateOfBirth) >= CHILD_AGE ? (
            <>
              <Label>{i18n.personSearch.createNewPerson.form.phone}*</Label>
              <InputField
                value={form.phone ?? ''}
                onChange={(value) => setForm(set('phone', value))}
                width="full"
                dataQa="phone-input"
              />
              <Label>{i18n.personSearch.createNewPerson.form.email}</Label>
              <InputField
                value={form.email ?? ''}
                onChange={(value) => setForm(set('email', value))}
                width="full"
                dataQa="email-input"
              />
            </>
          ) : null}
        </ListGrid>
        {saveError ? (
          <>
            <Gap size="m" />
            <ErrorText>{i18n.common.error.unknown}</ErrorText>
          </>
        ) : null}
      </ModalContent>
    </FormModal>
  )
})

const ModalContent = styled.div`
  width: 100%;
  align-items: center;
  padding: 0em 2em;
  width: auto;
`

const AddressContainer = styled.div`
  display: flex;
  flex-direction: column;
`

const PostalCodeAndOfficeContainer = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-between;
`

const PostalCodeContainer = styled.div`
  width: 32%;
`

const PostOfficeContainer = styled.div`
  width: 64%;
`

const ErrorText = styled.div`
  text-align: center;
`

function formIsValid(
  form: Partial<CreatePersonBody>
): form is CreatePersonBody {
  return (
    !!form.firstName &&
    !!form.lastName &&
    !!form.dateOfBirth &&
    !!form.streetAddress &&
    !!form.postalCode &&
    !!form.postOffice &&
    (getAge(form.dateOfBirth) < CHILD_AGE || !!form.phone)
  )
}
