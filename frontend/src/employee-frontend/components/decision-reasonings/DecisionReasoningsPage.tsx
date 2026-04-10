// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import { useTheme } from 'styled-components'

import type { DecisionReasoningCollectionType } from 'lib-common/generated/api-types/decision'
import { useQueryResult } from 'lib-common/query'
import { StaticChip } from 'lib-components/atoms/Chip'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Tabs } from 'lib-components/molecules/Tabs'
import { H1 } from 'lib-components/typography'
import { Gap, defaultMargins } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import GenericReasoningsSection from './GenericReasoningsSection'
import IndividualReasoningsSection from './IndividualReasoningsSection'
import { genericReasoningsQuery, individualReasoningsQuery } from './queries'

const ChipRow = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${defaultMargins.xs};
`

export default React.memo(function DecisionReasoningsPage() {
  const { i18n } = useTranslation()
  const t = i18n.decisionReasonings
  const theme = useTheme()

  const [collectionType, setCollectionType] =
    useState<DecisionReasoningCollectionType>('DAYCARE')

  const tabs = [
    {
      id: 'DAYCARE',
      label: t.tabs.DAYCARE,
      onClick: () => setCollectionType('DAYCARE')
    },
    {
      id: 'PRESCHOOL',
      label: t.tabs.PRESCHOOL,
      onClick: () => setCollectionType('PRESCHOOL')
    }
  ]

  const genericResult = useQueryResult(
    genericReasoningsQuery({ collectionType })
  )
  const individualResult = useQueryResult(
    individualReasoningsQuery({ collectionType })
  )

  return (
    <Container>
      <FixedSpaceColumn>
        <ContentArea $opaque>
          <H1 $noMargin>{i18n.titles.decisionReasonings}</H1>
        </ContentArea>

        <div>
          <Tabs active={collectionType} tabs={tabs} />
        </div>

        <ContentArea $opaque>
          <p>{t.collectionInfo[collectionType]}</p>
          <Gap $size="xxs" />
          <ChipRow>
            {t.placementTypes[collectionType].map((label) => (
              <StaticChip
                key={label}
                $color={theme.colors.grayscale.g4}
                $fitContent
              >
                {label}
              </StaticChip>
            ))}
          </ChipRow>
        </ContentArea>

        <ContentArea $opaque>
          {renderResult(genericResult, (reasonings) => (
            <GenericReasoningsSection
              collectionType={collectionType}
              reasonings={reasonings}
            />
          ))}
        </ContentArea>

        <ContentArea $opaque>
          {renderResult(individualResult, (reasonings) => (
            <IndividualReasoningsSection
              collectionType={collectionType}
              reasonings={reasonings}
            />
          ))}
        </ContentArea>
      </FixedSpaceColumn>
    </Container>
  )
})
