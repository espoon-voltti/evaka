// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { SetStateAction } from 'react'
import styled from 'styled-components'
import { set } from 'lodash/fp'
import LocalDate from 'lib-common/local-date'
import ListGrid from 'lib-components/layout/ListGrid'
import { Gap } from 'lib-components/white-space'
import { Label } from 'lib-components/typography'
import InputField from 'lib-components/atoms/form/InputField'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { useTranslation } from '../../state/i18n'
import { CreatePersonBody } from '../../api/person'

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
      {personType == 'NEW_NO_SSN' && (
        <ListGrid labelWidth="min-content">
          <Label>{i18n.personSearch.createNewPerson.form.firstName}*</Label>
          <InputField
            value={createPersonInfo.firstName ?? ''}
            onChange={(value) => setCreatePersonInfo(set('firstName', value))}
            onFocus={onFocus}
            width="full"
            dataQa="input-first-name"
          />
          <Label>{i18n.personSearch.createNewPerson.form.lastName}*</Label>
          <InputField
            value={createPersonInfo.lastName ?? ''}
            onChange={(value) => setCreatePersonInfo(set('lastName', value))}
            onFocus={onFocus}
            width="full"
            dataQa="input-last-name"
          />
          <Label>{i18n.personSearch.createNewPerson.form.dateOfBirth}*</Label>
          <DatePickerDeprecated
            type="full-width"
            date={createPersonInfo.dateOfBirth}
            onChange={(value) => {
              setCreatePersonInfo(set('dateOfBirth', value))
            }}
            onFocus={onFocus}
            maxDate={LocalDate.today()}
            dataQa={'datepicker-dob'}
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
              dataQa="input-street-address"
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
                  dataQa="input-postal-code"
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
                  dataQa="input-post-office"
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
            dataQa="input-phone"
          />
          <Label>{i18n.personSearch.createNewPerson.form.email}</Label>
          <InputField
            value={createPersonInfo.email ?? ''}
            onChange={(value) => setCreatePersonInfo(set('email', value))}
            onFocus={onFocus}
            width="full"
            dataQa="input-email"
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
