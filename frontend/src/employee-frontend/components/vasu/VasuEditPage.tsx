// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Button from 'lib-components/atoms/buttons/Button'
import React from 'react'
import { useHistory } from 'react-router'
import { RouteComponentProps } from 'react-router-dom'
import styled from 'styled-components'
import { DATE_FORMAT_TIME_ONLY, formatDate } from 'lib-common/date'
import { UUID } from 'lib-common/types'
import Spinner from 'lib-components/atoms/state/Spinner'
import FullWidthDiv from 'lib-components/layout/FullWidthDiv'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { Dimmed } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { useTranslation } from '../../state/i18n'
import { EditableAuthorsSection } from './sections/AuthorsSection'
import { DynamicSections } from './sections/DynamicSections'
import { EditableEvaluationDiscussionSection } from './sections/EvaluationDiscussionSection'
import { EditableVasuDiscussionSection } from './sections/VasuDiscussionSection'
import { VasuEvents } from './sections/VasuEvents'
import { VasuHeader } from './sections/VasuHeader'
import { useVasu, VasuStatus } from './use-vasu'
import { BasicsSection } from './sections/BasicsSection'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import { LeaveVasuPageButton } from './components/LeaveVasuPageButton'
import { VasuContainer } from './components/VasuContainer'

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

export default React.memo(function VasuEditPage({
  match
}: RouteComponentProps<{ id: UUID }>) {
  const { id } = match.params
  const { i18n } = useTranslation()
  const history = useHistory()

  const {
    vasu,
    content,
    setContent,
    authorsContent,
    setAuthorsContent,
    vasuDiscussionContent,
    setVasuDiscussionContent,
    evaluationDiscussionContent,
    setEvaluationDiscussionContent,
    status,
    translations,
    editFollowupEntry
  } = useVasu(id)

  function formatVasuStatus(status: VasuStatus): string | null {
    switch (status.state) {
      case 'loading-error':
        return i18n.common.error.unknown
      case 'save-error':
        return i18n.common.error.saveFailed
      case 'loading':
      case 'saving':
      case 'dirty':
      case 'clean':
        return status.savedAt
          ? `${i18n.common.saved} ${formatDate(
              status.savedAt,
              DATE_FORMAT_TIME_ONLY
            )}`
          : null
    }
  }
  const textualVasuStatus = formatVasuStatus(status)
  const showSpinner = status.state === 'saving'

  const dynamicSectionsOffset = 2

  return (
    <VasuContainer gapSize={'s'}>
      <Gap size={'L'} />
      {vasu && (
        <>
          <VasuHeader document={vasu} />
          <BasicsSection
            sectionIndex={0}
            content={vasu.basics}
            templateRange={vasu.templateRange}
            translations={translations}
          />
          <EditableAuthorsSection
            sectionIndex={1}
            content={authorsContent}
            setContent={setAuthorsContent}
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
            documentId={vasu.id}
            translations={translations}
          />
          <EditableVasuDiscussionSection
            sectionIndex={content.sections.length + dynamicSectionsOffset}
            content={vasuDiscussionContent}
            setContent={setVasuDiscussionContent}
            translations={translations}
          />
          {vasu.documentState !== 'DRAFT' && (
            <EditableEvaluationDiscussionSection
              sectionIndex={content.sections.length + dynamicSectionsOffset + 1}
              content={evaluationDiscussionContent}
              setContent={setEvaluationDiscussionContent}
              translations={translations}
            />
          )}
          <VasuEvents
            document={vasu}
            vasuDiscussionDate={vasuDiscussionContent.discussionDate}
            evaluationDiscussionDate={
              evaluationDiscussionContent.discussionDate
            }
          />
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
                  onClick={() => history.push(`/vasu/${vasu.id}`)}
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
