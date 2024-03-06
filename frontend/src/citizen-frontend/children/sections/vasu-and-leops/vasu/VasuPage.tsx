// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faChevronDown, faChevronUp } from 'Icons'
import React, { useState } from 'react'
import styled from 'styled-components'

import { useBoolean } from 'lib-common/form/hooks'
import { useQueryResult } from 'lib-common/query'
import useRequiredParams from 'lib-common/useRequiredParams'
import Main from 'lib-components/atoms/Main'
import DownloadButton from 'lib-components/atoms/buttons/DownloadButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { desktopMin, tabletMin } from 'lib-components/breakpoints'
import Container, { ContentArea } from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { vasuTranslations } from 'lib-customizations/employee'

import { UnwrapResult } from '../../../../async-rendering'
import { useTranslation } from '../../../../localization'
import {
  vasuDocumentQuery,
  givePermissionToShareVasuMutation
} from '../queries'

import { VasuContainer } from './components/VasuContainer'
import { CitizenBasicsSection } from './sections/CitizenBasicsSection'
import { CitizenDynamicSections } from './sections/CitizenDynamicSections'
import { CitizenVasuEvents } from './sections/CitizenVasuEvents'
import { CitizenVasuHeader } from './sections/CitizenVasuHeader'

const TopButtonRow = styled(FixedSpaceRow)`
  @media (max-width: ${tabletMin}) {
    margin-right: ${defaultMargins.s};
  }
`

export default React.memo(function VasuPage() {
  const { id } = useRequiredParams('id')
  const t = useTranslation()

  const [permissionExpanded, { toggle: togglePermissionExpanded }] =
    useBoolean(true)
  const [givePermissionToShareSelected, setGivePermissionToShareSelected] =
    useState<boolean>(false)
  const vasuDocument = useQueryResult(vasuDocumentQuery(id))

  return (
    <UnwrapResult result={vasuDocument}>
      {({
        vasu: { content, ...vasu },
        permissionToShareRequired,
        guardianHasGivenPermissionToShare
      }) => {
        const translations =
          vasu !== undefined
            ? vasuTranslations[vasu.language]
            : vasuTranslations.FI
        const dynamicSectionsOffset =
          content?.hasDynamicFirstSection ?? false ? 0 : 1
        return (
          <>
            <VasuContainer gapSize="zero" data-qa="vasu-preview">
              {vasu && (
                <>
                  <TopButtonRow justifyContent="space-between">
                    <ReturnButton label={t.common.return} />
                    <DownloadButton label={t.common.download} />
                  </TopButtonRow>
                  <Main>
                    <CitizenVasuHeader document={vasu} />
                    {!content.hasDynamicFirstSection && (
                      <CitizenBasicsSection
                        sectionIndex={0}
                        type={vasu.type}
                        basics={vasu.basics}
                        childLanguage={vasu.basics.childLanguage}
                        templateRange={vasu.templateRange}
                        translations={translations}
                      />
                    )}
                    <CitizenDynamicSections
                      sections={content.sections}
                      sectionIndex={dynamicSectionsOffset}
                      state={vasu.documentState}
                      translations={translations}
                      vasu={vasu}
                    />
                    <Gap size="s" />
                    <CitizenVasuEvents document={vasu} content={content} />
                  </Main>
                </>
              )}
            </VasuContainer>
            {vasu &&
              permissionToShareRequired &&
              !guardianHasGivenPermissionToShare && (
                <PermissionWrapper>
                  <Container>
                    <ContentArea
                      opaque
                      paddingVertical="m"
                      paddingHorizontal="L"
                    >
                      <FixedSpaceRow alignItems="flex-start">
                        <Label>
                          {vasu.type === 'DAYCARE'
                            ? t.children.vasu.givePermissionToShareTitleVasu
                            : t.children.vasu.givePermissionToShareTitleLeops}
                        </Label>
                        <InlineButton
                          onClick={togglePermissionExpanded}
                          text={
                            permissionExpanded ? t.common.show : t.common.hide
                          }
                          icon={
                            permissionExpanded ? faChevronDown : faChevronUp
                          }
                        />
                      </FixedSpaceRow>
                      {permissionExpanded && (
                        <>
                          <Gap size="s" />
                          <ExpandingInfo
                            info={
                              <div>
                                {vasu.type === 'DAYCARE'
                                  ? `${t.children.vasu.givePermissionToShareInfoBase} ${t.children.vasu.sharingVasuDisclaimer}`
                                  : `${t.children.vasu.givePermissionToShareInfoBase} ${t.children.vasu.sharingLeopsDisclaimer}`}
                              </div>
                            }
                            inlineChildren
                          >
                            <span>
                              {vasu.type === 'DAYCARE'
                                ? t.children.vasu.givePermissionToShareVasuBrief
                                : t.children.vasu
                                    .givePermissionToShareLeopsBrief}
                            </span>
                          </ExpandingInfo>
                          <Gap size="s" />
                          <Checkbox
                            checked={givePermissionToShareSelected}
                            label={
                              vasu.type === 'DAYCARE'
                                ? t.children.vasu.givePermissionToShareVasu
                                : t.children.vasu.givePermissionToShareLeops
                            }
                            onChange={(value) => {
                              setGivePermissionToShareSelected(value)
                            }}
                            data-qa="confirm-checkbox"
                          />
                          <Gap />
                          <MutateButton
                            primary
                            text={t.common.confirm}
                            disabled={!givePermissionToShareSelected}
                            mutation={givePermissionToShareVasuMutation}
                            onClick={() => id}
                            data-qa="confirm-button"
                          />
                        </>
                      )}
                    </ContentArea>
                  </Container>
                </PermissionWrapper>
              )}
          </>
        )
      }}
    </UnwrapResult>
  )
})

const PermissionWrapper = styled(StickyFooter)`
  @media screen and (max-width: ${desktopMin}) {
    bottom: 60px;
  }
`
