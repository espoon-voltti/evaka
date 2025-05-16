// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router'

import type {
  PersonDetailed,
  VoucherValueDecisionPlacementDetailed,
  VoucherValueDecisionServiceNeed
} from 'lib-common/generated/api-types/invoicing'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { faUserFriends } from 'lib-icons'

import LabelValueList from '../../components/common/LabelValueList'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'

interface Props {
  child: PersonDetailed
  placement: VoucherValueDecisionPlacementDetailed
  serviceNeed: VoucherValueDecisionServiceNeed
}

export default React.memo(function ChildSection({
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
            label: i18n.valueDecision.child.postOffice,
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
