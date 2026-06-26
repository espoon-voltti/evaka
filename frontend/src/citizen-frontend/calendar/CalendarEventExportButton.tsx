// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { InlineExternalLinkButton } from 'lib-components/atoms/buttons/InlineLinkButton'
import { faCalendar } from 'lib-icons'

import { useTranslation } from '../localization'

interface CalendarEventExportProps {
  // URL of a backend endpoint that returns the event as a `text/calendar` (.ics)
  // attachment. A real anchor navigation is used instead of a client-side `data:`
  // URI download, because iOS Safari ignores the `download` attribute on data URIs
  // and would show the raw .ics content instead of offering to add it to the calendar.
  href: string
  'data-qa': string
}

export const CalendarEventExportButton = React.memo(
  function CalendarEventExportButton({
    href,
    'data-qa': dataQa
  }: CalendarEventExportProps) {
    const i18n = useTranslation()
    return (
      <InlineExternalLinkButton
        data-qa={dataQa}
        href={href}
        text={i18n.calendar.discussionTimeReservation.calendarExportButtonLabel}
        icon={faCalendar}
      />
    )
  }
)
