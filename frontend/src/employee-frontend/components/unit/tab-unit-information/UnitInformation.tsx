// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Action } from 'lib-common/generated/action'
import { Daycare } from 'lib-common/generated/api-types/daycare'
import Title from 'lib-components/atoms/Title'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'
import { DataList } from '../../common/DataList'

const DetailsLink = styled(Link)`
  text-transform: uppercase;
`

interface Props {
  unit: Daycare
  permittedActions: Action.Unit[]
}

function UnitInformation({ unit, permittedActions }: Props) {
  const { i18n } = useTranslation()

  const renderDaycareManager = () => {
    const { unitManager } = unit

    return (
      <DataList>
        <div>
          <Label>{i18n.unit.manager.name}</Label>
          <span data-qa="unit-manager-name">{unitManager?.name}</span>
        </div>
        <div>
          <Label>{i18n.unit.manager.email}</Label>
          <span data-qa="unit-manager-email">{unitManager?.email}</span>
        </div>
        <div>
          <Label>{i18n.unit.manager.phone}</Label>
          <span data-qa="unit-manager-phone">{unitManager?.phone}</span>
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
          <span data-qa="unit-visiting-address">{streetAddress}</span>
        </div>
        <div>
          <label>{i18n.unit.info.mailingAddress}</label>
          <span data-qa="unit-mailing-address">{mailingAddress}</span>
        </div>
        <div>
          <label>{i18n.unit.info.phone}</label>
          <span data-qa="unit-phone">{unit.phone}</span>
        </div>
      </DataList>

      <div className="separator-gap-small" />

      <Title size={4}>{i18n.unit.manager.title}</Title>
      {renderDaycareManager()}

      {permittedActions.includes('UPDATE') && (
        <>
          <Gap size="L" />
          <DetailsLink
            to={`/units/${unit.id}/details`}
            data-qa="unit-details-link"
          >
            {i18n.unit.openDetails}
          </DetailsLink>
        </>
      )}
    </div>
  )
}

export default UnitInformation
