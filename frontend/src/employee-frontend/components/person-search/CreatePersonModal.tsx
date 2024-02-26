// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { set } from 'lodash/fp'
import React, { useState } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { CreatePersonBody } from 'lib-common/generated/api-types/pis'
import LocalDate from 'lib-common/local-date'
import { getAge } from 'lib-common/utils/local-date'
import InputField from 'lib-components/atoms/form/InputField'
import ListGrid from 'lib-components/layout/ListGrid'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import { CHILD_AGE } from '../../constants'
import { createPerson } from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'

const createPersonResult = wrapResult(createPerson)

type Form = Omit<CreatePersonBody, 'dateOfBirth'> & {
  dateOfBirth: LocalDate | null
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
    dateOfBirth: null,
    streetAddress: '',
    postalCode: '',
    postOffice: '',
    phone: '',
    email: null
  })
  const [requestInFlight, setRequestInFlight] = useState(false)
  const [saveError, setSaveError] = useState(false)

  const onConfirm = () => {
    const validForm = validateForm(form)
    if (validForm !== undefined) {
      setRequestInFlight(true)
      setSaveError(false)
      createPersonResult({ body: validForm })
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

  const isAdult = (dateOfBirth: LocalDate) => getAge(dateOfBirth) >= CHILD_AGE

  return (
    <FormModal
      type="info"
      icon={faPlus}
      title={i18n.personSearch.createNewPerson.title}
      resolveAction={onConfirm}
      resolveLabel={i18n.personSearch.createNewPerson.modalConfirmLabel}
      resolveDisabled={requestInFlight || !validateForm(form)}
      rejectAction={closeModal}
      rejectLabel={i18n.common.cancel}
    >
      <ModalContent>
        <ListGrid labelWidth="min-content">
          <Label>{i18n.personSearch.createNewPerson.form.firstName}*</Label>
          <InputField
            value={form.firstName}
            onChange={(value) => setForm(set('firstName', value))}
            width="full"
            data-qa="first-name-input"
          />
          <Label>{i18n.personSearch.createNewPerson.form.lastName}*</Label>
          <InputField
            value={form.lastName}
            onChange={(value) => setForm(set('lastName', value))}
            width="full"
            data-qa="last-name-input"
          />
          <Label>{i18n.personSearch.createNewPerson.form.dateOfBirth}*</Label>
          <DatePicker
            date={form.dateOfBirth}
            locale={lang}
            onChange={(value) => {
              if (!value || !isAdult(value)) {
                setForm(set('phone', ''))
                setForm(set('email', null))
              }
              setForm(set('dateOfBirth', value))
            }}
            maxDate={LocalDate.todayInSystemTz()}
            hideErrorsBeforeTouched
            data-qa="date-of-birth-input"
          />
          <Label>{i18n.personSearch.createNewPerson.form.address}*</Label>
          <AddressContainer>
            <InputField
              placeholder={i18n.personSearch.createNewPerson.form.streetAddress}
              value={form.streetAddress}
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
                  value={form.postalCode}
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
                  value={form.postOffice}
                  onChange={(value) => setForm(set('postOffice', value))}
                  width="full"
                  data-qa="post-office-input"
                />
              </PostOfficeContainer>
            </PostalCodeAndOfficeContainer>
          </AddressContainer>
          {form.dateOfBirth && isAdult(form.dateOfBirth) ? (
            <>
              <Label>{i18n.personSearch.createNewPerson.form.phone}*</Label>
              <InputField
                value={form.phone}
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
  const parsedDateOfBirth = form.dateOfBirth

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
