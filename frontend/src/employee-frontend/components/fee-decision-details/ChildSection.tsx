// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faUserFriends } from 'lib-icons'
import React from 'react'
import { Link } from 'react-router-dom'
import { PlacementType } from 'lib-common/api-types/serviceNeed/common'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import LabelValueList from '../../components/common/LabelValueList'
import { useTranslation } from '../../state/i18n'
import { PersonDetailed, UnitDetailed } from '../../types/invoicing'
import { formatName } from '../../utils'

interface Props {
  child: PersonDetailed
  placementType: PlacementType
  placementUnit: UnitDetailed
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
            label: i18n.feeDecision.form.child.city,
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
