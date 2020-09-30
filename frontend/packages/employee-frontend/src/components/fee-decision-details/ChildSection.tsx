// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faUserFriends } from 'icon-set'
import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Collapsible,
  LabelValueList,
  LabelValueListItem
} from '~components/shared/alpha'
import { formatName } from '~utils'
import { useTranslation } from '../../state/i18n'
import { PersonDetailed, Placement, UnitDetailed } from '../../types/invoicing'

interface Props {
  child: PersonDetailed
  placement: Placement
  placementUnit: UnitDetailed
}

const ChildSection = React.memo(function ChildSection({
  child,
  placement,
  placementUnit
}: Props) {
  const { i18n } = useTranslation()
  const [toggled, setToggled] = useState(true)

  if (!child || !placement) {
    return null
  }

  return (
    <Collapsible
      title={formatName(child.firstName, child.lastName, i18n)}
      icon={faUserFriends}
      open={toggled}
      onToggle={() => setToggled((prev) => !prev)}
    >
      <LabelValueList>
        <LabelValueListItem
          label={i18n.feeDecision.form.child.name}
          value={
            <Link to={`/child-information/${child.id}`}>
              {formatName(child.firstName, child.lastName, i18n)}
            </Link>
          }
          dataQa="child-name"
        />
        <LabelValueListItem
          label={i18n.feeDecision.form.child.ssn}
          value={child.ssn}
          dataQa="child-ssn"
        />
        <LabelValueListItem
          label={i18n.feeDecision.form.child.city}
          value={child.postOffice}
          dataQa="child-city"
        />
        <LabelValueListItem
          label={i18n.feeDecision.form.child.placementType}
          value={i18n.placement.type[placement.type]}
          dataQa="child-service-need-type"
        />
        <LabelValueListItem
          label={i18n.feeDecision.form.child.careArea}
          value={placementUnit.areaName}
          dataQa="child-care-area"
        />
        <LabelValueListItem
          label={i18n.feeDecision.form.child.daycare}
          value={placementUnit.name}
          dataQa="child-daycare"
        />
        <LabelValueListItem
          label={i18n.feeDecision.form.child.serviceNeed}
          value={i18n.feeDecision.form.summary.part.serviceNeedAmount(
            placement.type,
            placement.serviceNeed
          )}
          dataQa="child-service-need"
        />
      </LabelValueList>
    </Collapsible>
  )
})

export default ChildSection
