// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Gap } from 'lib-components/white-space'
import React, { Dispatch, SetStateAction } from 'react'
import TextArea from '../../../../lib-components/atoms/form/TextArea'
import { ContentArea } from '../../../../lib-components/layout/Container'
import { DatePickerClearableDeprecated } from '../../../../lib-components/molecules/DatePickerDeprecated'
import { H2, H3, Label } from '../../../../lib-components/typography'
import { useTranslation } from '../../../state/i18n'
import { VasuDiscussionContent } from '../api'
import { OrNoRecord } from '../components/OrNoRecord'

interface Props {
  sectionIndex: number
  content: VasuDiscussionContent
  setContent: Dispatch<SetStateAction<VasuDiscussionContent>>
}
export function EditableVasuDiscussionSection({
  sectionIndex,
  content,
  setContent
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.vasu.staticSections.vasuDiscussion
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
        onChange={(e) =>
          setContent((prev) => ({
            ...prev,
            participants: e.target.value
          }))
        }
      />

      <Gap />

      <Label>
        {sectionIndex + 1}.3 {t.guardianViewsAndCollaboration}
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

export function VasuDiscussionSection({
  sectionIndex,
  content
}: Pick<Props, 'sectionIndex' | 'content'>) {
  const { i18n } = useTranslation()
  const t = i18n.vasu.staticSections.vasuDiscussion
  return (
    <ContentArea opaque>
      <H2>
        {sectionIndex + 1}. {t.title}
      </H2>

      <H3>{t.title2}</H3>

      <Label>
        {sectionIndex + 1}.1 {t.discussionDate}
      </Label>
      <OrNoRecord>{content.discussionDate?.format()}</OrNoRecord>

      <Gap />

      <Label>
        {sectionIndex + 1}.2 {t.participants}
      </Label>
      <OrNoRecord>{content.participants}</OrNoRecord>

      <Gap />

      <Label>
        {sectionIndex + 1}.3 {t.guardianViewsAndCollaboration}
      </Label>
      <OrNoRecord>{content.guardianViewsAndCollaboration}</OrNoRecord>
    </ContentArea>
  )
}
