// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import ReactSelect, { components, OptionProps } from 'react-select'
import { Translations, useTranslation } from '../../state/i18n'
import { getOrCreatePersonBySsn } from '../../api/person'
import { Result, Success } from '@evaka/lib-common/src/api'
import { formatName } from '../../utils'
import { useDebounce } from '@evaka/lib-common/src/utils/useDebounce'
import { isSsnValid } from '../../utils/validation/validations'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { PersonDetails } from '../../types/person'

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
    return await getOrCreatePersonBySsn(q.toUpperCase(), true).then((res) =>
      res.map((r) => [r])
    )
  }

  return Success.of([])
}

interface Props {
  onResult: (result: PersonDetails | undefined) => void
  onFocus?: (e: React.FocusEvent<HTMLElement>) => void
}

function VtjPersonSearch({ onResult, onFocus }: Props) {
  const { i18n } = useTranslation()
  const [query, setQuery] = useState('')
  const [persons, setPersons] = useState<Result<PersonDetails[]>>(
    Success.of([])
  )
  const [selectedPerson, setSelectedPerson] = useState<PersonDetails>()
  const debouncedQuery = useDebounce(query, 500)

  useEffect(() => {
    onResult(selectedPerson)
  }, [selectedPerson])

  const searchPeople = useRestApi(search, setPersons)
  useEffect(() => {
    searchPeople(debouncedQuery)
  }, [debouncedQuery])

  const options = useMemo(() => persons.getOrElse([]), [persons])

  return (
    <Container data-qa="select-search-from-vtj-guardian">
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
          void setSelectedPerson(
            option && 'socialSecurityNumber' in option ? option : undefined
          )
        }
        isLoading={persons.isLoading || (!!query && query !== debouncedQuery)}
        loadingMessage={() => i18n.common.loading}
        noOptionsMessage={() => i18n.common.noResults}
        components={customComponents(i18n)}
        filterOption={() => true}
        onFocus={onFocus}
      />
    </Container>
  )
}

export default VtjPersonSearch
