// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import { set } from 'lodash/fp'
import { faPlus } from 'lib-icons'
import LocalDate from 'lib-common/local-date'
import { getAge } from 'lib-common/utils/local-date'
import ListGrid from 'lib-components/layout/ListGrid'
import { Gap } from 'lib-components/white-space'
import { Label } from 'lib-components/typography'
import InputField from 'lib-components/atoms/form/InputField'
import FormModal from 'lib-components/molecules/modals/FormModal'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { useTranslation } from '../../state/i18n'
import { createPerson, CreatePersonBody } from '../../api/person'
import { CHILD_AGE } from '../../constants'

type Form = Omit<CreatePersonBody, 'dateOfBirth'> & {
  dateOfBirth: string
}

export default React.memo(function CreatePersonModal({
  closeModal
}: {
  closeModal: () => void
}) {
  const { i18n, lang } = useTranslation()
  const [form, setForm] = useState<Form>({
    firstName: '',
    lastName: '',
    dateOfBirth: '',
    streetAddress: '',
    postalCode: '',
    postOffice: ''
  })
  const [requestInFlight, setRequestInFlight] = useState(false)
  const [saveError, setSaveError] = useState(false)

  const onConfirm = () => {
    const validForm = validateForm(form)
    if (validForm !== undefined) {
      setRequestInFlight(true)
      setSaveError(false)
      createPerson(validForm)
        .then((result) => {
          if (result.isSuccess) {
            closeModal()
          }
          if (result.isFailure) {
            setSaveError(true)
          }
        })
        .catch(() => {
          setRequestInFlight(false)
        })
    }
  }

  const isAdult = (dateOfBirth: string) => {
    const parsedDateOfBirth = LocalDate.parseFiOrNull(dateOfBirth)
    return parsedDateOfBirth && getAge(parsedDateOfBirth) >= CHILD_AGE
  }

  return (
    <FormModal
      iconColour={'blue'}
      icon={faPlus}
      title={i18n.personSearch.createNewPerson.title}
      resolve={{
        action: onConfirm,
        label: i18n.personSearch.createNewPerson.modalConfirmLabel,
        disabled: requestInFlight || !validateForm(form)
      }}
      reject={{
        action: closeModal,
        label: i18n.common.cancel
      }}
    >
      <ModalContent>
        <ListGrid labelWidth="min-content">
          <Label>{i18n.personSearch.createNewPerson.form.firstName}*</Label>
          <InputField
            value={form.firstName ?? ''}
            onChange={(value) => setForm(set('firstName', value))}
            width="full"
            data-qa="first-name-input"
          />
          <Label>{i18n.personSearch.createNewPerson.form.lastName}*</Label>
          <InputField
            value={form.lastName ?? ''}
            onChange={(value) => setForm(set('lastName', value))}
            width="full"
            data-qa="last-name-input"
          />
          <Label>{i18n.personSearch.createNewPerson.form.dateOfBirth}*</Label>
          <DatePicker
            date={form.dateOfBirth}
            locale={lang}
            onChange={(value) => {
              if (!isAdult(value)) {
                setForm(set('phone', undefined))
                setForm(set('email', undefined))
              }
              setForm(set('dateOfBirth', value))
            }}
            isValidDate={(date) => date.isEqualOrBefore(LocalDate.today())}
            hideErrorsBeforeTouched
            data-qa="date-of-birth-input"
          />
          <Label>{i18n.personSearch.createNewPerson.form.address}*</Label>
          <AddressContainer>
            <InputField
              placeholder={i18n.personSearch.createNewPerson.form.streetAddress}
              value={form.streetAddress ?? ''}
              onChange={(value) => setForm(set('streetAddress', value))}
              width="full"
              data-qa="street-address-input"
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
                  data-qa="postal-code-input"
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
                  data-qa="post-office-input"
                />
              </PostOfficeContainer>
            </PostalCodeAndOfficeContainer>
          </AddressContainer>
          {isAdult(form.dateOfBirth) ? (
            <>
              <Label>{i18n.personSearch.createNewPerson.form.phone}*</Label>
              <InputField
                value={form.phone ?? ''}
                onChange={(value) => setForm(set('phone', value))}
                width="full"
                data-qa="phone-input"
              />
              <Label>{i18n.personSearch.createNewPerson.form.email}</Label>
              <InputField
                value={form.email ?? ''}
                onChange={(value) => setForm(set('email', value))}
                width="full"
                data-qa="email-input"
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
  align-items: center;
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

function validateForm(form: Form): CreatePersonBody | undefined {
  const parsedDateOfBirth =
    form.dateOfBirth && LocalDate.parseFiOrNull(form.dateOfBirth)

  if (
    !form.firstName ||
    !form.lastName ||
    !parsedDateOfBirth ||
    !form.streetAddress ||
    !form.postalCode ||
    !form.postOffice ||
    (getAge(parsedDateOfBirth) >= CHILD_AGE && !form.phone)
  ) {
    return undefined
  }

  return { ...form, dateOfBirth: parsedDateOfBirth }
}
