// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Collapsible,
  LabelValueList,
  LabelValueListItem
} from '~components/shared/alpha'
import { faUserFriends } from 'icon-set'
import { useTranslation } from '../../state/i18n'
import LocalDate from '@evaka/lib-common/src/local-date'

interface Props {
  id: string
  fullName: string
  dateOfBirth: LocalDate
  ssn: string | null
}

const InvoiceHeadOfFamilySection = React.memo(
  function InvoiceHeadOfFamilySection({
    id,
    fullName,
    dateOfBirth,
    ssn
  }: Props) {
    const { i18n } = useTranslation()
    const [toggled, setToggled] = useState(true)

    return (
      <Collapsible
        title={i18n.invoice.form.headOfFamily.title}
        icon={faUserFriends}
        open={toggled}
        onToggle={() => setToggled((prev) => !prev)}
      >
        <LabelValueList>
          <LabelValueListItem
            label={i18n.invoice.form.headOfFamily.fullName}
            value={<Link to={`/profile/${id}`}>{fullName}</Link>}
            dataQa="invoice-details-status"
          />
          <LabelValueListItem
            label={i18n.invoice.form.headOfFamily.ssn}
            value={ssn || dateOfBirth.format()}
            dataQa="invoice-details-range"
          />
        </LabelValueList>
      </Collapsible>
    )
  }
)

export default InvoiceHeadOfFamilySection
