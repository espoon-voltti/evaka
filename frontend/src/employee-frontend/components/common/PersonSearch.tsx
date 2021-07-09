// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result, Success } from 'lib-common/api'
import { getAge } from 'lib-common/utils/local-date'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { useRestApi } from 'lib-common/utils/useRestApi'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import Combobox from '../../../lib-components/atoms/form/Combobox'
import {
  findByNameOrAddress,
  getOrCreatePersonBySsn,
  getPersonDetails
} from '../../api/person'
import { CHILD_AGE } from '../../constants'

import { useTranslation } from '../../state/i18n'
import { PersonDetails } from '../../types/person'
import { formatName } from '../../utils'
import { isSsnValid } from '../../utils/validation/validations'

const Container = styled.div`
  margin: 10px 0;
`

const search = async (q: string): Promise<Result<PersonDetails[]>> => {
  if (isSsnValid(q.toUpperCase())) {
    return await getOrCreatePersonBySsn(q.toUpperCase(), false).then((res) =>
      res.map((r) => [r])
    )
  }

  if (q.length === 36) {
    return await getPersonDetails(q).then((res) => res.map((r) => [r]))
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
  const [persons, setPersons] = useState<Result<PersonDetails[]>>(
    Success.of([])
  )
  const [selectedPerson, setSelectedPerson] = useState<PersonDetails>()
  const debouncedQuery = useDebounce(query, 500)

  useEffect(() => {
    onResult(selectedPerson)
  }, [selectedPerson]) // eslint-disable-line react-hooks/exhaustive-deps

  const searchPeople = useRestApi(search, setPersons)
  useEffect(() => {
    searchPeople(debouncedQuery)
  }, [debouncedQuery]) // eslint-disable-line react-hooks/exhaustive-deps

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
    () => persons.map((ps) => filterPeople(ps)).getOrElse([]),
    [persons] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const formatItemLabel = useCallback(
    ({ firstName, lastName }: PersonDetails): string =>
      formatName(firstName, lastName, i18n),
    [i18n]
  )

  const formatMenuItemLabel = useCallback(
    ({
      dateOfBirth,
      firstName,
      lastName,
      streetAddress
    }: PersonDetails): string =>
      `${formatName(firstName, lastName, i18n)} (${dateOfBirth.format()})${
        streetAddress ? `\n${streetAddress}` : ''
      }`,
    [i18n]
  )

  return (
    <Container>
      <Combobox
        placeholder={i18n.common.search}
        clearable
        selectedItem={selectedPerson ?? null}
        items={options}
        getItemLabel={formatItemLabel}
        getMenuItemLabel={formatMenuItemLabel}
        getItemDataQa={({ id }) => `value-${id}`}
        onInputChange={setQuery}
        onChange={(option) => setSelectedPerson(option || undefined)}
        isLoading={persons.isLoading || query !== debouncedQuery}
        menuEmptyLabel={i18n.common.noResults}
        filterItems={(_, items) => items}
        onFocus={onFocus}
      />
    </Container>
  )
}

export default PersonSearch
