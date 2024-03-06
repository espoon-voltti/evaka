// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import useRequiredParams from 'lib-common/useRequiredParams'
import Button from 'lib-components/atoms/buttons/Button'
import Spinner from 'lib-components/atoms/state/Spinner'
import StickyFooter, {
  StickyFooterContainer
} from 'lib-components/layout/StickyFooter'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import AutosaveStatusIndicator from '../common/AutosaveStatusIndicator'

import { LeaveVasuPageButton } from './components/LeaveVasuPageButton'
import { VasuContainer } from './components/VasuContainer'
import { BasicsSection } from './sections/BasicsSection'
import { DynamicSections } from './sections/DynamicSections'
import { VasuEvents } from './sections/VasuEvents'
import { VasuHeader } from './sections/VasuHeader'
import { useVasu } from './use-vasu'

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
  const { id } = useRequiredParams('id')
  const [searchParams] = useSearchParams()
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { vasu, content, setContent, status, translations } = useVasu(id)

  const showSpinner = status.state === 'saving'

  const dynamicSectionsOffset = content.hasDynamicFirstSection ? 0 : 1

  const childIdFromUrl = searchParams.get('childId')

  return (
    <>
      <VasuContainer
        gapSize="s"
        data-qa="vasu-container"
        data-status={status.state}
      >
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
              sectionIndex={dynamicSectionsOffset}
              sections={content.sections}
              setContent={setContent}
              state={vasu.documentState}
              translations={translations}
              vasu={vasu}
            />
            <VasuEvents
              document={vasu}
              content={content}
              translations={translations}
            />
          </>
        )}
      </VasuContainer>
      <StickyFooter>
        <StickyFooterContainer>
          {vasu && (
            <FixedSpaceRow justifyContent="space-between" flexWrap="wrap">
              <FixedSpaceRow spacing="s">
                <LeaveVasuPageButton
                  disabled={status.state != 'clean'}
                  childId={vasu.basics.child.id}
                />
                <StatusContainer>
                  <AutosaveStatusIndicator status={status} />
                  {showSpinner && <Spinner />}
                </StatusContainer>
              </FixedSpaceRow>
              <Button
                text={i18n.vasu.checkInPreview}
                disabled={status.state != 'clean'}
                onClick={() =>
                  navigate({
                    pathname: `/vasu/${vasu.id}`,
                    search:
                      childIdFromUrl !== null
                        ? `?childId=${childIdFromUrl}`
                        : undefined
                  })
                }
                data-qa="vasu-preview-btn"
                primary
              />
            </FixedSpaceRow>
          )}
        </StickyFooterContainer>
      </StickyFooter>
    </>
  )
})
