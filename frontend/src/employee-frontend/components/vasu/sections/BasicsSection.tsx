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
import InputField from 'lib-components/atoms/form/InputField'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { VasuTranslations } from 'lib-customizations/employee'

import QuestionInfo from '../QuestionInfo'
import StaticInfoSubsection from '../components/StaticInfoSubsection'
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

export function BasicsSection({
  sectionIndex,
  type,
  basics,
  childLanguage,
  setChildLanguage,
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

      <StaticInfoSubsection
        type={type}
        basics={basics}
        templateRange={templateRange}
        translations={translations}
      />

      {childLanguage && (
        <>
          <Gap size="s" />
          <QuestionInfo
            info={translations.staticSections.basics.childLanguage.info}
          >
            <Label>
              {`${sectionIndex + 1}.1 ${
                translations.staticSections.basics.childLanguage.label
              }`}
            </Label>
          </QuestionInfo>
          <Gap size="xs" />
          {setChildLanguage ? (
            <EditableChildLanguage
              sectionIndex={sectionIndex}
              childLanguage={childLanguage}
              translations={translations}
              setChildLanguage={setChildLanguage}
            />
          ) : (
            <ChildLanguage
              sectionIndex={sectionIndex}
              childLanguage={childLanguage}
              translations={translations}
            />
          )}
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

const ChildLanguage = React.memo(function ChildLanguage({
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

const EditableChildLanguage = React.memo(function EditableChildLanguage({
  childLanguage,
  translations,
  setChildLanguage
}: ChildLanguageProps & { setChildLanguage: Dispatch<ChildLanguage> }) {
  return (
    <FixedSpaceRow>
      <FixedSpaceColumn spacing="xxs">
        <Label>
          {translations.staticSections.basics.childLanguage.nativeLanguage}
        </Label>
        <InputField
          value={childLanguage.nativeLanguage}
          onChange={(nativeLanguage) =>
            setChildLanguage({ ...childLanguage, nativeLanguage })
          }
          width="m"
        />
      </FixedSpaceColumn>
      <FixedSpaceColumn spacing="xxs">
        <Label>
          {
            translations.staticSections.basics.childLanguage
              .languageSpokenAtHome
          }
        </Label>
        <InputField
          value={childLanguage.languageSpokenAtHome}
          onChange={(languageSpokenAtHome) =>
            setChildLanguage({ ...childLanguage, languageSpokenAtHome })
          }
          width="m"
        />
      </FixedSpaceColumn>
    </FixedSpaceRow>
  )
})
