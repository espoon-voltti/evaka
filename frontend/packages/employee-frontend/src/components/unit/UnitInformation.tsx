// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { Title } from '~components/shared/alpha'
import { useTranslation } from '~state/i18n'
import { Stats, Unit } from '~types/unit'
import { DataList } from 'components/common/DataList'
import { RequireRole } from '~utils/roles'

const DetailsLink = styled(Link)`
  text-transform: uppercase;
`

interface Props {
  unit: Unit
  caretakers: Stats
}

function UnitInformation({ unit, caretakers }: Props) {
  const { i18n } = useTranslation()

  const renderCaretakers = () => {
    const formatNumber = (num: number) =>
      parseFloat(num.toFixed(2)).toLocaleString()

    const min = formatNumber(caretakers.minimum)
    const max = formatNumber(caretakers.maximum)

    return min === max ? (
      <span>
        {min} {i18n.unit.info.caretakers.unitOfValue}
      </span>
    ) : (
      <span>{`${min} - ${max} ${i18n.unit.info.caretakers.unitOfValue}`}</span>
    )
  }

  const renderDaycareManager = () => {
    const { unitManager } = unit

    return (
      <DataList>
        <div>
          <label>{i18n.unit.manager.name}</label>
          <span data-qa={'unit-manager-name'}>{unitManager?.name}</span>
        </div>
        <div>
          <label>{i18n.unit.manager.email}</label>
          <span data-qa={'unit-manager-email'}>{unitManager?.email}</span>
        </div>
        <div>
          <label>{i18n.unit.manager.phone}</label>
          <span data-qa={'unit-manager-phone'}>{unitManager?.phone}</span>
        </div>
      </DataList>
    )
  }

  const streetAddress = [
    unit.visitingAddress.streetAddress,
    [unit.visitingAddress.postalCode, unit.visitingAddress.postOffice].join(' ')
  ].join(', ')

  const mailingAddress = [
    unit.mailingAddress.streetAddress,
    unit.mailingAddress.poBox,
    [unit.mailingAddress.postalCode, unit.mailingAddress.postOffice].join(' ')
  ]
    .filter(Boolean)
    .join(', ')

  return (
    <div>
      <Title size={4}>{i18n.unit.info.title}</Title>
      <DataList>
        <div>
          <label>{i18n.unit.info.area}</label>
          <span data-qa="unit-area">{unit.area.name}</span>
        </div>
        <div>
          <label>{i18n.unit.info.visitingAddress}</label>
          <span data-qa={'unit-visiting-address'}>{streetAddress}</span>
        </div>
        <div>
          <label>{i18n.unit.info.mailingAddress}</label>
          <span data-qa={'unit-mailing-address'}>{mailingAddress}</span>
        </div>
        <div>
          <label>{i18n.unit.info.phone}</label>
          <span data-qa={'unit-phone'}>{unit.phone}</span>
        </div>
        <div>
          <label>{i18n.unit.info.caretakers.titleLabel}</label>
          <span data-qa={'unit-total-caretaker-count'}>
            {renderCaretakers()}
          </span>
        </div>
      </DataList>

      <div className="separator-gap-small" />

      <Title size={4}>{i18n.unit.manager.title}</Title>
      {renderDaycareManager()}

      <RequireRole oneOf={['ADMIN']}>
        <div className="separator-gap-small" />
        <DetailsLink to={`/units/${unit.id}/details`}>
          {i18n.unit.openDetails}
        </DetailsLink>
      </RequireRole>
    </div>
  )
}

export default UnitInformation
