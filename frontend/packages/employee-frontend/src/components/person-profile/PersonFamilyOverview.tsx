// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { faHomeAlt } from '@evaka/icons'
import {
  Collapsible,
  Table,
  Loader,
  LabelValueList,
  LabelValueListItem
} from '~components/shared/alpha'
import { Translations, useTranslation } from '~state/i18n'
import { PersonContext } from '~state/person'
import { isSuccess, isLoading, isFailure } from '~api'
import {
  FamilyOverview,
  FamilyOverviewPerson,
  FamilyOverviewRow,
  FamilyOverviewPersonRole
} from '~types/family-overview'
import { UUID } from '~types'
import { formatCents } from '~utils/money'
import { getAge } from '@evaka/lib-common/src/utils/local-date'
import { formatName } from '~utils'

interface Props {
  id: UUID
  open: boolean
}

const LabelValueListContainer = styled.div`
  margin: 1rem 0;
`

function mapPersonToRow(
  {
    personId,
    firstName,
    lastName,
    incomeTotal,
    incomeEffect,
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
    name: formatName(firstName, lastName, i18n),
    role,
    age,
    incomeTotal,
    incomeEffect,
    restrictedDetailsEnabled,
    address: `${streetAddress} ${postalCode} ${postOffice}`
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
  ].filter(Boolean) as FamilyOverviewRow[]
}

function getAdults(family: FamilyOverview): FamilyOverviewPerson[] {
  const { headOfFamily, partner } = family
  return partner ? [headOfFamily, partner] : [headOfFamily]
}

const FamilyOverview = React.memo(function FamilyOverview({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { family, setFamily, reloadFamily } = useContext(PersonContext)

  function getIncomeString(
    incomeTotal?: number,
    incomeEffect?: string
  ): string {
    if (incomeEffect === 'INCOME') {
      const formattedTotal = formatCents(incomeTotal)
      return formattedTotal
        ? i18n.personProfile.familyOverview.incomeValue(formattedTotal)
        : ''
    }

    if (incomeEffect !== undefined) {
      return String(
        i18n.personProfile.income.details.effectOptions[incomeEffect]
      )
    }

    return i18n.personProfile.familyOverview.incomeMissingCompletely
  }

  useEffect(() => {
    void reloadFamily(id)
  }, [id, setFamily])

  const [toggled, setToggled] = useState(open)
  const toggle = useCallback(() => setToggled((toggled) => !toggled), [
    setToggled
  ])

  return (
    <div>
      {isSuccess(family) && (
        <Collapsible
          icon={faHomeAlt}
          title={i18n.personProfile.familyOverview.title}
          open={toggled}
          onToggle={toggle}
          dataQa="family-overview-collapsible"
        >
          <LabelValueListContainer>
            <LabelValueList>
              <LabelValueListItem
                label={i18n.personProfile.familyOverview.familySizeLabel}
                value={i18n.personProfile.familyOverview.familySizeValue(
                  getAdults(family.data).length,
                  family.data.children.length
                )}
                dataQa="head-of-family"
              />
              <LabelValueListItem
                label={i18n.personProfile.familyOverview.incomeTotalLabel}
                value={getIncomeString(
                  family.data.totalIncome,
                  family.data.totalIncomeEffect
                )}
                dataQa="head-of-family"
              />
            </LabelValueList>
          </LabelValueListContainer>
          <div>
            <Table.Table>
              <Table.Head>
                <Table.Row>
                  <Table.Th>
                    {i18n.personProfile.familyOverview.colName}
                  </Table.Th>
                  <Table.Th>
                    {i18n.personProfile.familyOverview.colRole}
                  </Table.Th>
                  <Table.Th>
                    {i18n.personProfile.familyOverview.colAge}
                  </Table.Th>
                  <Table.Th>
                    {i18n.personProfile.familyOverview.colIncome}
                  </Table.Th>
                  <Table.Th>
                    {i18n.personProfile.familyOverview.colAddress}
                  </Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {getMembers(family.data, i18n)?.map(
                  ({
                    personId,
                    name,
                    role,
                    age,
                    incomeTotal,
                    incomeEffect,
                    restrictedDetailsEnabled,
                    address
                  }) => (
                    <Table.Row
                      key={personId}
                      dataQa={`table-family-overview-row-${personId}`}
                    >
                      <Table.Td>
                        {role === 'CHILD' ? (
                          <Link to={`/child-information/${personId}`}>
                            {name}
                          </Link>
                        ) : (
                          <Link to={`/profile/${personId}`}>{name}</Link>
                        )}
                      </Table.Td>
                      <Table.Td>
                        {i18n.personProfile.familyOverview.role[role]}
                      </Table.Td>
                      <Table.Td dataQa="person-age">{age}</Table.Td>
                      <Table.Td dataQa="person-income-total">
                        {role !== 'CHILD' &&
                          getIncomeString(incomeTotal, incomeEffect)}
                      </Table.Td>
                      <Table.Td>
                        {restrictedDetailsEnabled
                          ? i18n.personProfile.restrictedDetails
                          : address}
                      </Table.Td>
                    </Table.Row>
                  )
                )}
              </Table.Body>
            </Table.Table>
          </div>
        </Collapsible>
      )}
      {isLoading(family) && <Loader />}
      {isFailure(family) && <div>{i18n.common.loadingFailed}</div>}
    </div>
  )
})

export default FamilyOverview
