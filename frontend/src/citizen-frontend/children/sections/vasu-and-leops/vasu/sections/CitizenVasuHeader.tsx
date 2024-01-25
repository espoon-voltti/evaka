// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { VasuDocument } from 'lib-common/generated/api-types/vasu'
import { tabletMin } from 'lib-components/breakpoints'
import { ContentArea } from 'lib-components/layout/Container'
import { fontWeights, H1, H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { vasuTranslations } from 'lib-customizations/employee'

import { useTranslation } from '../../../../../localization'
import { VasuStateChip } from '../components/VasuStateChip'

const HeaderSection = styled(ContentArea)`
  padding: ${defaultMargins.L};

  @media (min-width: ${tabletMin}) {
    display: flex;
    justify-content: space-between;
  }
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
  @media (min-width: ${tabletMin}) {
    align-items: flex-end;
    text-align: end;
    margin-top: 0;
  }
  align-items: flex-start;
  text-align: start;
  margin-top: ${defaultMargins.m};
`
const Confidential = styled.div`
  margin-top: ${defaultMargins.xs};
  font-weight: ${fontWeights.semibold};
`

interface Props {
  document: Pick<
    VasuDocument,
    'basics' | 'documentState' | 'templateName' | 'language' | 'type'
  >
}
export function CitizenVasuHeader({
  document: {
    basics: {
      child: { firstName, lastName }
    },
    documentState,
    templateName,
    language,
    type
  }
}: Props) {
  const i18n = useTranslation()
  const translations = vasuTranslations[language]

  return (
    <HeaderSection opaque>
      <Titles>
        <H1 data-qa="template-name">{templateName}</H1>
        <H2 data-qa="title-child-name">
          {firstName} {lastName}
        </H2>
      </Titles>
      <StateAndConfidentiality>
        <VasuStateChip
          state={documentState}
          labels={i18n.children.vasu.states}
        />
        <Confidential>{translations.confidential}</Confidential>
        <div>{translations.law[type]}</div>
      </StateAndConfidentiality>
    </HeaderSection>
  )
}
