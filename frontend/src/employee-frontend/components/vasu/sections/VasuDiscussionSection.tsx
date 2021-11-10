// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Gap } from 'lib-components/white-space'
import React, { Dispatch, SetStateAction } from 'react'
import TextArea from 'lib-components/atoms/form/TextArea'
import { ContentArea } from 'lib-components/layout/Container'
import { DatePickerClearableDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { H2, H3, Label } from 'lib-components/typography'
import { VasuDiscussionContent } from '../api'
import { ReadOnlyValue } from '../components/ReadOnlyValue'
import { VasuTranslations } from 'lib-customizations/employee'

interface Props {
  sectionIndex: number
  content: VasuDiscussionContent
  setContent: Dispatch<SetStateAction<VasuDiscussionContent>>
  translations: VasuTranslations
}

export function EditableVasuDiscussionSection({
  sectionIndex,
  content,
  setContent,
  translations
}: Props) {
  const t = translations.staticSections.vasuDiscussion
  return (
    <ContentArea opaque>
      <H2>
        {sectionIndex + 1}. {t.title}
      </H2>

      <H3>{t.title2}</H3>

      <Label>
        {sectionIndex + 1}.1 {t.discussionDate}
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
        {sectionIndex + 1}.2 {t.participants}
      </Label>
      <TextArea
        value={content.participants}
        onChange={(value) =>
          setContent((prev) => ({ ...prev, participants: value }))
        }
      />

      <Gap />

      <Label>
        {sectionIndex + 1}.3 {t.guardianViewsAndCollaboration}
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
    </ContentArea>
  )
}

export function VasuDiscussionSection({
  sectionIndex,
  content,
  translations
}: Pick<Props, 'sectionIndex' | 'content' | 'translations'>) {
  const t = translations.staticSections.vasuDiscussion
  return (
    <ContentArea opaque>
      <H2>
        {sectionIndex + 1}. {t.title}
      </H2>

      <H3>{t.title2}</H3>

      <ReadOnlyValue
        label={`${sectionIndex + 1}.1 ${t.discussionDate}`}
        value={content.discussionDate?.format()}
        translations={translations}
      />

      <Gap />

      <ReadOnlyValue
        label={`${sectionIndex + 1}.2 ${t.participants}`}
        value={content.participants}
        translations={translations}
      />

      <Gap />

      <ReadOnlyValue
        label={`${sectionIndex + 1}.3 ${t.guardianViewsAndCollaboration}`}
        value={content.guardianViewsAndCollaboration}
        translations={translations}
      />
    </ContentArea>
  )
}
