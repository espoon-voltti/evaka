// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { Paragraph } from 'lib-common/api-types/vasu'
import { VasuSection } from 'lib-common/generated/api-types/vasu'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import InputField from 'lib-components/atoms/form/InputField'
import TextArea from 'lib-components/atoms/form/TextArea'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../state/i18n'

interface Props {
  onSave: (paragraph: Paragraph) => void
  onCancel: () => void
  section: VasuSection
}

export default React.memo(function CreateParagraphModal({
  onCancel,
  onSave,
  section
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.vasuTemplates.questionModal
  const [title, setTitle] = useState('')
  const [paragraph, setParagraph] = useState('')
  const [identifier, setIdentifier] = useState('')
  const [dependsOn, setDependsOn] = useState<string[]>([])

  return (
    <FormModal
      title={t.title}
      resolveAction={() =>
        onSave({
          type: 'PARAGRAPH',
          title,
          paragraph,
          name: '',
          info: '',
          ophKey: null,
          id: identifier || null,
          dependsOn
        })
      }
      resolveLabel={i18n.common.confirm}
      rejectAction={onCancel}
      rejectLabel={i18n.common.cancel}
    >
      <FixedSpaceColumn spacing="xxs">
        <Label>{t.paragraphTitle}</Label>
        <InputField value={title} onChange={setTitle} width="full" />
        <Label>{t.paragraphText}</Label>
        <TextArea value={paragraph} onChange={setParagraph} />
        <Label>{t.id}</Label>
        <InputField value={identifier} onChange={setIdentifier} width="full" />
        <Label>{t.dependsOn}</Label>
        {Array(dependsOn.length + 1)
          .fill({})
          .map((_, i) => (
            <Combobox
              key={i}
              items={section.questions
                .flatMap((q) => q.id)
                .filter((id) => id && !dependsOn.includes(id))}
              selectedItem={dependsOn[i]}
              onChange={(value) => {
                if (value) {
                  const copy = Array.from(dependsOn)
                  copy[i] = value
                  setDependsOn(copy)
                }
              }}
            />
          ))}
      </FixedSpaceColumn>
    </FormModal>
  )
})
