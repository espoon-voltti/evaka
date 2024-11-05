// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

import { localDate } from 'lib-common/form/fields'
import {
  object,
  oneOf,
  required,
  transformed,
  value
} from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import Main from 'lib-components/atoms/Main'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { H1, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import Footer from '../../../Footer'
import { renderResult } from '../../../async-rendering'
import { useLang, useTranslation } from '../../../localization'
import {
  childServiceNeedOptionsQuery,
  createServiceApplicationsMutation
} from '../../queries'

const form = transformed(
  object({
    startDate: required(localDate()),
    serviceNeed: required(oneOf<UUID>()),
    additionalInfo: value<string>()
  }),
  (res) => {
    if (res.startDate.date !== 1 && res.additionalInfo === '') {
      return ValidationError.field('additionalInfo', 'explainDate')
    }
    return ValidationSuccess.of(res)
  }
)

export default React.memo(function NewServiceApplicationPage() {
  const { childId } = useRouteParams(['childId'])
  const i18n = useTranslation()
  const navigate = useNavigate()
  const [lang] = useLang()
  const boundForm = useForm(
    form,
    () => ({
      startDate: localDate.fromDate(null, {
        minDate: LocalDate.todayInHelsinkiTz()
      }),
      serviceNeed: {
        options: [], // loaded when date is selected
        domValue: ''
      },
      additionalInfo: ''
    }),
    {
      ...i18n.validationErrors,
      explainDate: i18n.children.serviceApplication.additionalInfoRequired
    }
  )

  const { startDate, serviceNeed, additionalInfo } = useFormFields(boundForm)

  const optionsResult = useQueryResult(
    startDate.isValid()
      ? childServiceNeedOptionsQuery({ childId, date: startDate.value() })
      : constantQuery([])
  )

  const update = serviceNeed.update
  useEffect(() => {
    if (optionsResult.isSuccess) {
      const options = optionsResult.value.map((opt) => ({
        value: opt.id,
        domValue: opt.id,
        label:
          lang === 'sv' ? opt.nameSv : lang === 'en' ? opt.nameEn : opt.nameFi
      }))
      update((prev) => ({
        ...prev,
        options,
        domValue: options.some((opt) => opt.domValue === prev.domValue)
          ? prev.domValue
          : ''
      }))
    }
  }, [optionsResult, lang, update])

  const startDateWarning =
    startDate.isValid() && startDate.value().getDate() !== 1

  return (
    <>
      <Main>
        <Container>
          <ReturnButton label={i18n.common.return} />
          <ContentArea opaque>
            <H1 noMargin>{i18n.children.serviceApplication.createTitle}</H1>
            <Gap />
            <FixedSpaceColumn spacing="m">
              <div>
                <Label>{i18n.children.serviceApplication.startDate} *</Label>
                <Gap size="s" />
                <div>{i18n.children.serviceApplication.startDateInfo}</div>
                <Gap size="s" />
                <DatePickerF
                  bind={startDate}
                  locale={lang}
                  hideErrorsBeforeTouched
                  info={startDate.inputInfo()}
                  data-qa="start-date"
                />
                <Gap size="s" />
                <div aria-live="polite">
                  {startDateWarning && (
                    <>
                      <Gap size="s" />
                      <AlertBox
                        message={
                          i18n.children.serviceApplication
                            .startDateOnlyFirstDayWarning
                        }
                        noMargin
                        thin
                      />
                    </>
                  )}
                </div>
              </div>
              {startDate.isValid() && (
                <div>
                  {renderResult(optionsResult, (options) => (
                    <div>
                      {options.length > 0 ? (
                        <FixedSpaceColumn spacing="s">
                          <ExpandingInfo
                            info={
                              i18n.children.serviceApplication.serviceNeedInfo
                            }
                          >
                            <Label>
                              {i18n.children.serviceApplication.serviceNeed} *
                            </Label>
                          </ExpandingInfo>
                          <SelectF
                            bind={serviceNeed}
                            placeholder={i18n.common.select}
                            data-qa="service-need-option"
                          />
                        </FixedSpaceColumn>
                      ) : (
                        <AlertBox
                          title={
                            i18n.children.serviceApplication
                              .noSuitablePlacementOnDateTitle
                          }
                          message={
                            i18n.children.serviceApplication
                              .noSuitablePlacementOnDateMessage
                          }
                          noMargin
                        />
                      )}
                    </div>
                  ))}
                </div>
              )}
              <FixedSpaceColumn spacing="s">
                <ExpandingInfo
                  info={i18n.children.serviceApplication.additionalInfoInfo}
                >
                  <Label>
                    {i18n.children.serviceApplication.additionalInfo}
                    {startDateWarning && ' *'}
                  </Label>
                </ExpandingInfo>
                <InputFieldF bind={additionalInfo} data-qa="additional-info" />
              </FixedSpaceColumn>
              <FixedSpaceRow>
                <Button
                  text={i18n.common.cancel}
                  appearance="button"
                  onClick={() => navigate(`/children/${childId}`)}
                />
                <MutateButton
                  text={i18n.children.serviceApplication.send}
                  mutation={createServiceApplicationsMutation}
                  appearance="button"
                  primary
                  disabled={!boundForm.isValid() || !optionsResult.isSuccess}
                  onClick={() => ({
                    body: {
                      childId,
                      startDate: startDate.value(),
                      serviceNeedOptionId: serviceNeed.value(),
                      additionalInfo: additionalInfo.value()
                    }
                  })}
                  onSuccess={() => navigate(`/children/${childId}`)}
                  data-qa="create-button"
                />
              </FixedSpaceRow>
            </FixedSpaceColumn>
          </ContentArea>
        </Container>
      </Main>
      <Footer />
    </>
  )
})
