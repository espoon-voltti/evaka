// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import AutosaveStatusIndicator from 'employee-frontend/components/common/AutosaveStatusIndicator'
import { I18nContext, Lang, useTranslation } from 'employee-frontend/state/i18n'
import { Failure } from 'lib-common/api'
import {
  AssistanceNeedDecisionForm,
  AssistanceNeedDecisionLanguage
} from 'lib-common/generated/api-types/assistanceneed'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import AssistanceNeedDecisionInfoHeader from 'lib-components/assistance-need-decision/AssistanceNeedDecisionInfoHeader'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Content, { ContentArea } from 'lib-components/layout/Container'
import StickyFooter from 'lib-components/layout/StickyFooter'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import Select, { SelectOption } from 'lib-components/molecules/Select'
import { H1, H2, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'

import { getPerson } from '../../../../api/person'

import AssistanceNeededDecisionForm, {
  FieldInfos
} from './AssistanceNeededDecisionForm'
import { useAssistanceNeedDecision } from './assistance-need-decision-form'
import { FooterContainer } from './common'

const requiredFormFields = ['selectedUnit'] as const

const HorizontalLineWithoutBottomMargin = styled(HorizontalLine)`
  margin-bottom: 0;
`

export default React.memo(function AssistanceNeedDecisionEditPage() {
  const { childId, id } = useNonNullableParams<{ childId: UUID; id: UUID }>()
  const [child] = useApiState(() => getPerson(childId), [childId])

  const { i18n } = useTranslation()

  const { formState, setFormState, status, forceSave } =
    useAssistanceNeedDecision(id)

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

  const missingFields = useMemo(
    () => requiredFormFields.filter((key) => !formState?.[key]),
    [formState]
  )

  const [showFormErrors, setShowFormErrors] = useState(false)

  const fieldInfos = useMemo<FieldInfos>(
    () =>
      (showFormErrors
        ? Object.fromEntries(
            missingFields.map((field) => [
              field,
              { text: i18n.validationErrors.required, status: 'warning' }
            ])
          )
        : {}) as FieldInfos,
    [missingFields, showFormErrors, i18n]
  )

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
                  fieldInfos={fieldInfos}
                />
              </I18nContext.Provider>
            </>
          ))}
        </ContentArea>
      </Content>
      <Gap size="m" />
      <StickyFooter>
        {showFormErrors && missingFields.length > 0 && (
          <>
            <H2>
              {i18n.childInformation.assistanceNeedDecision.validation.title}
            </H2>
            <AlertBox
              message={
                <>
                  <P noMargin>
                    {
                      i18n.childInformation.assistanceNeedDecision.validation
                        .description
                    }
                  </P>
                  <ul>
                    {missingFields.map((field) => (
                      <li key={field}>
                        {field in i18n.childInformation.assistanceNeedDecision
                          ? i18n.childInformation.assistanceNeedDecision[field]
                          : field}
                      </li>
                    ))}
                  </ul>
                </>
              }
            />
            <HorizontalLineWithoutBottomMargin slim />
          </>
        )}
        <FooterContainer>
          <AsyncButton
            primary
            text={i18n.childInformation.assistanceNeedDecision.leavePage}
            onClick={forceSave}
            onSuccess={() => navigate(`/child-information/${childId}`)}
            data-qa="leave-page-button"
            hideSuccess
          />
          <AutosaveStatusIndicator status={status} />
          <FlexGap />
          <AsyncButton
            primary
            text={i18n.childInformation.assistanceNeedDecision.preview}
            onClick={() => {
              if (missingFields.length === 0) {
                return forceSave()
              }

              setShowFormErrors(true)

              return Promise.resolve(
                Failure.of({
                  message: 'Invalid form'
                })
              )
            }}
            onSuccess={() =>
              navigate(
                `/child-information/${childId}/assistance-need-decision/${id}`
              )
            }
            data-qa="preview-button"
            hideSuccess
          />
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
  setFormState,
  fieldInfos
}: {
  child: PersonJSON
  formState?: AssistanceNeedDecisionForm
  setFormState: React.Dispatch<
    React.SetStateAction<AssistanceNeedDecisionForm | undefined>
  >
  fieldInfos: FieldInfos
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
        <AssistanceNeedDecisionInfoHeader
          decisionNumber={formState?.decisionNumber || 0}
          decisionStatus={formState?.status || 'DRAFT'}
          texts={i18n.childInformation.assistanceNeedDecision}
        />
      </FixedSpaceRow>
      {formState && (
        <AssistanceNeededDecisionForm
          formState={formState}
          setFormState={setFormState}
          fieldInfos={fieldInfos}
        />
      )}
    </>
  )
})
