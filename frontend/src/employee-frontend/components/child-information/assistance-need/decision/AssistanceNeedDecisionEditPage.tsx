// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { ChildContextProvider } from 'employee-frontend/state/child'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Content, { ContentArea } from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import Select, { SelectOption } from 'lib-components/molecules/Select'
import { H1, H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { getPerson } from '../../../../api/person'

import AssistanceNeededDecisionForm from './AssistanceNeededDecisionForm'
import { useAssistanceNeedDecision } from './assistance-need-decision-form'
import { FooterContainer } from './common'

export default React.memo(function AssistanceNeedDecisionEditPage() {
  const { childId, id } = useNonNullableParams<{ childId: UUID; id: UUID }>()
  const [child] = useApiState(() => getPerson(childId), [childId])

  const { i18n } = useTranslation()

  const { formState, setFormState } = useAssistanceNeedDecision(childId, id)

  const languageOptions = useMemo<SelectOption[]>(
    () => [
      {
        value: 'fi',
        label: i18n.language.fi
      },
      {
        value: 'sv',
        label: i18n.language.sv
      }
    ],
    [i18n]
  )
  const [language, setLanguage] = useState<SelectOption>(languageOptions[0])
  const selectLanguage = useCallback((o: SelectOption | null) => {
    o !== null && setLanguage(o)
  }, [])

  return (
    <ChildContextProvider id={childId}>
      <Content>
        <ContentArea opaque>
          {renderResult(child, (child) => (
            <>
              <FixedSpaceRow alignItems="baseline">
                <Label>
                  {i18n.childInformation.assistanceNeedDecision.formLanguage}
                </Label>
                <Select
                  items={languageOptions}
                  selectedItem={language}
                  onChange={selectLanguage}
                />
              </FixedSpaceRow>
              <HorizontalLine />
              <H1 noMargin>{i18n.titles.assistanceNeedDecision}</H1>
              <H2 noMargin>
                {child.firstName} {child.lastName}
              </H2>
              <Gap size="s" />
              {formState && (
                <AssistanceNeededDecisionForm
                  formState={formState}
                  setFormState={setFormState}
                />
              )}
            </>
          ))}
        </ContentArea>
        <StickyFooter>
          <FooterContainer></FooterContainer>
        </StickyFooter>
      </Content>
    </ChildContextProvider>
  )
})
