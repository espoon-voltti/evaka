// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import { set } from 'lodash/fp'
import { faPlus } from '@evaka/lib-icons'
import LocalDate from '@evaka/lib-common/local-date'
import { getAge } from '@evaka/lib-common/utils/local-date'
import ListGrid from '@evaka/lib-components/layout/ListGrid'
import { Gap } from '@evaka/lib-components/white-space'
import { Label } from '@evaka/lib-components/typography'
import InputField from '@evaka/lib-components/atoms/form/InputField'
import FormModal from '@evaka/lib-components/molecules/modals/FormModal'
import { DatePickerDeprecated } from '@evaka/lib-components/molecules/DatePickerDeprecated'
import { useTranslation } from '../../state/i18n'
import { createPerson, CreatePersonBody } from '../../api/person'
import { CHILD_AGE } from '../../constants'

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

  return (
    <FormModal
      iconColour={'blue'}
      icon={faPlus}
      title={i18n.personSearch.createNewPerson.title}
      resolve={{
        action: onConfirm,
        label: i18n.personSearch.createNewPerson.modalConfirmLabel,
        disabled: requestInFlight || !formIsValid(form)
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
          <DatePickerDeprecated
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
