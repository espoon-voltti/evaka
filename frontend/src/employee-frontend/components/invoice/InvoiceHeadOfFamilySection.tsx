// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'

import { InvoiceDetailed } from 'lib-common/generated/api-types/invoicing'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { faUserFriends } from 'lib-icons'

import LabelValueList from '../../components/common/LabelValueList'
import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'

interface Props {
  headOfFamily: InvoiceDetailed['headOfFamily']
  codebtor: InvoiceDetailed['codebtor']
}

export default React.memo(function InvoiceHeadOfFamilySection({
  headOfFamily,
  codebtor
}: Props) {
  const { i18n } = useTranslation()

  return (
    <CollapsibleSection
      title={i18n.invoice.form.headOfFamily.title}
      icon={faUserFriends}
      startCollapsed={false}
      data-qa="head-of-family"
    >
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.invoice.form.headOfFamily.fullName,
            value: (
              <Link
                to={`/profile/${headOfFamily.id}`}
                data-qa="invoice-details-head-of-family"
              >
                {formatName(
                  headOfFamily.firstName,
                  headOfFamily.lastName,
                  i18n
                )}
              </Link>
            ),
            dataQa: 'head-of-family-name'
          },
          {
            label: i18n.invoice.form.headOfFamily.ssn,
            value: headOfFamily.ssn || headOfFamily.dateOfBirth.format(),
            dataQa: 'head-of-family-ssn'
          },
          ...(codebtor
            ? [
                {
                  label: i18n.invoice.form.headOfFamily.codebtorName,
                  value: (
                    <Link to={`/profile/${codebtor.id}`}>
                      {formatName(codebtor.firstName, codebtor.lastName, i18n)}
                    </Link>
                  ),
                  dataQa: 'codebtor-name'
                },
                {
                  label: i18n.invoice.form.headOfFamily.codebtorSsn,
                  value: codebtor.ssn || codebtor.dateOfBirth.format(),
                  dataQa: 'codebtor-ssn'
                }
              ]
            : [])
        ]}
      />
    </CollapsibleSection>
  )
})
