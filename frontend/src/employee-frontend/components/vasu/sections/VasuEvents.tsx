// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { formatDate } from '../../../../lib-common/date'
import HorizontalLine from '../../../../lib-components/atoms/HorizontalLine'
import { ContentArea } from '../../../../lib-components/layout/Container'
import ListGrid from '../../../../lib-components/layout/ListGrid'
import { Dimmed, H2, Label } from '../../../../lib-components/typography'
import { defaultMargins } from '../../../../lib-components/white-space'
import { useTranslation } from '../../../state/i18n'
import { VasuStateChip } from '../../common/VasuStateChip'
import { VasuDocument } from '../api'
import { getLastPublished } from '../vasu-events'

const labelWidth = '320px'

const Container = styled(ContentArea)`
  padding: ${defaultMargins.L};
  ${H2} {
    margin-top: 0;
  }
`

const ChipContainer = styled.div`
  display: inline-flex;
`

function EventRow({ label, date }: { label: string; date?: Date }) {
  const { i18n } = useTranslation()
  const eventDate = formatDate(date)
  return (
    <>
      <Label>{label}</Label>
      {eventDate.length > 0 ? (
        <span>{eventDate}</span>
      ) : (
        <Dimmed>{i18n.vasu.noRecord}</Dimmed>
      )}
    </>
  )
}

interface Props {
  document: Pick<
    VasuDocument,
    | 'documentState'
    | 'evaluationDiscussionDate'
    | 'events'
    | 'modifiedAt'
    | 'vasuDiscussionDate'
  >
}

export function VasuEvents({
  document: {
    documentState,
    evaluationDiscussionDate,
    events,
    modifiedAt,
    vasuDiscussionDate
  }
}: Props) {
  const { i18n } = useTranslation()
  const lastPublished = getLastPublished(events)

  return (
    <Container opaque>
      <H2>{i18n.vasu.events}</H2>
      <ListGrid labelWidth={labelWidth}>
        <Label>{i18n.vasu.state}</Label>
        <ChipContainer>
          <VasuStateChip state={documentState} labels={i18n.vasu.states} />
        </ChipContainer>
        <EventRow label={i18n.vasu.lastModified} date={modifiedAt} />
        <EventRow label={i18n.vasu.lastPublished} date={lastPublished} />
        <EventRow label={i18n.vasu.vasuDiscussion} date={vasuDiscussionDate} />
        <EventRow
          label={i18n.vasu.evaluationDiscussion}
          date={evaluationDiscussionDate}
        />
      </ListGrid>
      {events.length > 0 && (
        <>
          <HorizontalLine slim />
          <ListGrid labelWidth={labelWidth}>
            {events.map(({ id, eventType, created }) => (
              <EventRow
                key={id}
                label={i18n.vasu.eventTypes[eventType]}
                date={created}
              />
            ))}
          </ListGrid>
        </>
      )}
    </Container>
  )
}
