// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'

import LabelValueList from '../../components/common/LabelValueList'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { faUserFriends } from 'lib-icons'
import { useTranslation } from '../../state/i18n'
import LocalDate from 'lib-common/local-date'

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

    return (
      <CollapsibleSection
        title={i18n.invoice.form.headOfFamily.title}
        icon={faUserFriends}
        startCollapsed={false}
      >
        <LabelValueList
          spacing="small"
          contents={[
            {
              label: i18n.invoice.form.headOfFamily.fullName,
              value: (
                <Link to={`/profile/${id}`} data-qa="invoice-details-status">
                  {fullName}
                </Link>
              )
            },
            {
              label: i18n.invoice.form.headOfFamily.ssn,
              value: ssn || dateOfBirth.format()
            }
          ]}
        />
      </CollapsibleSection>
    )
  }
)

export default InvoiceHeadOfFamilySection
