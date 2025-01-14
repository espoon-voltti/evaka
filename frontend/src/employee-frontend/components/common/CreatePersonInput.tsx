// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { set } from 'lodash/fp'
import React, { SetStateAction } from 'react'
import styled from 'styled-components'

import { CreatePersonBody } from 'lib-common/generated/api-types/pis'
import LocalDate from 'lib-common/local-date'
import InputField from 'lib-components/atoms/form/InputField'
import ListGrid from 'lib-components/layout/ListGrid'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

interface Props {
  createPersonInfo: Partial<CreatePersonBody>
  setCreatePersonInfo: (s: SetStateAction<Partial<CreatePersonBody>>) => void
  personType: string
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void
}

export default React.memo(function CreatePersonInput({
  createPersonInfo,
  setCreatePersonInfo,
  personType,
  onFocus
}: Props) {
  const { i18n } = useTranslation()

  return (
    <>
      {personType === 'NEW_NO_SSN' && (
        <ListGrid labelWidth="min-content">
          <Label>{i18n.personSearch.createNewPerson.form.firstName}*</Label>
          <InputField
            value={createPersonInfo.firstName ?? ''}
            onChange={(value) => setCreatePersonInfo(set('firstName', value))}
            onFocus={onFocus}
            width="full"
            data-qa="input-first-name"
          />
          <Label>{i18n.personSearch.createNewPerson.form.lastName}*</Label>
          <InputField
            value={createPersonInfo.lastName ?? ''}
            onChange={(value) => setCreatePersonInfo(set('lastName', value))}
            onFocus={onFocus}
            width="full"
            data-qa="input-last-name"
          />
          <Label>{i18n.personSearch.createNewPerson.form.dateOfBirth}*</Label>
          <DatePickerDeprecated
            type="full-width"
            date={createPersonInfo.dateOfBirth}
            onChange={(value) => {
              setCreatePersonInfo(set('dateOfBirth', value))
            }}
            onFocus={onFocus}
            maxDate={LocalDate.todayInSystemTz()}
            data-qa="datepicker-dob"
          />
          <Label>{i18n.personSearch.createNewPerson.form.address}*</Label>
          <AddressContainer>
            <InputField
              placeholder={i18n.personSearch.createNewPerson.form.streetAddress}
              value={createPersonInfo.streetAddress ?? ''}
              onChange={(value) =>
                setCreatePersonInfo(set('streetAddress', value))
              }
              onFocus={onFocus}
              width="full"
              data-qa="input-street-address"
            />
            <Gap size="xs" />
            <PostalCodeAndOfficeContainer>
              <PostalCodeContainer>
                <InputField
                  placeholder={
                    i18n.personSearch.createNewPerson.form.postalCode
                  }
                  value={createPersonInfo.postalCode ?? ''}
                  onChange={(value) =>
                    setCreatePersonInfo(set('postalCode', value))
                  }
                  onFocus={onFocus}
                  width="full"
                  data-qa="input-postal-code"
                />
              </PostalCodeContainer>
              <PostOfficeContainer>
                <InputField
                  placeholder={
                    i18n.personSearch.createNewPerson.form.postOffice
                  }
                  value={createPersonInfo.postOffice ?? ''}
                  onChange={(value) =>
                    setCreatePersonInfo(set('postOffice', value))
                  }
                  onFocus={onFocus}
                  width="full"
                  data-qa="input-post-office"
                />
              </PostOfficeContainer>
            </PostalCodeAndOfficeContainer>
          </AddressContainer>

          <Label>{i18n.personSearch.createNewPerson.form.phone}*</Label>
          <InputField
            value={createPersonInfo.phone ?? ''}
            onChange={(value) => setCreatePersonInfo(set('phone', value))}
            onFocus={onFocus}
            width="full"
            data-qa="input-phone"
          />
          <Label>{i18n.personSearch.createNewPerson.form.email}</Label>
          <InputField
            value={createPersonInfo.email ?? ''}
            onChange={(value) => setCreatePersonInfo(set('email', value))}
            onFocus={onFocus}
            width="full"
            data-qa="input-email"
          />
        </ListGrid>
      )}
    </>
  )
})

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
