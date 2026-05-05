// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import type { OneOfOption } from 'lib-common/form/form'
import type { BoundForm } from 'lib-common/form/hooks'
import type { DocumentDeletionBasis } from 'lib-common/generated/api-types/document'
import { documentDeletionBases } from 'lib-common/generated/api-types/document'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'
import type {
  deletionRetentionBasisField,
  deletionRetentionDaysField
} from '../forms'

export function useDeletionBasisOptions(): OneOfOption<DocumentDeletionBasis>[] {
  const { i18n } = useTranslation()
  return useMemo(
    () =>
      documentDeletionBases.map((option) => ({
        domValue: option,
        value: option,
        label:
          i18n.documentTemplates.templateModal.deletionRetentionBasis[option]
      })),
    [i18n.documentTemplates]
  )
}

interface Props {
  retentionDays: BoundForm<typeof deletionRetentionDaysField>
  retentionBasis: BoundForm<typeof deletionRetentionBasisField>
}

export default React.memo(function DeletionRulesEditor({
  retentionDays,
  retentionBasis
}: Props) {
  const { i18n } = useTranslation()
  return (
    <>
      <Label>
        {i18n.documentTemplates.templateModal.deletionRetentionDays}
      </Label>
      <InputFieldF
        data-qa="deletion-retention-days"
        bind={retentionDays}
        type="number"
        hideErrorsBeforeTouched
      />
      <Gap $size="xs" />
      <FixedSpaceColumn $spacing="xs">
        {documentDeletionBases.map((option) => (
          <Radio
            key={option}
            data-qa={`deletion-retention-basis-${option}`}
            label={
              i18n.documentTemplates.templateModal.deletionRetentionBasis[
                option
              ]
            }
            checked={retentionBasis.state.domValue === option}
            onChange={() =>
              retentionBasis.update((s) => ({
                ...s,
                domValue: option
              }))
            }
          />
        ))}
      </FixedSpaceColumn>
    </>
  )
})
