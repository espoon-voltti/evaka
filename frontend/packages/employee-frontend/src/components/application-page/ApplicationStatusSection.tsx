// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { faFile } from '@evaka/lib-icons'
import { Label } from '@evaka/lib-components/src/typography'
import CollapsibleSection from '~components/shared/molecules/CollapsibleSection'
import ListGrid from '~components/shared/layout/ListGrid'
import { useTranslation } from '~state/i18n'
import { ApplicationDetails } from '~types/application'
import { formatDate } from '~utils/date'

type Props = {
  application: ApplicationDetails
}

export default React.memo(function ApplicationStatusSection({
  application
}: Props) {
  const { i18n } = useTranslation()

  return (
    <CollapsibleSection title={i18n.application.state.title} icon={faFile}>
      <ListGrid>
        <Label>{i18n.application.state.status}</Label>
        <span data-qa="application-status">
          {i18n.application.statuses[application.status]}
        </span>

        <Label>{i18n.application.state.origin}</Label>
        <span data-qa="application-origin">
          {i18n.application.origins[application.origin]}
        </span>

        <Label>{i18n.application.state.sent}</Label>
        <span data-qa="application-sent-date">
          {application.sentDate?.format()}
        </span>

        <Label>{i18n.application.state.modified}</Label>
        <span data-qa="application-modified-date">
          {application.modifiedDate
            ? formatDate(application.modifiedDate, 'dd.MM.yyyy HH:mm')
            : ''}
        </span>

        <Label>{i18n.application.state.due}</Label>
        <span data-qa="application-due-date">
          {application.dueDate?.format()}
        </span>
      </ListGrid>
    </CollapsibleSection>
  )
})
