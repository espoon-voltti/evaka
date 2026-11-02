// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import type {
  PersonSummary,
  VtjPersonPreview
} from 'lib-common/generated/api-types/pis'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { tryFromUuid } from 'lib-common/id-type'
import type LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { getAge } from 'lib-common/utils/local-date'
import { useDebounce } from 'lib-common/utils/useDebounce'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import type { BaseProps } from 'lib-components/utils'

import {
  personBySsnQuery,
  personIdentityQuery,
  searchPersonQuery,
  vtjPersonPreviewQuery
} from '../../queries'
import { useTranslation } from '../../state/i18n'
import { isSsnValid } from '../../utils/validation/validations'

const Container = styled.div`
  margin: 10px 0;
`

interface SearchablePerson {
  dateOfBirth: LocalDate
  firstName: string
  lastName: string
  streetAddress: string | null
}

interface PersonSearchProps extends BaseProps {
  onFocus?: (e: React.FocusEvent<HTMLElement>) => void
  ageLessThan?: number
  ageAtLeast?: number
}

interface Props<T extends SearchablePerson> extends PersonSearchProps {
  results: Result<T[]>
  isLoading: boolean
  onInputChange: (query: string) => void
  isExcluded: (person: T) => boolean
  getItemDataQa: (item: T) => string
  onResult: (result: T | undefined) => void
}

function allItems<T>(_: string, items: readonly T[]): T[] {
  return [...items]
}

function PersonSearch<T extends SearchablePerson>({
  results,
  isLoading,
  onInputChange,
  isExcluded,
  onResult,
  onFocus,
  ageLessThan,
  ageAtLeast,
  'data-qa': dataQa,
  getItemDataQa
}: Props<T>) {
  const { i18n } = useTranslation()
  const [selectedPerson, setSelectedPerson] = useState<T>()

  const options = useMemo(
    () =>
      results
        .map((people) =>
          people.filter((person) => {
            if (isExcluded(person)) return false
            const age = getAge(person.dateOfBirth)
            if (ageLessThan !== undefined && age >= ageLessThan) return false
            if (ageAtLeast !== undefined && age < ageAtLeast) return false
            return true
          })
        )
        .getOrElse([]),
    [results, isExcluded, ageLessThan, ageAtLeast]
  )

  const formatItemLabel = useCallback(
    (person: T) => formatPersonName(person, 'First Last'),
    []
  )

  const formatMenuItemLabel = useCallback(
    ({ dateOfBirth, firstName, lastName, streetAddress }: T): string =>
      `${formatPersonName({ firstName, lastName }, 'First Last')} (${dateOfBirth.format()})${
        streetAddress ? `\n${streetAddress}` : ''
      }`,
    []
  )

  const onChange = useCallback(
    (option: T | null) => {
      const person = option ?? undefined
      setSelectedPerson(person)
      onResult(person)
    },
    [onResult]
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
        onInputChange={onInputChange}
        onChange={onChange}
        isLoading={isLoading}
        menuEmptyLabel={i18n.common.noResults}
        filterItems={allItems}
        onFocus={onFocus}
      />
    </Container>
  )
}

function useDebouncedSearchTerm() {
  const [query, setQuery] = useState('')
  const debouncedQuery = useDebounce(query, 500)
  const isSsn = useMemo(
    () => isSsnValid(debouncedQuery.toUpperCase()),
    [debouncedQuery]
  )
  return { query, setQuery, debouncedQuery, isSsn }
}

export function DbPersonSearch({
  excludePeople,
  onResult,
  ...props
}: PersonSearchProps & {
  excludePeople?: PersonId[]
  onResult: (result: PersonSummary | undefined) => void
}) {
  const { query, setQuery, debouncedQuery, isSsn } = useDebouncedSearchTerm()
  const queryFn = useMemo(() => {
    if (isSsn) {
      return personBySsnQuery({ body: { ssn: debouncedQuery.toUpperCase() } })
    }

    const personId = tryFromUuid<PersonId>(debouncedQuery)
    if (personId !== undefined) {
      return personIdentityQuery({ personId })
    }

    return searchPersonQuery({
      body: {
        searchTerm: debouncedQuery,
        orderBy: 'last_name,first_name',
        sortDirection: 'ASC'
      }
    })
  }, [debouncedQuery, isSsn])
  const results = useQueryResult(queryFn).map((res) =>
    Array.isArray(res) ? res : [res]
  )

  const isExcluded = useCallback(
    (p: PersonSummary) => excludePeople?.includes(p.id) ?? false,
    [excludePeople]
  )
  const getItemDataQa = useCallback((p: PersonSummary) => `person-${p.id}`, [])

  return (
    <PersonSearch<PersonSummary>
      {...props}
      results={results}
      isLoading={results.isLoading || query !== debouncedQuery}
      onInputChange={setQuery}
      isExcluded={isExcluded}
      getItemDataQa={getItemDataQa}
      onResult={onResult}
    />
  )
}

export function VtjPersonSearch({
  excludeSsns,
  onResult,
  ...props
}: PersonSearchProps & {
  excludeSsns?: string[]
  onResult: (result: VtjPersonPreview | undefined) => void
}) {
  const { query, setQuery, debouncedQuery, isSsn } = useDebouncedSearchTerm()
  const results = useQueryResult(
    isSsn
      ? vtjPersonPreviewQuery({ body: { ssn: debouncedQuery.toUpperCase() } })
      : constantQuery(null)
  ).map((res) => (res ? [res] : []))

  const isExcluded = useCallback(
    (p: VtjPersonPreview) =>
      excludeSsns?.includes(p.socialSecurityNumber) ?? false,
    [excludeSsns]
  )
  const getItemDataQa = useCallback(
    (p: VtjPersonPreview) => `person-${p.socialSecurityNumber}`,
    []
  )

  return (
    <PersonSearch<VtjPersonPreview>
      {...props}
      results={results}
      isLoading={results.isLoading || query !== debouncedQuery}
      onInputChange={setQuery}
      isExcluded={isExcluded}
      getItemDataQa={getItemDataQa}
      onResult={onResult}
    />
  )
}
