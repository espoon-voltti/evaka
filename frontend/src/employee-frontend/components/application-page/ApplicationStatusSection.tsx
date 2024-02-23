// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { ApplicationDetails } from 'lib-common/generated/api-types/application'
import { Decision } from 'lib-common/generated/api-types/decision'
import ListGrid from 'lib-components/layout/ListGrid'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { Label } from 'lib-components/typography'
import { faFile } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

interface Props {
  application: ApplicationDetails
  decisions?: Decision[]
  dueDateEditor?: React.JSX.Element
}

export default React.memo(function ApplicationStatusSection({
  application,
  decisions,
  dueDateEditor
}: Props) {
  const { i18n } = useTranslation()

  return (
    <CollapsibleSection title={i18n.application.state.title} icon={faFile}>
      <ListGrid>
        <Label>{i18n.application.state.status}</Label>
        <span data-qa="application-status">
          {i18n.application.statuses[application.status]}
          {decisions?.find((d) => d.resolvedByName !== null) ? ', ' : ''}
          {decisions?.map((d) => d.resolvedByName).join(', ')}
        </span>

        <Label>{i18n.application.state.origin}</Label>
        <span data-qa="application-origin">
          {i18n.application.origins[application.origin]}
        </span>

        <Label>{i18n.application.state.sent}</Label>
        <span data-qa="application-sent-date">
          {application.sentDate?.format() ?? ''}
        </span>

        <Label>{i18n.application.state.modified}</Label>
        <span data-qa="application-modified-date">
          {application.modifiedDate?.format() ?? ''}
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
