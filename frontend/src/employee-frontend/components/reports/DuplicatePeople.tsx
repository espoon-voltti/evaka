// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import type { DuplicatePeopleReportRow } from 'lib-common/generated/api-types/reports'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import type { Arg0 } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Th, Tr, Td, Thead, Tbody } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { colors } from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import { faQuestion } from 'lib-icons'

import { PROFILE_AGE_THRESHOLD_DEFAULT } from '../../constants'
import type { getDuplicatePeopleReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { renderResult } from '../async-rendering'

import { FilterRow, TableScrollable } from './common'
import {
  duplicatePeopleReportQuery,
  mergePeopleMutation,
  safeDeletePersonMutation
} from './queries'

type DuplicatePeopleFilters = Arg0<typeof getDuplicatePeopleReport>

interface RowProps {
  odd: boolean
}

const StyledRow = styled(Tr)<RowProps>`
  ${(props) => (props.odd ? `background: ${colors.main.m4};` : '')}
`
const NoWrapTd = styled(Td)`
  white-space: nowrap;
`

interface Selection {
  group: number
  row: number
}

const hasReferences = (row: DuplicatePeopleReportRow) =>
  row.referenceCounts.find(({ count }) => count > 0) !== undefined

const isChild = (dateOfBirth: LocalDate) => {
  const age = LocalDate.todayInSystemTz().differenceInYears(dateOfBirth)
  return age < PROFILE_AGE_THRESHOLD_DEFAULT
}

export default React.memo(function DuplicatePeople() {
  const { i18n } = useTranslation()
  const [duplicate, setDuplicate] = useState<Selection | null>(null)
  const [master, setMaster] = useState<Selection | null>(null)
  const [deleteId, setDeleteId] = useState<PersonId | null>(null)
  const { setErrorMessage } = useContext(UIContext)

  const [filters, setFilters] = useState<DuplicatePeopleFilters>({
    showIntentionalDuplicates: !featureFlags.personDuplicate
  })

  const rows = useQueryResult(duplicatePeopleReportQuery(filters))
  const { mutateAsync: safeDeletePerson } = useMutationResult(
    safeDeletePersonMutation
  )
  const { mutateAsync: mergePeople } = useMutationResult(mergePeopleMutation)

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.duplicatePeople.title}</Title>
        {featureFlags.personDuplicate && (
          <FilterRow>
            <Checkbox
              onChange={(checkedValue) =>
                setFilters({
                  ...filters,
                  showIntentionalDuplicates: checkedValue
                })
              }
              label={i18n.reports.common.filters.showIntentionalDuplicates}
              checked={filters.showIntentionalDuplicates ?? false}
            />
          </FilterRow>
        )}
        {renderResult(rows, (rows) => (
          <>
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>Henkilön nimi</Th>
                  <Th>Hetu</Th>
                  <Th>Synt.aika</Th>
                  <Th>Katuosoite</Th>
                  <Th />
                  <Th />

                  {rows.length > 0
                    ? rows[0].referenceCounts
                        .map(({ table, column }) => `${table}.${column}`)
                        .map((key) => (
                          <Th key={key}>
                            {i18n.reports.duplicatePeople.columns[
                              key as keyof typeof i18n.reports.duplicatePeople.columns
                            ] ?? key}
                          </Th>
                        ))
                    : null}
                </Tr>
              </Thead>
              <Tbody>
                {rows.map((row: DuplicatePeopleReportRow) => (
                  <StyledRow key={row.id} odd={row.groupIndex % 2 !== 0}>
                    <NoWrapTd>
                      <Link
                        to={
                          isChild(row.dateOfBirth)
                            ? `/child-information/${row.id}`
                            : `/profile/${row.id}`
                        }
                      >
                        {row.lastName} {row.firstName}
                      </Link>
                    </NoWrapTd>
                    <NoWrapTd>{row.socialSecurityNumber}</NoWrapTd>
                    <NoWrapTd>{row.dateOfBirth.format()}</NoWrapTd>
                    <NoWrapTd>{row.streetAddress}</NoWrapTd>
                    <NoWrapTd>
                      {duplicate ? (
                        duplicate.group === row.groupIndex ? (
                          duplicate.row === row.duplicateNumber ? (
                            <LegacyButton
                              className="inline"
                              onClick={() => setDuplicate(null)}
                              text={i18n.common.cancel}
                            />
                          ) : (
                            <LegacyButton
                              className="inline"
                              onClick={() =>
                                setMaster({
                                  group: row.groupIndex,
                                  row: row.duplicateNumber
                                })
                              }
                              text={i18n.reports.duplicatePeople.moveTo}
                            />
                          )
                        ) : null
                      ) : (
                        <LegacyButton
                          className="inline"
                          disabled={!hasReferences(row)}
                          onClick={() =>
                            setDuplicate({
                              group: row.groupIndex,
                              row: row.duplicateNumber
                            })
                          }
                          text={i18n.reports.duplicatePeople.moveFrom}
                        />
                      )}
                    </NoWrapTd>
                    <NoWrapTd>
                      <LegacyButton
                        className="inline"
                        disabled={duplicate != null || hasReferences(row)}
                        onClick={() => setDeleteId(row.id)}
                        text={i18n.common.remove}
                      />
                    </NoWrapTd>
                    {row.referenceCounts.map(({ table, column, count }) => (
                      <NoWrapTd key={`${table}.${column}`}>{count}</NoWrapTd>
                    ))}
                  </StyledRow>
                ))}
              </Tbody>
            </TableScrollable>
            {master && duplicate && (
              <InfoModal
                type="warning"
                title={i18n.reports.duplicatePeople.confirmMoveTitle}
                icon={faQuestion}
                reject={{
                  action: () => {
                    setMaster(null)
                    setDuplicate(null)
                  },
                  label: i18n.common.cancel
                }}
                resolve={{
                  action: () => {
                    const masterId = rows.find(
                      (row) =>
                        row.groupIndex === master.group &&
                        row.duplicateNumber === master.row
                    )?.id
                    const duplicateId = rows.find(
                      (row) =>
                        row.groupIndex === duplicate.group &&
                        row.duplicateNumber === duplicate.row
                    )?.id

                    setMaster(null)
                    setDuplicate(null)

                    if (masterId && duplicateId) {
                      void mergePeople({
                        body: { master: masterId, duplicate: duplicateId }
                      }).then((res) => {
                        if (res.isFailure) {
                          setErrorMessage({
                            type: 'error',
                            title: i18n.reports.duplicatePeople.errorTitle,
                            text: i18n.reports.duplicatePeople.errorText,
                            resolveLabel: i18n.common.ok
                          })
                        }
                      })
                    }
                  },
                  label: i18n.common.confirm
                }}
              />
            )}
            {!!deleteId && (
              <InfoModal
                type="warning"
                title={i18n.reports.duplicatePeople.confirmDeleteTitle}
                icon={faQuestion}
                reject={{
                  action: () => {
                    setDeleteId(null)
                  },
                  label: i18n.common.cancel
                }}
                resolve={{
                  action: () => {
                    void safeDeletePerson({ personId: deleteId })
                    setDeleteId(null)
                  },
                  label: i18n.common.remove
                }}
              />
            )}
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
