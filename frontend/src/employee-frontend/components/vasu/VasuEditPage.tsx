// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { formatTime } from 'lib-common/date'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Button from 'lib-components/atoms/buttons/Button'
import Spinner from 'lib-components/atoms/state/Spinner'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import FullWidthDiv from 'lib-components/layout/FullWidthDiv'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { Dimmed } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

import { LeaveVasuPageButton } from './components/LeaveVasuPageButton'
import { VasuContainer } from './components/VasuContainer'
import { BasicsSection } from './sections/BasicsSection'
import { DynamicSections } from './sections/DynamicSections'
import { VasuEvents } from './sections/VasuEvents'
import { VasuHeader } from './sections/VasuHeader'
import { useVasu, VasuStatus } from './use-vasu'

const FooterContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: ${defaultMargins.s};
`

const StatusContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;

  ${Spinner} {
    margin: ${defaultMargins.xs};
    height: ${defaultMargins.s};
    width: ${defaultMargins.s};
  }
`

export default React.memo(function VasuEditPage() {
  const { id } = useNonNullableParams<{ id: UUID }>()
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const {
    vasu,
    content,
    setContent,
    childLanguage,
    setChildLanguage,
    status,
    translations,
    editFollowupEntry,
    permittedFollowupActions
  } = useVasu(id)

  function formatVasuStatus(status: VasuStatus): string | null {
    switch (status.state) {
      case 'loading-error':
        return i18n.common.error.unknown
      case 'save-error':
        return i18n.common.error.saveFailed
      case 'loading':
      case 'loading-dirty':
      case 'saving':
      case 'saving-dirty':
      case 'dirty':
      case 'clean':
        return status.savedAt
          ? `${i18n.common.saved} ${formatTime(status.savedAt)}`
          : null
    }
  }

  const textualVasuStatus = formatVasuStatus(status)
  const showSpinner = status.state === 'saving'

  const dynamicSectionsOffset = 1

  return (
    <VasuContainer
      gapSize="s"
      data-qa="vasu-container"
      data-status={status.state}
    >
      {vasu && (
        <>
          <VasuHeader document={vasu} />
          <BasicsSection
            sectionIndex={0}
            type={vasu.type}
            basics={vasu.basics}
            childLanguage={childLanguage}
            setChildLanguage={setChildLanguage}
            templateRange={vasu.templateRange}
            translations={translations}
          />
          <DynamicSections
            sectionIndex={dynamicSectionsOffset}
            sections={content.sections}
            setContent={setContent}
            editFollowupEntry={(entry) =>
              editFollowupEntry({
                documentId: vasu.id,
                entryId: entry.id,
                text: entry.text
              })
            }
            state={vasu.documentState}
            permittedFollowupActions={permittedFollowupActions}
            translations={translations}
          />
          <VasuEvents document={vasu} content={content} />
        </>
      )}
      <StickyFooter>
        <FooterContainer>
          <StatusContainer>
            <Dimmed>{textualVasuStatus}</Dimmed>
            {showSpinner && <Spinner />}
          </StatusContainer>
          {vasu && (
            <FullWidthDiv>
              <ButtonContainer>
                <Button
                  text={i18n.vasu.checkInPreview}
                  disabled={status.state != 'clean'}
                  onClick={() => navigate(`/vasu/${vasu.id}`)}
                  data-qa="vasu-preview-btn"
                  primary
                />
                <LeaveVasuPageButton
                  disabled={status.state != 'clean'}
                  childId={vasu.basics.child.id}
                />
              </ButtonContainer>
            </FullWidthDiv>
          )}
        </FooterContainer>
      </StickyFooter>
    </VasuContainer>
  )
})
