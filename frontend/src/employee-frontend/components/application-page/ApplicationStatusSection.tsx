// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { faFile } from 'lib-icons'
import { Label } from 'lib-components/typography'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import ListGrid from 'lib-components/layout/ListGrid'
import { useTranslation } from '../../state/i18n'
import { DATE_FORMAT_DATE_TIME, formatDate } from 'lib-common/date'
import { ApplicationDetails } from 'lib-common/api-types/application/ApplicationDetails'

interface Props {
  application: ApplicationDetails
  dueDateEditor?: JSX.Element
}

export default React.memo(function ApplicationStatusSection({
  application,
  dueDateEditor
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
            ? formatDate(application.modifiedDate, DATE_FORMAT_DATE_TIME)
            : ''}
        </span>

        <Label inputRow={!!dueDateEditor}>{i18n.application.state.due}</Label>
        {dueDateEditor || (
          <span data-qa="application-due-date">
            {application.dueDate?.format()}
          </span>
        )}
      </ListGrid>
    </CollapsibleSection>
  )
})
