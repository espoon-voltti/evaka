// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import AutosaveStatusIndicator from 'employee-frontend/components/common/AutosaveStatusIndicator'
import { I18nContext, Lang, useTranslation } from 'employee-frontend/state/i18n'
import {
  AssistanceNeedDecisionForm,
  AssistanceNeedDecisionLanguage
} from 'lib-common/generated/api-types/assistanceneed'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Button from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Content, { ContentArea } from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import Select, { SelectOption } from 'lib-components/molecules/Select'
import { H1, H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'

import { getPerson } from '../../../../api/person'

import AssistanceNeededDecisionForm from './AssistanceNeededDecisionForm'
import { useAssistanceNeedDecision } from './assistance-need-decision-form'
import { FooterContainer, DecisionInfoHeader } from './common'

export default React.memo(function AssistanceNeedDecisionEditPage() {
  const { childId, id } = useNonNullableParams<{ childId: UUID; id: UUID }>()
  const [child] = useApiState(() => getPerson(childId), [childId])

  const { i18n } = useTranslation()

  const { formState, setFormState, status } = useAssistanceNeedDecision(id)

  const navigate = useNavigate()

  const languageOptions = useMemo<SelectOption[]>(
    () => [
      {
        value: 'FI',
        label: i18n.language.fi
      },
      {
        value: 'SV',
        label: i18n.language.sv
      }
    ],
    [i18n]
  )
  const selectLanguage = useCallback(
    (o: SelectOption | null) => {
      if (formState && o) {
        setFormState({
          ...formState,
          language: (o.value as AssistanceNeedDecisionLanguage) ?? 'FI'
        })
      }
    },
    [formState, setFormState]
  )
  const formLanguageState = useMemo(
    () => ({
      lang: (formState?.language.toLowerCase() ?? 'fi') as Lang,
      setLang: () => undefined
    }),
    [formState]
  )

  useEffect(() => {
    if (
      formState &&
      formState.status !== 'NEEDS_WORK' &&
      (formState.status !== 'DRAFT' || formState.sentForDecision !== null)
    ) {
      navigate(`/child-information/${childId}/assistance-need-decision/${id}`, {
        replace: true
      })
    }
  }, [formState, childId, id, navigate])

  return (
    <>
      <Content>
        <ReturnButton label={i18n.common.goBack} />

        <ContentArea opaque>
          {renderResult(child, (child) => (
            <>
              {featureFlags.experimental
                ?.assistanceNeedDecisionsLanguageSelect && (
                <>
                  <FixedSpaceRow alignItems="baseline">
                    <Label>
                      {
                        i18n.childInformation.assistanceNeedDecision
                          .formLanguage
                      }
                    </Label>
                    <Select
                      items={languageOptions}
                      selectedItem={
                        languageOptions.find(
                          (l) => l.value === formState?.language
                        ) ?? languageOptions[0]
                      }
                      onChange={selectLanguage}
                      data-qa="language-select"
                    />
                  </FixedSpaceRow>
                  <HorizontalLine />
                </>
              )}
              <I18nContext.Provider value={formLanguageState}>
                <DecisionContents
                  child={child}
                  formState={formState}
                  setFormState={setFormState}
                />
              </I18nContext.Provider>
            </>
          ))}
        </ContentArea>
      </Content>
      <Gap size="m" />
      <StickyFooter>
        <FooterContainer>
          <Button
            onClick={() => navigate(`/child-information/${childId}`)}
            data-qa="leave-page-button"
          >
            {i18n.childInformation.assistanceNeedDecision.leavePage}
          </Button>
          <AutosaveStatusIndicator status={status} />
          <FlexGap />
          <Button
            primary
            onClick={() =>
              navigate(
                `/child-information/${childId}/assistance-need-decision/${id}`
              )
            }
            data-qa="preview-button"
          >
            {i18n.childInformation.assistanceNeedDecision.preview}
          </Button>
        </FooterContainer>
      </StickyFooter>
    </>
  )
})

const FlexGap = styled.div`
  flex: 1;
`

const DecisionContents = React.memo(function DecisionContents({
  child,
  formState,
  setFormState
}: {
  child: PersonJSON
  formState?: AssistanceNeedDecisionForm
  setFormState: React.Dispatch<
    React.SetStateAction<AssistanceNeedDecisionForm | undefined>
  >
}) {
  const { i18n } = useTranslation()

  return (
    <>
      <FixedSpaceRow
        alignItems="flex-start"
        justifyContent="space-between"
        fullWidth
      >
        <FixedSpaceColumn>
          <H1 noMargin data-qa="page-title">
            {i18n.childInformation.assistanceNeedDecision.pageTitle}
          </H1>
          <H2 noMargin>
            {child.firstName} {child.lastName}
          </H2>
        </FixedSpaceColumn>
        <DecisionInfoHeader
          decisionNumber={formState?.decisionNumber || 0}
          decisionStatus={formState?.status || 'DRAFT'}
        />
      </FixedSpaceRow>
      {formState && (
        <AssistanceNeededDecisionForm
          formState={formState}
          setFormState={setFormState}
        />
      )}
    </>
  )
})
