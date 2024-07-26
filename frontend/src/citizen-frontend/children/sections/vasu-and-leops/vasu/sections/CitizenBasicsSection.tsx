// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch } from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import {
  ChildLanguage,
  CurriculumType,
  VasuBasics
} from 'lib-common/generated/api-types/vasu'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { VasuTranslations } from 'lib-customizations/employee'

import { ValueOrNoRecord } from '../components/ValueOrNoRecord'

interface Props {
  sectionIndex: number
  type: CurriculumType
  basics: VasuBasics
  childLanguage: ChildLanguage | null
  setChildLanguage?: Dispatch<ChildLanguage>
  templateRange: FiniteDateRange
  translations: VasuTranslations
}

export function CitizenBasicsSection({
  sectionIndex,
  type,
  basics,
  childLanguage,
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
      <div translate="no">
        {basics.child.firstName} {basics.child.lastName}
      </div>

      <Gap size="s" />

      <Label>{t.dateOfBirth}</Label>
      <div>{basics.child.dateOfBirth.format()}</div>

      <Gap size="s" />

      <Label>{t.placements[type]}</Label>
      {basics.placements?.map((p) => (
        <div key={p.range.start.formatIso()} translate="no">
          {p.unitName} ({p.groupName}) {p.range.start.format()} -{' '}
          {p.range.end.isAfter(templateRange.end) ? '' : p.range.end.format()}
        </div>
      ))}

      <Gap size="s" />

      <Label>{t.guardians}</Label>
      {basics.guardians.map((g) => (
        <div key={g.id} translate="no">
          {g.firstName} {g.lastName}
        </div>
      ))}

      {childLanguage && (
        <>
          <Gap size="s" />
          <Label>
            {`${sectionIndex + 1}.1 ${
              translations.staticSections.basics.childLanguage.label
            }`}
          </Label>
          <Gap size="xs" />
          <ChildLang
            sectionIndex={sectionIndex}
            childLanguage={childLanguage}
            translations={translations}
          />
        </>
      )}
    </ContentArea>
  )
}

interface ChildLanguageProps {
  sectionIndex: number
  childLanguage: ChildLanguage
  translations: VasuTranslations
}

const ChildLang = React.memo(function ChildLanguage({
  childLanguage,
  translations
}: ChildLanguageProps) {
  return (
    <FixedSpaceRow>
      <div>
        <Label>
          {translations.staticSections.basics.childLanguage.nativeLanguage}
        </Label>
        <ValueOrNoRecord
          text={childLanguage.nativeLanguage}
          translations={translations}
        />
      </div>
      <div>
        <Label>
          {
            translations.staticSections.basics.childLanguage
              .languageSpokenAtHome
          }
        </Label>
        <ValueOrNoRecord
          text={childLanguage.languageSpokenAtHome}
          translations={translations}
        />
      </div>
    </FixedSpaceRow>
  )
})
