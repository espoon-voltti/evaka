// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { ContentArea } from 'lib-components/layout/Container'
import { fontWeights, H1, H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { useTranslation } from '../../../state/i18n'
import { VasuStateChip } from '../../common/VasuStateChip'
import { VasuDocument } from 'lib-common/generated/api-types/vasu'
import { vasuTranslations } from 'lib-customizations/employee'

const HeaderSection = styled(ContentArea)`
  display: flex;
  justify-content: space-between;
  padding: ${defaultMargins.L};
`
const Titles = styled.div`
  ${H1} {
    margin: 0;
  }
  ${H2} {
    margin: 8px 0 0 0;
  }
`
const StateAndConfidentiality = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  text-align: end;
`
const Confidential = styled.div`
  margin-top: ${defaultMargins.xs};
  font-weight: ${fontWeights.semibold};
`

interface Props {
  document: Pick<
    VasuDocument,
    'basics' | 'documentState' | 'templateName' | 'language'
  >
}
export function VasuHeader({
  document: {
    basics: {
      child: { firstName, lastName }
    },
    documentState,
    templateName,
    language
  }
}: Props) {
  const { i18n } = useTranslation()
  const translations = vasuTranslations[language]

  return (
    <HeaderSection opaque>
      <Titles>
        <H1>{templateName}</H1>
        <H2>
          {firstName} {lastName}
        </H2>
      </Titles>
      <StateAndConfidentiality>
        <VasuStateChip state={documentState} labels={i18n.vasu.states} />
        <Confidential>{translations.confidential}</Confidential>
        <div>{translations.confidentialSectionOfLaw}</div>
      </StateAndConfidentiality>
    </HeaderSection>
  )
}
