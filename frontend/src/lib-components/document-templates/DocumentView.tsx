// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { BoundForm } from 'lib-common/form/hooks'
import { useFormElems, useFormFields } from 'lib-common/form/hooks'
import type { UiLanguage } from 'lib-common/generated/api-types/shared'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { H2 } from 'lib-components/typography'
import EN from 'lib-customizations/defaults/components/i18n/en'
import FI from 'lib-customizations/defaults/components/i18n/fi'
import SV from 'lib-customizations/defaults/components/i18n/sv'

import type { Translations } from '../i18n'
import { ComponentLocalizationContextProvider } from '../i18n'

import {
  type documentForm,
  DocumentQuestionView,
  type documentSectionForm
} from './documents'

interface Props {
  bind: BoundForm<typeof documentForm>
  readOnly?: boolean
  templateLanguage?: UiLanguage
}

const translationsByLang: Record<UiLanguage, Translations> = {
  FI,
  SV,
  EN
}

export default React.memo(function DocumentView({
  bind,
  readOnly,
  templateLanguage
}: Props) {
  const sectionElems = useFormElems(bind)
  const translations = translationsByLang[templateLanguage ?? 'FI']

  return (
    <div>
      <ComponentLocalizationContextProvider
        useTranslations={() => translations}
      >
        <FixedSpaceColumn spacing="XL">
          {sectionElems.map((section) => (
            <DocumentSectionView
              key={section.state.id}
              bind={section}
              readOnly={readOnly ?? false}
            />
          ))}
        </FixedSpaceColumn>
      </ComponentLocalizationContextProvider>
    </div>
  )
})

interface DocumentSectionProps {
  bind: BoundForm<typeof documentSectionForm>
  readOnly: boolean
}
export const DocumentSectionView = React.memo(function DocumentSectionView({
  bind,
  readOnly
}: DocumentSectionProps) {
  const { label, questions, infoText } = useFormFields(bind)
  const questionElems = useFormElems(questions)
  return (
    <div data-qa="document-section">
      <ExpandingInfo info={readOnly ? undefined : infoText.state}>
        <H2>{label.value()}</H2>
      </ExpandingInfo>
      <FixedSpaceColumn spacing="L">
        {questionElems.map((question) => (
          <DocumentQuestionView
            bind={question}
            key={question.state.state.template.id}
            readOnly={readOnly}
          />
        ))}
      </FixedSpaceColumn>
    </div>
  )
})
