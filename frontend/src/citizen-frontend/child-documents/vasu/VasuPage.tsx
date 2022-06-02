// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { ContentArea } from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { useTranslation } from '../../localization'

import { givePermissionToShareVasu } from './api'
import { VasuContainer } from './components/VasuContainer'
import { CitizenBasicsSection } from './sections/CitizenBasicsSection'
import { CitizenDynamicSections } from './sections/CitizenDynamicSections'
import { CitizenVasuEvents } from './sections/CitizenVasuEvents'
import { CitizenVasuHeader } from './sections/CitizenVasuHeader'
import { useVasu } from './use-vasu'

const FooterContainer = styled.div`
  display: flex;
  justify-content: flex-start;
  align-items: center;
  padding: ${defaultMargins.s};
`

export default React.memo(function VasuPage() {
  const { id } = useNonNullableParams<{ id: UUID }>()
  const t = useTranslation()

  const [givePermissionToShareSelected, setGivePermissionToShareSelected] =
    useState<boolean>(false)

  const {
    vasu,
    content,
    translations,
    guardianHasGivenPermissionToShare,
    setGuardianHasGivenPermissionToShare
  } = useVasu(id)

  const dynamicSectionsOffset = 1

  return (
    <VasuContainer gapSize="zero" data-qa="vasu-preview">
      {vasu && (
        <>
          <ReturnButton label={t.common.return} />
          <CitizenVasuHeader document={vasu} />
          <CitizenBasicsSection
            sectionIndex={0}
            type={vasu.type}
            basics={vasu.basics}
            childLanguage={vasu.basics.childLanguage}
            templateRange={vasu.templateRange}
            translations={translations}
          />
          <CitizenDynamicSections
            sections={content.sections}
            sectionIndex={dynamicSectionsOffset}
            state={vasu.documentState}
            translations={translations}
          />
          <Gap size="s" />
          <CitizenVasuEvents document={vasu} content={content} />
        </>
      )}
      {vasu && !guardianHasGivenPermissionToShare && (
        <StickyFooter>
          <FooterContainer>
            <ContentArea opaque paddingVertical="xs" paddingHorizontal="L">
              <ExpandingInfo
                info={
                  vasu.type === 'DAYCARE'
                    ? t.vasu.givePermissionToShareInfoVasu
                    : t.vasu.givePermissionToShareInfoLeops
                }
                ariaLabel={t.common.openExpandingInfo}
              >
                <Label>
                  {vasu.type === 'DAYCARE'
                    ? t.vasu.givePermissionToShareTitleVasu
                    : t.vasu.givePermissionToShareTitleLeops}
                </Label>
              </ExpandingInfo>
              <Gap />
              <Checkbox
                checked={givePermissionToShareSelected}
                label={
                  vasu.type === 'DAYCARE'
                    ? t.vasu.givePermissionToShareVasu
                    : t.vasu.givePermissionToShareLeops
                }
                onChange={setGivePermissionToShareSelected}
                data-qa="confirm-checkbox"
              />
              <Gap />
              <AsyncButton
                primary
                text={t.common.confirm}
                disabled={!givePermissionToShareSelected}
                onClick={() => givePermissionToShareVasu(id)}
                onSuccess={() => setGuardianHasGivenPermissionToShare(true)}
                data-qa="confirm-button"
              />
            </ContentArea>
          </FooterContainer>
        </StickyFooter>
      )}
    </VasuContainer>
  )
})
