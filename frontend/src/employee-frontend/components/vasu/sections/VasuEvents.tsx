// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled from 'styled-components'

import { VasuDocument } from 'lib-common/generated/api-types/vasu'
import LocalDate from 'lib-common/local-date'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { Dimmed, H2, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { VasuTranslations } from 'lib-customizations/employee'

import { VasuStateChip } from '../../common/VasuStateChip'
import { isDateQuestion } from '../vasu-content'
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

function EventRow({
  label,
  date,
  translations
}: {
  label: string
  date: LocalDate | null
  translations: VasuTranslations
}) {
  return (
    <>
      <Label>{label}</Label>
      {date ? (
        <span>{date.format()}</span>
      ) : (
        <Dimmed>{translations.noRecord}</Dimmed>
      )}
    </>
  )
}

interface Props {
  document: Pick<
    VasuDocument,
    'documentState' | 'events' | 'modifiedAt' | 'type'
  >
  content: VasuDocument['content']
  translations: VasuTranslations
}

export function VasuEvents({
  document: { documentState, events, modifiedAt, type },
  content,
  translations
}: Props) {
  const lastPublished = getLastPublished(events)

  const trackedDates: [string, LocalDate][] = useMemo(
    () =>
      content.sections.flatMap((section) =>
        section.questions
          .filter(isDateQuestion)
          .filter((question) => question.trackedInEvents)
          .map(({ name, nameInEvents, value }): [string, LocalDate | null] => [
            nameInEvents || name,
            value
          ])
          .filter((pair): pair is [string, LocalDate] => pair[1] !== null)
      ),
    [content]
  )

  return (
    <Container opaque data-qa="vasu-event-list">
      <H2>{translations.events[type]}</H2>
      <ListGrid labelWidth={labelWidth}>
        <Label>{translations.state}</Label>
        <ChipContainer>
          <VasuStateChip state={documentState} labels={translations.states} />
        </ChipContainer>
        <EventRow
          label={translations.lastModified}
          date={modifiedAt.toLocalDate()}
          translations={translations}
        />
        <EventRow
          label={translations.lastPublished}
          date={lastPublished?.toLocalDate() ?? null}
          translations={translations}
        />
        {trackedDates.map(([label, date]) => (
          <EventRow
            key={label}
            label={label}
            date={date}
            translations={translations}
          />
        ))}
      </ListGrid>
      {events.length > 0 && (
        <>
          <HorizontalLine slim />
          <ListGrid labelWidth={labelWidth}>
            {events.map(({ id, eventType, created }) => (
              <EventRow
                key={id}
                label={translations.eventTypes[eventType]}
                date={created.toLocalDate()}
                translations={translations}
              />
            ))}
          </ListGrid>
        </>
      )}
    </Container>
  )
}
