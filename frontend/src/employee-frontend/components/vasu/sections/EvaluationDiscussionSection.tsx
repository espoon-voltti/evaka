// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction } from 'react'
import styled from 'styled-components'
import { ContentArea } from 'lib-components/layout/Container'
import { H2, H3, Label } from 'lib-components/typography'
import { EvaluationDiscussionContent } from '../api'
import TextArea from 'lib-components/atoms/form/TextArea'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { DatePickerClearableDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { ReadOnlyValue } from '../components/ReadOnlyValue'
import { VasuTranslations } from 'lib-customizations/employee'
import { blueColors } from 'lib-customizations/common'

interface Props {
  sectionIndex: number
  content: EvaluationDiscussionContent
  setContent: Dispatch<SetStateAction<EvaluationDiscussionContent>>
  translations: VasuTranslations
}

export function EditableEvaluationDiscussionSection({
  sectionIndex,
  content,
  setContent,
  translations
}: Props) {
  const t = translations.staticSections.evaluationDiscussion
  return (
    <HighlightedContentArea opaque>
      <H2>
        {sectionIndex + 1}. {t.title}
      </H2>

      <Label>
        {sectionIndex + 1}.1 {t.evaluation}
      </Label>
      <TextArea
        value={content.evaluation}
        onChange={(value) =>
          setContent((prev) => ({
            ...prev,
            evaluation: value
          }))
        }
      />

      <H3>{t.title2}</H3>

      <Label>
        {sectionIndex + 1}.2 {t.discussionDate}
      </Label>
      <DatePickerClearableDeprecated
        date={content.discussionDate ?? undefined}
        onChange={(date) =>
          setContent((prev) => ({
            ...prev,
            discussionDate: date
          }))
        }
        onCleared={() =>
          setContent((prev) => ({
            ...prev,
            discussionDate: null
          }))
        }
        type="short"
      />

      <Gap />

      <Label>
        {sectionIndex + 1}.3 {t.participants}
      </Label>
      <TextArea
        value={content.participants}
        onChange={(value) =>
          setContent((prev) => ({
            ...prev,
            participants: value
          }))
        }
      />

      <Gap />

      <Label>
        {sectionIndex + 1}.4 {t.guardianViewsAndCollaboration}
      </Label>
      <TextArea
        value={content.guardianViewsAndCollaboration}
        onChange={(value) =>
          setContent((prev) => ({
            ...prev,
            guardianViewsAndCollaboration: value
          }))
        }
      />
    </HighlightedContentArea>
  )
}

const HighlightedContentArea = styled(ContentArea)`
  border-left: 5px solid ${blueColors.medium};
  box-shadow: 0px -4px 4px rgba(15, 15, 15, 0.1);
  padding: ${defaultMargins.L};
`

export function EvaluationDiscussionSection({
  sectionIndex,
  content,
  translations
}: Pick<Props, 'sectionIndex' | 'content' | 'translations'>) {
  const t = translations.staticSections.evaluationDiscussion
  return (
    <ContentArea opaque>
      <H2>
        {sectionIndex + 1}. {t.title}
      </H2>

      <ReadOnlyValue
        label={`${sectionIndex + 1}.1 ${t.evaluation}`}
        value={content.evaluation}
        translations={translations}
      />

      <H3>{t.title2}</H3>

      <ReadOnlyValue
        label={`${sectionIndex + 1}.2 ${t.discussionDate}`}
        value={content.discussionDate?.format()}
        translations={translations}
      />

      <Gap />

      <ReadOnlyValue
        label={`${sectionIndex + 1}.3 ${t.participants}`}
        value={content.participants}
        translations={translations}
      />

      <Gap />

      <ReadOnlyValue
        label={`${sectionIndex + 1}.4 ${t.guardianViewsAndCollaboration}`}
        value={content.guardianViewsAndCollaboration}
        translations={translations}
      />
    </ContentArea>
  )
}
