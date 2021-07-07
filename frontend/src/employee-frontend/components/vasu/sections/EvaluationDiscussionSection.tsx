// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction } from 'react'
import { ContentArea } from '../../../../lib-components/layout/Container'
import { H2, H3, Label } from '../../../../lib-components/typography'
import { EvaluationDiscussionContent } from '../api'
import TextArea from '../../../../lib-components/atoms/form/TextArea'
import { Gap } from 'lib-components/white-space'
import { DatePickerClearableDeprecated } from '../../../../lib-components/molecules/DatePickerDeprecated'
import { useTranslation } from '../../../state/i18n'
import { OrNoRecord } from '../components/OrNoRecord'

interface Props {
  sectionIndex: number
  content: EvaluationDiscussionContent
  setContent: Dispatch<SetStateAction<EvaluationDiscussionContent>>
}
export function EditableEvaluationDiscussionSection({
  sectionIndex,
  content,
  setContent
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.vasu.staticSections.evaluationDiscussion
  return (
    <ContentArea opaque>
      <H2>
        {sectionIndex + 1}. {t.title}
      </H2>

      <Label>
        {sectionIndex + 1}.1 {t.evaluation}
      </Label>
      <TextArea
        value={content.evaluation}
        onChange={(e) =>
          setContent((prev) => ({
            ...prev,
            evaluation: e.target.value
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
        onChange={(e) =>
          setContent((prev) => ({
            ...prev,
            participants: e.target.value
          }))
        }
      />

      <Gap />

      <Label>
        {sectionIndex + 1}.4 {t.guardianViewsAndCollaboration}
      </Label>
      <TextArea
        value={content.guardianViewsAndCollaboration}
        onChange={(e) =>
          setContent((prev) => ({
            ...prev,
            guardianViewsAndCollaboration: e.target.value
          }))
        }
      />
    </ContentArea>
  )
}

export function EvaluationDiscussionSection({
  sectionIndex,
  content
}: Pick<Props, 'sectionIndex' | 'content'>) {
  const { i18n } = useTranslation()
  const t = i18n.vasu.staticSections.evaluationDiscussion
  return (
    <ContentArea opaque>
      <H2>
        {sectionIndex + 1}. {t.title}
      </H2>

      <Label>
        {sectionIndex + 1}.1 {t.evaluation}
      </Label>
      <div>
        <OrNoRecord>{content.evaluation}</OrNoRecord>
      </div>

      <H3>{t.title2}</H3>

      <Label>
        {sectionIndex + 1}.2 {t.discussionDate}
      </Label>
      <div>
        <OrNoRecord>{content.discussionDate?.format()}</OrNoRecord>
      </div>

      <Gap />

      <Label>
        {sectionIndex + 1}.3 {t.participants}
      </Label>
      <div>
        <OrNoRecord>{content.participants}</OrNoRecord>
      </div>

      <Gap />

      <Label>
        {sectionIndex + 1}.4 {t.guardianViewsAndCollaboration}
      </Label>
      <div>
        <OrNoRecord>{content.guardianViewsAndCollaboration}</OrNoRecord>
      </div>
    </ContentArea>
  )
}
