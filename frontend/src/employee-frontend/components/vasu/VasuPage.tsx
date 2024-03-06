// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import useRouteParams from 'lib-common/useRouteParams'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { VasuStateTransitionButtons } from './VasuStateTransitionButtons'
import { VasuContainer } from './components/VasuContainer'
import { BasicsSection } from './sections/BasicsSection'
import { DynamicSections } from './sections/DynamicSections'
import { VasuEvents } from './sections/VasuEvents'
import { VasuHeader } from './sections/VasuHeader'
import { useVasu } from './use-vasu'

const FooterContainer = styled.div`
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: ${defaultMargins.s};
`

export default React.memo(function VasuPage() {
  const { id } = useRouteParams(['id'])
  const [searchParams] = useSearchParams()

  const { vasu, content, permittedActions, translations } = useVasu(id)

  const dynamicSectionsOffset = content.hasDynamicFirstSection ? 0 : 1

  return (
    <>
      <VasuContainer gapSize="zero" data-qa="vasu-preview">
        {vasu && (
          <>
            <VasuHeader document={vasu} />
            {!content.hasDynamicFirstSection && (
              <BasicsSection
                sectionIndex={0}
                type={vasu.type}
                basics={vasu.basics}
                childLanguage={vasu.basics.childLanguage}
                templateRange={vasu.templateRange}
                translations={translations}
              />
            )}
            <DynamicSections
              sections={content.sections}
              sectionIndex={dynamicSectionsOffset}
              state={vasu.documentState}
              translations={translations}
              vasu={vasu}
            />
            <Gap size="s" />
            <VasuEvents
              document={vasu}
              content={content}
              translations={translations}
            />
          </>
        )}
      </VasuContainer>
      <StickyFooter>
        <FooterContainer>
          {vasu && (
            <VasuStateTransitionButtons
              childId={vasu.basics.child.id}
              childIdFromUrl={searchParams.get('childId')}
              documentId={vasu.id}
              permittedActions={permittedActions}
              state={vasu.documentState}
            />
          )}
        </FooterContainer>
      </StickyFooter>
    </>
  )
})
