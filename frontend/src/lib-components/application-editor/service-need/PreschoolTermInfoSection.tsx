// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import type { Term } from 'lib-common/application/validations'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import type { ApplicationEditorDeps } from '../types'

const Ul = styled.ul`
  margin: 0;
`

interface Props {
  deps: ApplicationEditorDeps
  preschoolTerms: Term[] | undefined
}

export function PreschoolTermsInfoSection({ deps, preschoolTerms }: Props) {
  const { translations: i18n } = deps
  return (
    <>
      <Label>
        {
          i18n.applications.editor.serviceNeed.startDate[
            preschoolTerms?.length === 1 ? 'preschoolTerm' : 'preschoolTerms'
          ]
        }
      </Label>
      <Gap $size="s" />
      <Ul data-qa="preschool-terms">
        {preschoolTerms?.map((term, i) => (
          <li key={i}>
            <Label>{`${term.extendedTerm.start.year}-${term.extendedTerm.end.year}`}</Label>
            <p>{`${term.extendedTerm.format()}`}</p>
          </li>
        ))}
      </Ul>
      <Gap $size="m" />
    </>
  )
}
