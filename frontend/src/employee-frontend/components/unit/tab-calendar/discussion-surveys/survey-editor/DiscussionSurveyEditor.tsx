// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { UUID } from 'lib-common/types'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'

import DiscussionTimesForm from './DiscussionTimesForm'

export const WidthLimiter = styled.div`
  max-width: 400px;
`
export type DiscussionSurveyEditMode = 'create' | 'edit'

export default React.memo(function DiscussionSurveyEditor({
  unitId,
  groupId,
  eventData
}: {
  unitId: UUID
  groupId: UUID
  eventData: CalendarEvent | null
}) {
  const { i18n } = useTranslation()
  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <DiscussionTimesForm
          eventData={eventData}
          unitId={unitId}
          groupId={groupId}
          editMode="edit"
        />
      </ContentArea>
    </Container>
  )
})

export const CreateDiscussionSurveyEditor = React.memo(
  function CreateDiscussionSurveyEditor({
    unitId,
    groupId
  }: {
    unitId: UUID
    groupId: UUID
  }) {
    const { i18n } = useTranslation()
    return (
      <Container>
        <ReturnButton label={i18n.common.goBack} />
        <ContentArea opaque>
          <DiscussionTimesForm
            eventData={null}
            unitId={unitId}
            groupId={groupId}
            editMode="create"
          />
        </ContentArea>
      </Container>
    )
  }
)
