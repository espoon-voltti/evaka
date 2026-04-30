// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { Language } from 'lib-common/generated/api-types/daycare'

import { SelectionChip } from '../atoms/Chip'
import { FixedSpaceColumn, FixedSpaceRow } from '../layout/flex-helpers'
import { Label } from '../typography'

interface Props {
  availableLanguages: Set<Language>
  selected: Language[]
  onChange: (next: Language[]) => void
  getLabel: (lang: Language) => string
  label: string
  labelId: string
  /** Used as the group's data-qa and as the prefix for each chip's data-qa (`${dataQa}-${lang}`). */
  dataQa: string
}

export default React.memo(function LanguageFilterChips({
  availableLanguages,
  selected,
  onChange,
  getLabel,
  label,
  labelId,
  dataQa
}: Props) {
  if (availableLanguages.size < 2) return null

  return (
    <FixedSpaceColumn
      $spacing="xs"
      role="group"
      aria-labelledby={labelId}
      data-qa={dataQa}
    >
      <Label id={labelId}>{label}</Label>
      <FixedSpaceRow>
        {(['fi', 'sv', 'en'] as const)
          .filter((lang) => availableLanguages.has(lang))
          .map((lang) => (
            <SelectionChip
              key={lang}
              data-qa={`${dataQa}-${lang}`}
              text={getLabel(lang)}
              selected={selected.includes(lang)}
              onChange={(s) =>
                onChange(
                  s ? [...selected, lang] : selected.filter((l) => l !== lang)
                )
              }
            />
          ))}
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
})
