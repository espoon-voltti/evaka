// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import Title from 'lib-components/atoms/Title'
import { useTranslation } from '../../../state/i18n'
import { Unit } from '../../../types/unit'
import { DataList } from '../../common/DataList'
import { RequireRole } from '../../../utils/roles'
import { Gap } from 'lib-components/white-space'

const DetailsLink = styled(Link)`
  text-transform: uppercase;
`

interface Props {
  unit: Unit
}

function UnitInformation({ unit }: Props) {
  const { i18n } = useTranslation()

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
      </DataList>

      <div className="separator-gap-small" />

      <Title size={4}>{i18n.unit.manager.title}</Title>
      {renderDaycareManager()}

      <RequireRole oneOf={['ADMIN']}>
        <Gap size={'L'} />
        <DetailsLink to={`/units/${unit.id}/details`}>
          {i18n.unit.openDetails}
        </DetailsLink>
      </RequireRole>
    </div>
  )
}

export default UnitInformation
