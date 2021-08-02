// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { RouteComponentProps } from 'react-router-dom'
import styled from 'styled-components'
import { UUID } from 'lib-common/types'
import 'lib-components/layout/ButtonContainer'
import { Container } from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { AuthorsSection } from './sections/AuthorsSection'
import { DynamicSections } from './sections/DynamicSections'
import { EvaluationDiscussionSection } from './sections/EvaluationDiscussionSection'
import { VasuDiscussionSection } from './sections/VasuDiscussionSection'
import { VasuEvents } from './sections/VasuEvents'
import { VasuHeader } from './sections/VasuHeader'
import { useVasu } from './use-vasu'
import { VasuStateTransitionButtons } from './VasuStateTransitionButtons'

const FooterContainer = styled.div`
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: ${defaultMargins.s};
`

export default React.memo(function VasuPage({
  match
}: RouteComponentProps<{ id: UUID }>) {
  const { id } = match.params

  const {
    vasu,
    content,
    authorsContent,
    vasuDiscussionContent,
    evaluationDiscussionContent
  } = useVasu(id)

  const dynamicSectionsOffset = 1

  return (
    <Container>
      <Gap size={'L'} />
      {vasu && (
        <>
          <VasuHeader document={vasu} />
          <AuthorsSection sectionIndex={0} content={authorsContent} />
          <DynamicSections
            sections={content.sections}
            sectionIndex={dynamicSectionsOffset}
          />
          <VasuDiscussionSection
            sectionIndex={content.sections.length + dynamicSectionsOffset}
            content={vasuDiscussionContent}
          />
          {vasu.documentState !== 'DRAFT' && (
            <EvaluationDiscussionSection
              sectionIndex={content.sections.length + dynamicSectionsOffset + 1}
              content={evaluationDiscussionContent}
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
          {vasu && (
            <VasuStateTransitionButtons
              childId={vasu.child.id}
              documentId={vasu.id}
              state={vasu.documentState}
            />
          )}
        </FooterContainer>
      </StickyFooter>
    </Container>
  )
})
