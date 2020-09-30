// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import ReactSelect, { components, OptionProps } from 'react-select'
import { Translations, useTranslation } from '~state/i18n'
import {
  findByNameOrAddress,
  getOrCreatePersonBySsn,
  getPersonDetails
} from '~api/person'
import { isLoading, isSuccess, Result, Success } from '~api'
import { formatName } from '~utils'
import { useDebounce } from '~utils/useDebounce'
import { isSsnValid } from '~utils/validation/validations'
import { useRestApi } from '~utils/useRestApi'
import { CHILD_AGE } from '~constants.ts'
import { PersonDetails } from '~types/person'
import { getAge } from '@evaka/lib-common/src/utils/local-date'

const Container = styled.div`
  margin: 10px 0;
`

const customComponents = (i18n: Translations) => ({
  Option: React.memo(function Option(props: OptionProps<PersonDetails>) {
    const {
      id,
      firstName,
      lastName,
      dateOfBirth,
      streetAddress
    } = props.data as PersonDetails
    return (
      <components.Option {...props} data-qa={`value-${id}`}>
        {formatName(firstName, lastName, i18n)} ({dateOfBirth.format()})
        <br />
        {streetAddress}
      </components.Option>
    )
  })
})

const search = async (q: string): Promise<Result<PersonDetails[]>> => {
  if (isSsnValid(q.toUpperCase())) {
    return await getOrCreatePersonBySsn(q.toUpperCase(), false).then((res) =>
      isSuccess(res) ? Success([res.data]) : res
    )
  }

  if (q.length === 36) {
    return await getPersonDetails(q).then((res) =>
      isSuccess(res) ? Success([res.data]) : res
    )
  }

  return await findByNameOrAddress(q, 'last_name,first_name', 'ASC')
}

interface Props {
  onResult: (result: PersonDetails | undefined) => void
  onFocus?: (e: React.FocusEvent<HTMLElement>) => void
  onlyChildren?: boolean
  onlyAdults?: boolean
}

function PersonSearch({
  onResult,
  onFocus,
  onlyChildren = false,
  onlyAdults = false
}: Props) {
  const { i18n } = useTranslation()
  const [query, setQuery] = useState('')
  const [persons, setPersons] = useState<Result<PersonDetails[]>>(Success([]))
  const [selectedPerson, setSelectedPerson] = useState<PersonDetails>()
  const debouncedQuery = useDebounce(query, 500)

  useEffect(() => {
    onResult(selectedPerson)
  }, [selectedPerson])

  const searchPeople = useRestApi(search, setPersons)
  useEffect(() => {
    searchPeople(debouncedQuery)
  }, [debouncedQuery])

  const filterPeople = (people: PersonDetails[]) =>
    people.filter((person) =>
      person.dateOfBirth
        ? onlyChildren
          ? getAge(person.dateOfBirth) < CHILD_AGE
          : onlyAdults
          ? getAge(person.dateOfBirth) >= CHILD_AGE
          : true
        : true
    )

  const options = useMemo(
    () => (isSuccess(persons) ? filterPeople(persons.data) : []),
    [persons]
  )

  return (
    <Container>
      <ReactSelect
        placeholder={i18n.common.search}
        isClearable
        escapeClearsValue
        value={selectedPerson}
        options={options}
        getOptionValue={(option) => option.id}
        getOptionLabel={({ firstName, lastName }) =>
          formatName(firstName, lastName, i18n)
        }
        onInputChange={setQuery}
        onChange={(option) =>
          void setSelectedPerson(option && 'id' in option ? option : undefined)
        }
        isLoading={isLoading(persons) || (!!query && query !== debouncedQuery)}
        loadingMessage={() => i18n.common.loading}
        noOptionsMessage={() => i18n.common.noResults}
        components={customComponents(i18n)}
        filterOption={() => true}
        onFocus={onFocus}
      />
    </Container>
  )
}

export default PersonSearch
