// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import { isLoading } from 'lib-common/api'
import type { IncomeEffect } from 'lib-common/generated/api-types/invoicing'
import type {
  FamilyOverview,
  FamilyOverviewIncome,
  FamilyOverviewPerson
} from 'lib-common/generated/api-types/pis'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { formatCents } from 'lib-common/money'
import { useQueryResult } from 'lib-common/query'
import { getAge } from 'lib-common/utils/local-date'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import type { Translations } from '../../state/i18n'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'
import LabelValueList from '../common/LabelValueList'

import { familyByPersonQuery } from './queries'

type FamilyOverviewPersonRole = 'HEAD' | 'PARTNER' | 'CHILD'

interface FamilyOverviewRow {
  personId: string
  name: string
  role: FamilyOverviewPersonRole
  age: number
  restrictedDetailsEnabled: boolean
  address: string
  income: FamilyOverviewIncome | null
}

interface Props {
  id: PersonId
}

const LabelValueListContainer = styled.div`
  margin: 1rem 0;
`

function mapPersonToRow(
  {
    personId,
    firstName,
    lastName,
    income,
    dateOfBirth,
    restrictedDetailsEnabled,
    streetAddress,
    postalCode,
    postOffice
  }: FamilyOverviewPerson,
  role: FamilyOverviewPersonRole,
  i18n: Translations
): FamilyOverviewRow {
  const age = getAge(dateOfBirth)
  return {
    personId,
    name: formatName(firstName, lastName, i18n, true),
    role,
    age,
    restrictedDetailsEnabled,
    address: `${streetAddress} ${postalCode} ${postOffice}`,
    income
  }
}

function getMembers(
  family: FamilyOverview,
  i18n: Translations
): FamilyOverviewRow[] {
  const { headOfFamily, partner, children } = family
  return [
    mapPersonToRow(headOfFamily, 'HEAD', i18n),
    partner && mapPersonToRow(partner, 'PARTNER', i18n),
    ...children.map((item) => mapPersonToRow(item, 'CHILD', i18n))
  ].filter((row): row is FamilyOverviewRow => row !== null)
}

function getAdults(family: FamilyOverview): FamilyOverviewPerson[] {
  const { headOfFamily, partner } = family
  return partner ? [headOfFamily, partner] : [headOfFamily]
}

export default React.memo(function FamilyOverview({ id }: Props) {
  const { i18n } = useTranslation()

  const family = useQueryResult(familyByPersonQuery({ id }))

  function getIncomeString(
    incomeTotal: number | undefined,
    incomeEffect: IncomeEffect | undefined
  ): string {
    if (incomeEffect === 'INCOME') {
      const formattedTotal = formatCents(incomeTotal)
      return formattedTotal
        ? i18n.personProfile.familyOverview.incomeValue(formattedTotal)
        : ''
    }

    if (incomeEffect !== undefined) {
      return i18n.personProfile.income.details.effectOptions[incomeEffect]
    }

    return i18n.personProfile.familyOverview.incomeMissingCompletely
  }

  const familyIncomeTotal = family
    .map(({ totalIncome }) => formatCents(totalIncome?.total ?? undefined))
    .getOrElse(undefined)

  return (
    <div data-qa="family-overview-section" data-isloading={isLoading(family)}>
      {renderResult(family, (family) => (
        <>
          <LabelValueListContainer>
            <LabelValueList
              spacing="small"
              contents={[
                {
                  label: i18n.personProfile.familyOverview.familySizeLabel,
                  value: i18n.personProfile.familyOverview.familySizeValue(
                    getAdults(family).length,
                    family.children.length
                  )
                },
                ...(familyIncomeTotal !== undefined
                  ? [
                      {
                        label:
                          i18n.personProfile.familyOverview.incomeTotalLabel,
                        value:
                          i18n.personProfile.familyOverview.incomeValue(
                            familyIncomeTotal
                          )
                      }
                    ]
                  : [])
              ]}
            />
          </LabelValueListContainer>
          <div>
            <Table>
              <Thead>
                <Tr>
                  <Th>{i18n.personProfile.familyOverview.colName}</Th>
                  <Th>{i18n.personProfile.familyOverview.colRole}</Th>
                  <Th>{i18n.personProfile.familyOverview.colAge}</Th>
                  {family.totalIncome ? (
                    <Th>{i18n.personProfile.familyOverview.colIncome}</Th>
                  ) : null}
                  <Th>{i18n.personProfile.familyOverview.colAddress}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {getMembers(family, i18n)?.map(
                  ({
                    personId,
                    name,
                    role,
                    age,
                    restrictedDetailsEnabled,
                    address,
                    income
                  }) => (
                    <Tr
                      key={personId}
                      data-qa={`table-family-overview-row-${personId}`}
                    >
                      <Td>
                        {role === 'CHILD' ? (
                          <Link to={`/child-information/${personId}`}>
                            {name}
                          </Link>
                        ) : (
                          <Link to={`/profile/${personId}`}>{name}</Link>
                        )}
                      </Td>
                      <Td>{i18n.personProfile.familyOverview.role[role]}</Td>
                      <Td data-qa="person-age">{age}</Td>
                      {family.totalIncome ? (
                        <Td data-qa="person-income-total">
                          {getIncomeString(
                            income?.total ?? undefined,
                            income?.effect ?? undefined
                          )}
                        </Td>
                      ) : null}
                      <Td>
                        {restrictedDetailsEnabled
                          ? i18n.personProfile.restrictedDetails
                          : address}
                      </Td>
                    </Tr>
                  )
                )}
              </Tbody>
            </Table>
          </div>
        </>
      ))}
    </div>
  )
})
