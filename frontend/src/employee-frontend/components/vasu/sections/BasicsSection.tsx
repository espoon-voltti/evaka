// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Gap } from 'lib-components/white-space'
import { ContentArea } from 'lib-components/layout/Container'
import { H2, Label } from 'lib-components/typography'
import { VasuBasics } from 'lib-common/generated/api-types/vasu'
import FiniteDateRange from 'lib-common/finite-date-range'
import { VasuTranslations } from 'lib-customizations/employee'

interface Props {
  sectionIndex: number
  content: VasuBasics
  templateRange: FiniteDateRange
  translations: VasuTranslations
}

export function BasicsSection({
  sectionIndex,
  content,
  templateRange,
  translations
}: Props) {
  const t = translations.staticSections.basics

  return (
    <ContentArea opaque paddingVertical="L" paddingHorizontal="L">
      <H2 noMargin>
        {sectionIndex + 1}. {t.title}
      </H2>

      <Gap size="m" />

      <Label>{t.name}</Label>
      <div>
        {content.child.firstName} {content.child.lastName}
      </div>

      <Gap size="s" />

      <Label>{t.dateOfBirth}</Label>
      <div>{content.child.dateOfBirth.format()}</div>

      <Gap size="s" />

      <Label>{t.placements}</Label>
      {content.placements?.map((p) => (
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
