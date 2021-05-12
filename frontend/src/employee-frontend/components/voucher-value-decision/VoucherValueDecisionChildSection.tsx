// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faUserFriends } from 'lib-icons'
import React from 'react'
import { Link } from 'react-router-dom'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import LabelValueList from '../../components/common/LabelValueList'
import { useTranslation } from '../../state/i18n'
import {
  PersonDetailed,
  VoucherValueDecisionPlacement,
  VoucherValueDecisionServiceNeed
} from '../../types/invoicing'
import { formatName } from '../../utils'

interface Props {
  child: PersonDetailed
  placement: VoucherValueDecisionPlacement
  serviceNeed: VoucherValueDecisionServiceNeed
}

const ChildSection = React.memo(function ChildSection({
  child,
  placement,
  serviceNeed
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
            label: i18n.valueDecision.child.name,
            value: (
              <Link to={`/child-information/${child.id}`} data-qa="child-name">
                {formatName(child.firstName, child.lastName, i18n)}
              </Link>
            )
          },
          {
            label: i18n.valueDecision.child.ssn,
            value: child.ssn
          },
          {
            label: i18n.valueDecision.child.city,
            value: child.postOffice
          },
          {
            label: i18n.valueDecision.child.placementType,
            value: i18n.placement.type[placement.type]
          },
          {
            label: i18n.valueDecision.child.careArea,
            value: placement.unit.areaName
          },
          {
            label: i18n.valueDecision.child.unit,
            value: placement.unit.name
          },
          {
            label: i18n.valueDecision.child.serviceNeed,
            value: serviceNeed.feeDescriptionFi
          }
        ]}
      />
    </CollapsibleSection>
  )
})

export default ChildSection
