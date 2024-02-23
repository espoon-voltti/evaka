// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { AdditionalInformation } from 'lib-common/generated/api-types/daycare'
import { IsoLanguage, isoLanguages } from 'lib-common/generated/language'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import TextArea from 'lib-components/atoms/form/TextArea'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H4 } from 'lib-components/typography'
import { faPen } from 'lib-icons'

import LabelValueList from '../../../components/common/LabelValueList'
import {
  getAdditionalInfo,
  updateAdditionalInfo
} from '../../../generated/api-clients/daycare'
import { useTranslation } from '../../../state/i18n'
import { UIContext, UiState } from '../../../state/ui'
import { formatParagraphs } from '../../../utils/html-utils'
import { RequireRole } from '../../../utils/roles'
import { renderResult } from '../../async-rendering'
import { FlexRow } from '../../common/styled/containers'
import { textAreaRows } from '../../utils'

const TextAreaInput = styled(TextArea)`
  width: 100%;
`

const RightAlignedRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  padding-top: 20px;
`

const filterLanguages = (
  input: string,
  items: readonly IsoLanguage[]
): IsoLanguage[] => {
  const filter = input.toLowerCase()
  return items.filter((item) => item.nameFi.includes(filter))
}

interface Props {
  id: UUID
}

export default React.memo(function AdditionalInformation({ id }: Props) {
  const { i18n } = useTranslation()
  const [additionalInformation, loadData] = useApiState(
    () => wrapResult(getAdditionalInfo)({ childId: id }),
    [id]
  )
  const { uiMode, toggleUiMode, clearUiMode } = useContext<UiState>(UIContext)
  const [form, setForm] = useState<AdditionalInformation>({
    additionalInfo: '',
    allergies: '',
    diet: '',
    preferredName: '',
    medication: '',
    languageAtHome: '',
    languageAtHomeDetails: ''
  })

  const editing = uiMode == 'child-additional-details-editing'

  const startEdit = useCallback(() => {
    if (additionalInformation.isSuccess) {
      setForm({
        additionalInfo: additionalInformation.value.additionalInfo,
        allergies: additionalInformation.value.allergies,
        diet: additionalInformation.value.diet,
        preferredName: additionalInformation.value.preferredName,
        medication: additionalInformation.value.medication,
        languageAtHome: additionalInformation.value.languageAtHome,
        languageAtHomeDetails: additionalInformation.value.languageAtHomeDetails
      })
      toggleUiMode('child-additional-details-editing')
    }
  }, [additionalInformation, toggleUiMode])

  const onSubmit = useCallback(
    () => wrapResult(updateAdditionalInfo)({ childId: id, body: form }),
    [id, form]
  )
  const onSuccess = useCallback(() => {
    clearUiMode()
    void loadData()
  }, [clearUiMode, loadData])

  const valueWidth = '600px'

  const languages = useMemo(
    () => sortBy(Object.values(isoLanguages), ({ nameFi }) => nameFi),
    []
  )

  return (
    <div data-qa="additional-information-section">
      <FlexRow justifyContent="space-between">
        <H4>{i18n.childInformation.additionalInformation.title}</H4>
        {!editing && (
          <RequireRole
            oneOf={[
              'SERVICE_WORKER',
              'FINANCE_ADMIN',
              'UNIT_SUPERVISOR',
              'ADMIN',
              'STAFF',
              'SPECIAL_EDUCATION_TEACHER'
            ]}
          >
            <InlineButton
              icon={faPen}
              onClick={startEdit}
              data-qa="edit-child-settings-button"
              text={i18n.common.edit}
            />
          </RequireRole>
        )}
      </FlexRow>
      {renderResult(additionalInformation, (data) => (
        <>
          <LabelValueList
            spacing="small"
            contents={[
              {
                label:
                  i18n.childInformation.additionalInformation.preferredName,
                value: editing ? (
                  <TextAreaInput
                    value={form.preferredName}
                    onChange={(value) =>
                      setForm({ ...form, preferredName: value })
                    }
                    rows={textAreaRows(form.preferredName)}
                  />
                ) : (
                  formatParagraphs(data.preferredName)
                ),
                valueWidth
              },
              {
                label:
                  i18n.childInformation.additionalInformation.additionalInfo,
                value: editing ? (
                  <TextAreaInput
                    value={form.additionalInfo}
                    onChange={(value) =>
                      setForm({ ...form, additionalInfo: value })
                    }
                    rows={textAreaRows(form.additionalInfo)}
                  />
                ) : (
                  formatParagraphs(data.additionalInfo)
                ),
                valueWidth
              },
              {
                label: i18n.childInformation.additionalInformation.allergies,
                value: editing ? (
                  <TextAreaInput
                    value={form.allergies}
                    onChange={(value) => setForm({ ...form, allergies: value })}
                    rows={textAreaRows(form.allergies)}
                  />
                ) : (
                  formatParagraphs(data.allergies)
                ),
                valueWidth
              },
              {
                label: i18n.childInformation.additionalInformation.diet,
                value: editing ? (
                  <TextAreaInput
                    value={form.diet}
                    onChange={(value) => setForm({ ...form, diet: value })}
                    rows={textAreaRows(form.diet)}
                  />
                ) : (
                  formatParagraphs(data.diet)
                ),
                valueWidth
              },
              {
                label: i18n.childInformation.additionalInformation.medication,
                value: editing ? (
                  <TextAreaInput
                    value={form.medication}
                    onChange={(value) =>
                      setForm({ ...form, medication: value })
                    }
                    rows={textAreaRows(form.medication)}
                    data-qa="medication-input"
                  />
                ) : (
                  <span data-qa="medication">
                    {formatParagraphs(data.medication)}
                  </span>
                ),
                valueWidth
              },
              {
                label: i18n.childInformation.personDetails.languageAtHome,
                value: editing ? (
                  <>
                    <Combobox
                      data-qa="input-language-at-home"
                      items={languages}
                      getItemDataQa={(item) => `language-${item.id}`}
                      selectedItem={
                        (form.languageAtHome
                          ? isoLanguages[form.languageAtHome]
                          : null) ?? null
                      }
                      onChange={(item) =>
                        setForm({ ...form, languageAtHome: item?.id ?? '' })
                      }
                      filterItems={filterLanguages}
                      getItemLabel={(item) => item?.nameFi}
                      placeholder={
                        i18n.childInformation.personDetails.placeholder
                          .languageAtHome
                      }
                      clearable={true}
                    />
                    <TextArea
                      data-qa="input-language-at-home-details"
                      value={form.languageAtHomeDetails}
                      placeholder={
                        i18n.childInformation.personDetails.placeholder
                          .languageAtHomeDetails
                      }
                      onChange={(languageAtHomeDetails) =>
                        setForm({ ...form, languageAtHomeDetails })
                      }
                    />
                  </>
                ) : (
                  <>
                    <div data-qa="person-language-at-home">
                      {isoLanguages[data.languageAtHome]?.nameFi ?? ''}
                    </div>
                    <div data-qa="person-language-at-home-details">
                      {data.languageAtHomeDetails}
                    </div>
                  </>
                )
              }
            ]}
          />
          {editing && (
            <RightAlignedRow>
              <FixedSpaceRow>
                <Button
                  onClick={() => clearUiMode()}
                  text={i18n.common.cancel}
                />
                <AsyncButton
                  primary
                  disabled={false}
                  onClick={onSubmit}
                  onSuccess={onSuccess}
                  data-qa="confirm-edited-child-button"
                  text={i18n.common.confirm}
                />
              </FixedSpaceRow>
            </RightAlignedRow>
          )}
        </>
      ))}
    </div>
  )
})
