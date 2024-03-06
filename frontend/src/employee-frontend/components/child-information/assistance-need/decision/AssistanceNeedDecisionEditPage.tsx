// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import concat from 'lodash/concat'
import isEmpty from 'lodash/isEmpty'
import omit from 'lodash/omit'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import AutosaveStatusIndicator from 'employee-frontend/components/common/AutosaveStatusIndicator'
import { I18nContext, Lang, useTranslation } from 'employee-frontend/state/i18n'
import { AutosaveStatus } from 'employee-frontend/utils/use-autosave'
import { Failure, Result, wrapResult } from 'lib-common/api'
import {
  AssistanceNeedDecisionForm,
  AssistanceNeedDecisionLanguage
} from 'lib-common/generated/api-types/assistanceneed'
import { Employee, PersonJSON } from 'lib-common/generated/api-types/pis'
import useRequiredParams from 'lib-common/useRequiredParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import AssistanceNeedDecisionInfoHeader from 'lib-components/assistance-need-decision/AssistanceNeedDecisionInfoHeader'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { InputInfo } from 'lib-components/atoms/form/InputField'
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
import { featureFlags, Translations } from 'lib-customizations/employee'

import { getPersonIdentity } from '../../../../generated/api-clients/pis'

import AssistanceNeededDecisionForm, {
  FieldInfos
} from './AssistanceNeededDecisionForm'
import { useAssistanceNeedDecision } from './assistance-need-decision-form'
import { FooterContainer } from './common'

const getPersonIdentityResult = wrapResult(getPersonIdentity)

// straightforward required fields, more complex cases are handled
// in missingFields and fieldInfos useMemos
const requiredFormFields = [
  'selectedUnit',
  'pedagogicalMotivation',
  'guardiansHeardOn'
] as const

const getFieldTranslation = (
  t: Translations['childInformation']['assistanceNeedDecision'],
  fieldKey: keyof FieldInfos
): string => {
  switch (fieldKey) {
    case 'guardianInfo':
      return t.guardiansHeardOn
    case 'viewOfGuardians':
      return t.viewOfTheGuardians
    case 'servicesMotivation':
      return t.servicesPlaceholder
    case 'decisionMakerTitle':
      return `${t.decisionMaker}, ${t.title}`
    case 'preparator1Title':
      return `${t.preparator} (1.), ${t.title}`
    case 'preparator2Title':
      return `${t.preparator} (2.), ${t.title}`
    case 'otherRepresentative':
      return t.otherLegalRepresentation
    case 'guardianDetails':
      return t.guardiansHeard
  }

  return t[fieldKey]
}

const HorizontalLineWithoutBottomMargin = styled(HorizontalLine)`
  margin-bottom: 0;
`

export default React.memo(function AssistanceNeedDecisionEditPage() {
  const { childId, id } = useRequiredParams('childId', 'id')
  const [child] = useApiState(
    () => getPersonIdentityResult({ personId: childId }),
    [childId]
  )

  const { i18n } = useTranslation()

  const { decisionMakerOptions, formState, setFormState, status, forceSave } =
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
    () =>
      concat(
        [] as (keyof FieldInfos | undefined | null | false)[],
        requiredFormFields.filter((key) => !formState?.[key]),
        // decision-maker is required
        !formState?.decisionMaker?.employeeId && 'decisionMaker',
        // at least one preparator must be selected
        !formState?.preparedBy1?.employeeId &&
          !formState?.preparedBy2?.employeeId &&
          'preparator',
        // decision-maker's title is required
        !formState?.decisionMaker?.title && 'decisionMakerTitle',
        // titles are required, if preparator is selected
        formState?.preparedBy1 &&
          !formState.preparedBy1.title &&
          'preparator1Title',
        formState?.preparedBy2 &&
          !formState.preparedBy2.title &&
          'preparator2Title',
        // there must be an end date if ASSISTANCE_SERVICES_FOR_TIME is selected
        formState?.assistanceLevels.includes('ASSISTANCE_SERVICES_FOR_TIME') &&
          formState?.validityPeriod.end === null &&
          'endDate',
        // at least one assistance level must be selected
        formState?.assistanceLevels.length === 0 && 'futureLevelOfAssistance'
      ).filter((field): field is keyof FieldInfos => !!field),
    [formState]
  )

  const [showFormErrors, setShowFormErrors] = useState(false)

  const fieldInfos = useMemo<FieldInfos>(
    () =>
      ({
        ...Object.fromEntries(
          missingFields.map((field) => [
            field,
            { text: i18n.validationErrors.required, status: 'warning' }
          ])
        ),
        guardianInfo: !formState?.guardianInfo.every(
          (guardian) => guardian.isHeard
        )
          ? {
              text: i18n.childInformation.assistanceNeedDecision
                .guardiansHeardValidation,
              status: 'warning'
            }
          : undefined,
        guardianDetails: Object.fromEntries(
          formState?.guardianInfo
            .filter(
              (guardian) =>
                !guardian.details || guardian.details.trim().length === 0
            )
            .map<[string, InputInfo]>((guardian) => [
              guardian.id ?? '',
              {
                text: i18n.validationErrors.required,
                status: 'warning'
              }
            ]) ?? []
        ),
        otherRepresentative:
          formState?.otherRepresentativeHeard &&
          (!formState?.otherRepresentativeDetails ||
            formState?.otherRepresentativeDetails.trim().length === 0)
            ? {
                text: i18n.validationErrors.required,
                status: 'warning'
              }
            : undefined
      }) as const,
    [
      missingFields,
      formState?.guardianInfo,
      formState?.otherRepresentativeHeard,
      formState?.otherRepresentativeDetails,
      i18n.childInformation.assistanceNeedDecision.guardiansHeardValidation,
      i18n.validationErrors.required
    ]
  )

  return (
    <>
      <Content>
        <ReturnButton label={i18n.common.goBack} />

        <ContentArea opaque>
          {renderResult(child, (child) => (
            <>
              {featureFlags.assistanceNeedDecisionsLanguageSelect && (
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
                  status={status}
                  decisionMakerOptions={decisionMakerOptions}
                  child={child}
                  formState={formState}
                  setFormState={setFormState}
                  fieldInfos={showFormErrors ? fieldInfos : {}}
                />
              </I18nContext.Provider>
            </>
          ))}
        </ContentArea>
      </Content>
      <Gap size="m" />
      <StickyFooter>
        {showFormErrors &&
          Object.values(fieldInfos).filter((value) => value && !isEmpty(value))
            .length > 0 && (
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
                      {(
                        Object.entries(
                          omit(fieldInfos, ['guardianDetails'])
                        ).filter(([, value]) => value) as [
                          keyof FieldInfos,
                          InputInfo
                        ][]
                      ).map(([field, value]) => (
                        <li key={field}>
                          {getFieldTranslation(
                            i18n.childInformation.assistanceNeedDecision,
                            field
                          )}
                          : {value?.text}
                        </li>
                      ))}
                      {Object.entries(fieldInfos.guardianDetails ?? {}).map(
                        ([id, value]) =>
                          value && (
                            <li key={id}>
                              {getFieldTranslation(
                                i18n.childInformation.assistanceNeedDecision,
                                'guardianDetails'
                              )}{' '}
                              (
                              {
                                formState?.guardianInfo.find(
                                  (guardian) => guardian.id === id
                                )?.name
                              }
                              ): {value?.text}
                            </li>
                          )
                      )}
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
              if (
                Object.values(fieldInfos).filter(
                  (value) => value && !isEmpty(value)
                ).length === 0
              ) {
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
  status,
  decisionMakerOptions,
  child,
  formState,
  setFormState,
  fieldInfos
}: {
  status: AutosaveStatus
  decisionMakerOptions: Result<Employee[]>
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
          status={status}
          decisionMakerOptions={decisionMakerOptions}
          formState={formState}
          setFormState={setFormState}
          fieldInfos={fieldInfos}
        />
      )}
    </>
  )
})
