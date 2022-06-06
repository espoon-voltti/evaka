// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'

import {
  ChildVasuSummary,
  VasuDocumentSummary
} from 'lib-common/generated/api-types/vasu'
import LocalDate from 'lib-common/local-date'
import { useApiState } from 'lib-common/utils/useRestApi'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import {
  CollapsibleContentArea,
  Container,
  ContentArea
} from 'lib-components/layout/Container'
import { H2, H3 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faExclamation } from 'lib-icons'

import { renderResult } from '../async-rendering'
import { useUser } from '../auth/state'
import { useTranslation } from '../localization'

import { Desktop, Mobile, PaddedDiv } from './components'
import { getGuardianChildVasuSummaries } from './vasu/api'
import { VasuStateChip } from './vasu/components/VasuStateChip'

const VasuTableContainer = styled.table`
  width: 100%;
  padding-left 16px;
`

const VasuTr = styled.tr``

const VasuTd = styled.td`
  padding-bottom: 16px;
  padding-right: 16px;
`

const StateTd = styled(VasuTd)``
const PermissionToShareText = styled.span`
  padding-left: 6px;
`

const PermissionToShare = React.memo(function PermissionToShare() {
  const i18n = useTranslation()
  const theme = useTheme()

  return (
    <>
      <RoundIcon
        content={faExclamation}
        color={theme.colors.status.warning}
        size="s"
        data-qa="attention-indicator"
      />
      <PermissionToShareText>
        {i18n.vasu.givePermissionToShareReminder}
      </PermissionToShareText>
    </>
  )
})

const VasuTable = React.memo(function VasuTable({
  summary
}: {
  summary: ChildVasuSummary
}) {
  const navigate = useNavigate()
  const i18n = useTranslation()
  const user = useUser()

  return (
    <>
      <VasuTableContainer key={summary.child.id}>
        <tbody>
          {summary.vasuDocumentsSummary.map((vasu) => (
            <VasuTr key={vasu.id} data-qa={`vasu-${vasu.id}`}>
              <VasuTd>
                <InlineButton
                  onClick={() => navigate(`/vasu/${vasu.id}`)}
                  text={`${vasu.name}`}
                  data-qa="vasu-link"
                />
              </VasuTd>
              <StateTd data-qa={`state-chip-${vasu.id}`}>
                <VasuStateChip
                  state={vasu.documentState}
                  labels={i18n.vasu.states}
                />
              </StateTd>
              <VasuTd data-qa={`published-at-${vasu.id}`}>
                {vasu.publishedAt
                  ? LocalDate.fromSystemTzDate(vasu.publishedAt).format()
                  : ''}
              </VasuTd>
              <VasuTd data-qa={`permission-to-share-needed-${vasu.id}`}>
                {!vasu.guardiansThatHaveGivenPermissionToShare.some(
                  (guardianId) => guardianId === user?.id
                ) && <PermissionToShare />}
              </VasuTd>
            </VasuTr>
          ))}
        </tbody>
      </VasuTableContainer>
    </>
  )
})

const MobileRowContainer = styled.div<{ isLast: boolean }>`
  border-bottom: 1px solid ${colors.grayscale.g15};
  padding: ${defaultMargins.s};
  border-bottom: ${(p) =>
    p.isLast ? 'none' : `1px solid ${colors.grayscale.g15}`};
`
const MobileStatusRow = styled.div``

const MobilePermissionToShareContainer = styled.div`
  padding-top: 16px;
  display: flex;
  flex-direction: row;
`

const VasuChildContainer = styled.div<{ isLast: boolean }>`
  border-bottom: ${(p) =>
    p.isLast ? 'none' : `1px solid ${colors.grayscale.g15}`};
`

const VasuList = React.memo(function VasuList({
  items
}: {
  items: ChildVasuSummary[]
}) {
  const i18n = useTranslation()
  const navigate = useNavigate()
  const user = useUser()

  const itemsWithDocs = React.useMemo(
    () =>
      items ? items.filter((item) => item.vasuDocumentsSummary.length) : [],
    [items]
  )

  const isLastChildOfTheList = (
    childId: string,
    allChildren: ChildVasuSummary[]
  ) => {
    return childId === allChildren[allChildren.length - 1].child.id
  }

  const isLastDocumentOfTheList = (
    documentId: string,
    allDocuments: VasuDocumentSummary[]
  ) => {
    return documentId === allDocuments[allDocuments.length - 1].id
  }

  return (
    <>
      {itemsWithDocs.map((summary) => (
        <VasuChildContainer
          key={summary.child.id}
          data-qa="vasu-child-container"
          isLast={isLastChildOfTheList(summary.child.id, itemsWithDocs)}
        >
          <Mobile>
            <PaddedDiv>
              <H3>{`${summary.child.firstName} ${summary.child.lastName}`}</H3>
            </PaddedDiv>
            {summary.vasuDocumentsSummary.map((vasu) => (
              <MobileRowContainer
                key={vasu.id}
                isLast={isLastDocumentOfTheList(
                  vasu.id,
                  summary.vasuDocumentsSummary
                )}
              >
                <MobileStatusRow>
                  <VasuStateChip
                    state={vasu.documentState}
                    labels={i18n.vasu.states}
                  />
                  <Gap horizontal size="xs" />
                  <span data-qa={`published-at-${vasu.id}`}>
                    {vasu.publishedAt
                      ? LocalDate.fromSystemTzDate(vasu.publishedAt).format()
                      : ''}
                  </span>
                </MobileStatusRow>
                <Gap size="xs" />
                <InlineButton
                  onClick={() => navigate(`/vasu/${vasu.id}`)}
                  text={`${vasu.name}`}
                  data-qa="vasu-link"
                />
                {!vasu.guardiansThatHaveGivenPermissionToShare.some(
                  (guardianId) => guardianId === user?.id
                ) && (
                  <MobilePermissionToShareContainer>
                    <PermissionToShare />
                  </MobilePermissionToShareContainer>
                )}
              </MobileRowContainer>
            ))}
          </Mobile>
          <Desktop>
            <H3>{`${summary.child.firstName} ${summary.child.lastName}`}</H3>
            <VasuTable summary={summary} />
          </Desktop>
        </VasuChildContainer>
      ))}
    </>
  )
})

const VasuDisplay = React.memo(function VasuDisplay({
  items
}: {
  items: ChildVasuSummary[]
}) {
  const i18n = useTranslation()
  const [open, setOpen] = useState(true)

  return (
    <>
      <Mobile>
        <CollapsibleContentArea
          title={
            <PaddedDiv>
              <H2 noMargin>{i18n.vasu.title}</H2>
            </PaddedDiv>
          }
          open={open}
          toggleOpen={() => setOpen(!open)}
          opaque
          paddingVertical="16px 0px 0px 0px;"
          paddingHorizontal="zero"
          data-qa="vasu-and-leops-collapsible"
        >
          <ContentArea opaque paddingVertical="s" paddingHorizontal="zero">
            <VasuList items={items} />
          </ContentArea>
        </CollapsibleContentArea>
      </Mobile>

      <Desktop>
        <CollapsibleContentArea
          title={<H2 noMargin>{i18n.vasu.title}</H2>}
          open={open}
          toggleOpen={() => setOpen(!open)}
          opaque
          paddingVertical="L"
          data-qa="vasu-and-leops-collapsible"
        >
          <ContentArea opaque paddingVertical="s" paddingHorizontal="zero">
            <VasuList items={items} />
          </ContentArea>
        </CollapsibleContentArea>
      </Desktop>
    </>
  )
})

export default React.memo(function CitizenVasuAndLeops() {
  const [vasus] = useApiState(() => getGuardianChildVasuSummaries(), [])
  return (
    <>
      <Container>
        {renderResult(vasus, (vasus) => (
          <VasuDisplay items={vasus} />
        ))}
      </Container>
    </>
  )
})
