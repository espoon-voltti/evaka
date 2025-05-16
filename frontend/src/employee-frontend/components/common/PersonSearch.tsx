// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import type { PersonSummary } from 'lib-common/generated/api-types/pis'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { tryFromUuid } from 'lib-common/id-type'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { getAge } from 'lib-common/utils/local-date'
import { useDebounce } from 'lib-common/utils/useDebounce'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import type { BaseProps } from 'lib-components/utils'

import {
  personBySsnQuery,
  personIdentityQuery,
  searchPersonQuery
} from '../../queries'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { isSsnValid } from '../../utils/validation/validations'

const Container = styled.div`
  margin: 10px 0;
`

interface Props extends BaseProps {
  getItemDataQa: (item: PersonSummary) => string
  filterItems: (
    inputValue: string,
    items: readonly PersonSummary[]
  ) => PersonSummary[]
  onResult: (result: PersonSummary | undefined) => void
  onFocus?: (e: React.FocusEvent<HTMLElement>) => void
  ageLessThan?: number
  ageAtLeast?: number
  excludePeople?: PersonId[]
  vtjReadOnlySearch: boolean
}

function PersonSearch({
  filterItems,
  onResult,
  onFocus,
  ageLessThan,
  ageAtLeast,
  excludePeople,
  'data-qa': dataQa,
  getItemDataQa,
  vtjReadOnlySearch
}: Props) {
  const { i18n } = useTranslation()
  const [query, setQuery] = useState('')
  const [selectedPerson, setSelectedPerson] = useState<PersonSummary>()
  const debouncedQuery = useDebounce(query, 500)

  const isSsn = useMemo(
    () => isSsnValid(debouncedQuery.toUpperCase()),
    [debouncedQuery]
  )

  const queryFn = useMemo(() => {
    if (vtjReadOnlySearch) {
      if (isSsn) {
        return personBySsnQuery({
          body: {
            ssn: debouncedQuery.toUpperCase(),
            readonly: true
          }
        })
      } else {
        return constantQuery([])
      }
    }

    if (isSsn) {
      return personBySsnQuery({
        body: {
          ssn: debouncedQuery.toUpperCase(),
          readonly: false
        }
      })
    }

    const personId = tryFromUuid<PersonId>(debouncedQuery)
    if (personId !== undefined) {
      return personIdentityQuery({
        personId
      })
    }

    return searchPersonQuery({
      body: {
        searchTerm: debouncedQuery,
        orderBy: 'last_name,first_name',
        sortDirection: 'ASC'
      }
    })
  }, [debouncedQuery, vtjReadOnlySearch, isSsn])

  const persons = useQueryResult(queryFn).map((res) =>
    Array.isArray(res) ? res : [res]
  )

  useEffect(() => {
    onResult(selectedPerson)
  }, [selectedPerson]) // eslint-disable-line react-hooks/exhaustive-deps

  const filterPeople = (people: PersonSummary[]) =>
    people.filter((person) => {
      if (excludePeople && excludePeople.includes(person.id)) return false
      if (!person.dateOfBirth) return true
      const age = getAge(person.dateOfBirth)
      if (ageLessThan !== undefined && age >= ageLessThan) return false
      if (ageAtLeast !== undefined && age < ageAtLeast) return false
      return true
    })

  const options = useMemo(
    () => persons.map((ps) => filterPeople(ps)).getOrElse([]),
    [persons] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const formatItemLabel = useCallback(
    ({ firstName, lastName }: PersonSummary) =>
      formatName(firstName, lastName, i18n),
    [i18n]
  )

  const formatMenuItemLabel = useCallback(
    ({
      dateOfBirth,
      firstName,
      lastName,
      streetAddress
    }: PersonSummary): string =>
      `${formatName(firstName, lastName, i18n)} (${dateOfBirth.format()})${
        streetAddress ? `\n${streetAddress}` : ''
      }`,
    [i18n]
  )

  const onChange = useCallback(
    (option: PersonSummary | null) => setSelectedPerson(option || undefined),
    []
  )
  return (
    <Container data-qa={dataQa}>
      <Combobox
        placeholder={i18n.common.search}
        clearable
        selectedItem={selectedPerson ?? null}
        items={options}
        getItemLabel={formatItemLabel}
        getMenuItemLabel={formatMenuItemLabel}
        getItemDataQa={getItemDataQa}
        onInputChange={setQuery}
        onChange={onChange}
        isLoading={persons.isLoading || query !== debouncedQuery}
        menuEmptyLabel={i18n.common.noResults}
        filterItems={filterItems}
        onFocus={onFocus}
      />
    </Container>
  )
}

type PersonSearchProps = Omit<
  Props,
  'filterItems' | 'getItemDataQa' | 'vtjReadOnlySearch'
>

export function DbPersonSearch(props: PersonSearchProps) {
  const filterItems = useCallback(
    (_: string, items: readonly PersonSummary[]) => [...items],
    []
  )
  const getItemDataQa = useCallback((p: PersonSummary) => `person-${p.id}`, [])

  return (
    <PersonSearch
      {...props}
      filterItems={filterItems}
      getItemDataQa={getItemDataQa}
      vtjReadOnlySearch={false}
    />
  )
}

export function VtjPersonSearch(props: PersonSearchProps) {
  const filterItems = useCallback(
    (_: string, items: readonly PersonSummary[]) =>
      items.filter((i) => i.socialSecurityNumber),
    []
  )
  const getItemDataQa = useCallback(
    (p: PersonSummary) => `person-${p.socialSecurityNumber ?? 'null'}`,
    []
  )
  return (
    <PersonSearch
      {...props}
      filterItems={filterItems}
      getItemDataQa={getItemDataQa}
      vtjReadOnlySearch={true}
    />
  )
}
