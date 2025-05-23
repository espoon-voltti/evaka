// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'wouter'

import type {
  PersonDetailed,
  UnitData
} from 'lib-common/generated/api-types/invoicing'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { faUserFriends } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import LabelValueList from '../common/LabelValueList'

interface Props {
  child: PersonDetailed
  placementType: PlacementType
  placementUnit: UnitData
  serviceNeedDescription: string
}

const ChildSection = React.memo(function ChildSection({
  child,
  placementType,
  placementUnit,
  serviceNeedDescription
}: Props) {
  const { i18n } = useTranslation()

  return (
    <CollapsibleSection
      title={formatName(child.firstName, child.lastName, i18n)}
      icon={faUserFriends}
      startCollapsed={false}
    >
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.feeDecision.form.child.name,
            value: (
              <Link to={`/child-information/${child.id}`} data-qa="child-name">
                {formatName(child.firstName, child.lastName, i18n)}
              </Link>
            )
          },
          {
            label: i18n.feeDecision.form.child.ssn,
            value: child.ssn
          },
          {
            label: i18n.feeDecision.form.child.postOffice,
            value: child.postOffice
          },
          {
            label: i18n.feeDecision.form.child.placementType,
            value: i18n.placement.type[placementType]
          },
          {
            label: i18n.feeDecision.form.child.careArea,
            value: placementUnit.areaName
          },
          {
            label: i18n.feeDecision.form.child.daycare,
            value: placementUnit.name
          },
          {
            label: i18n.feeDecision.form.child.serviceNeed,
            value: serviceNeedDescription
          }
        ]}
      />
    </CollapsibleSection>
  )
})

export default ChildSection
