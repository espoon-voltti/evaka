// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Gap } from 'lib-components/white-space'
import { ContentArea } from 'lib-components/layout/Container'
import { H2, Label } from 'lib-components/typography'
import { useTranslation } from '../../../state/i18n'
import { VasuBasics } from '../api'
import FiniteDateRange from 'lib-common/finite-date-range'

interface Props {
  sectionIndex: number
  content: VasuBasics
  templateRange: FiniteDateRange
}

export function BasicsSection({ sectionIndex, content, templateRange }: Props) {
  const { i18n } = useTranslation()
  const t = i18n.vasu.staticSections.basics

  return (
    <ContentArea opaque>
      <H2>
        {sectionIndex + 1}. {t.title}
      </H2>

      <Label>{t.name}</Label>
      <div>
        {content.child.firstName} {content.child.lastName}
      </div>

      <Gap size="s" />

      <Label>{t.dateOfBirth}</Label>
      <div>{content.child.dateOfBirth.format()}</div>

      <Gap size="s" />

      <Label>{t.placements}</Label>
      {content.placements.map((p) => (
        <div key={p.range.start.formatIso()}>
          {p.unitName} ({p.groupName}) {p.range.start.format()} -{' '}
          {p.range.end.isAfter(templateRange.end) ? '' : p.range.end.format()}
        </div>
      ))}

      <Gap size="s" />

      <Label>{t.guardians}</Label>
      {content.guardians.map((g) => (
        <div key={g.id}>
          {g.firstName} {g.lastName}
        </div>
      ))}
    </ContentArea>
  )
}
