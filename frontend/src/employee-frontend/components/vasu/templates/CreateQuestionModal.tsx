// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faTimesCircle } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useState } from 'react'
import styled, { useTheme } from 'styled-components'

import {
  QuestionOption,
  VasuQuestion,
  VasuQuestionType,
  vasuQuestionTypes
} from 'lib-common/api-types/vasu'
import { VasuSection } from 'lib-common/generated/api-types/vasu'
import { IconButton } from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import TextArea from 'lib-components/atoms/form/TextArea'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { faCalendarAlt, faCheckCircle, faTrash } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'

const TopRightOverlayedIcon = styled(FontAwesomeIcon)`
  transform: translate(70%, -175%) scale(0.8);
  pointer-events: none;
`

interface Props {
  onSave: (question: VasuQuestion) => void
  onCancel: () => void
  section: VasuSection
}

export default React.memo(function CreateQuestionModal({
  onCancel,
  onSave,
  section
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.vasuTemplates.questionModal
  const [type, setType] =
    useState<Exclude<VasuQuestionType, 'PARAGRAPH'>>('TEXT')
  const [name, setName] = useState('')

  const [options, setOptions] = useState<Omit<QuestionOption, 'key'>[]>([
    { name: '' }
  ])
  const [multiline, setMultiline] = useState(false)
  const [minSelections, setMinSelections] = useState(0)
  const [keys, setKeys] = useState<{ name: string; info?: string }[]>([
    { name: '' }
  ])
  const [info, setInfo] = useState('')
  const [trackedInEvents, setTrackedInEvents] = useState(false)
  const [nameInEvents, setNameInEvents] = useState('')
  const [identifier, setIdentifier] = useState('')
  const [dependsOn, setDependsOn] = useState<string[]>([])
  const [continuesNumbering, setContinuesNumbering] = useState(false)
  const [label, setLabel] = useState('')
  const [separateRows, setSeparateRows] = useState(false)

  function createQuestion(): VasuQuestion {
    const id = identifier || null
    switch (type) {
      case 'TEXT':
        return {
          type: 'TEXT',
          ophKey: null,
          name: name,
          info: info,
          multiline: multiline,
          value: '',
          id,
          dependsOn
        }
      case 'CHECKBOX':
        return {
          type: 'CHECKBOX',
          ophKey: null,
          name: name,
          info: info,
          value: false,
          id,
          dependsOn,
          label: label || null
        }
      case 'RADIO_GROUP':
        return {
          type: 'RADIO_GROUP',
          ophKey: null,
          name: name,
          info: info,
          options: options.map((opt) => ({
            ...opt,
            key: opt.name
          })),
          value: null,
          id,
          dependsOn,
          dateRange: null
        }
      case 'MULTISELECT':
        return {
          type: 'MULTISELECT',
          ophKey: null,
          name: name,
          info: info,
          options: options.map((opt) => ({
            ...opt,
            key: opt.name
          })),
          minSelections: minSelections,
          maxSelections: null,
          value: [],
          id,
          dependsOn
        }
      case 'MULTI_FIELD':
        return {
          type: 'MULTI_FIELD',
          ophKey: null,
          name,
          info,
          keys,
          value: keys.map(() => ''),
          id,
          dependsOn,
          separateRows
        }
      case 'MULTI_FIELD_LIST':
        return {
          type: 'MULTI_FIELD_LIST',
          ophKey: null,
          name,
          info,
          keys,
          value: [],
          id,
          dependsOn
        }
      case 'DATE':
        return {
          type: 'DATE',
          ophKey: null,
          name,
          info,
          trackedInEvents,
          nameInEvents: trackedInEvents ? nameInEvents : '',
          value: null,
          id,
          dependsOn
        }
      case 'FOLLOWUP':
        return {
          type: 'FOLLOWUP',
          ophKey: null,
          name: name,
          info: info,
          title: '',
          value: [],
          id,
          dependsOn,
          continuesNumbering
        }
      case 'STATIC_INFO_SUBSECTION':
        return {
          type: 'STATIC_INFO_SUBSECTION',
          ophKey: null,
          name: name,
          info: info,
          id,
          dependsOn
        }
    }
  }

  const theme = useTheme()

  return (
    <FormModal
      title={t.title}
      resolveAction={() => onSave(createQuestion())}
      resolveLabel={i18n.common.confirm}
      rejectAction={onCancel}
      rejectLabel={i18n.common.cancel}
    >
      <FixedSpaceColumn>
        <FixedSpaceColumn spacing="xxs">
          <Label>{t.type}</Label>
          <Combobox
            items={[...vasuQuestionTypes]}
            selectedItem={type}
            onChange={(value) => {
              if (value) setType(value)
            }}
            getItemLabel={(option) => i18n.vasuTemplates.questionTypes[option]}
          />
        </FixedSpaceColumn>

        {type === 'CHECKBOX' && (
          <FixedSpaceColumn spacing="xxs">
            <Label>{t.checkboxLabel}</Label>
            <InputField value={label} onChange={setLabel} width="full" />
          </FixedSpaceColumn>
        )}

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.name}</Label>
          <InputField value={name} onChange={setName} width="full" />
        </FixedSpaceColumn>

        {type === 'TEXT' && (
          <FixedSpaceColumn spacing="xxs">
            <Checkbox
              label={t.multiline}
              checked={multiline}
              onChange={setMultiline}
            />
          </FixedSpaceColumn>
        )}

        {(type === 'RADIO_GROUP' || type === 'MULTISELECT') && (
          <FixedSpaceColumn spacing="xxs">
            <Label>{t.options}</Label>
            {options.map((opt, i) => (
              <FixedSpaceRow spacing="xs" key={`opt-${i}`}>
                <InputField
                  value={opt.name}
                  onChange={(val) =>
                    setOptions([
                      ...options.slice(0, i),
                      { ...opt, name: val },
                      ...options.slice(i + 1)
                    ])
                  }
                  width="m"
                />
                <IconButton
                  icon={faTrash}
                  disabled={options.length < 2}
                  onClick={(e) => {
                    e.preventDefault()
                    setOptions([
                      ...options.slice(0, i),
                      ...options.slice(i + 1)
                    ])
                  }}
                  aria-label={i18n.common.remove}
                />
                {type === 'RADIO_GROUP' && (
                  <div>
                    <IconButton
                      icon={faCalendarAlt}
                      onClick={(e) => {
                        e.preventDefault()
                        setOptions([
                          ...options.slice(0, i),
                          {
                            ...opt,
                            dateRange: !opt.dateRange
                          },
                          ...options.slice(i + 1)
                        ])
                      }}
                      aria-label={i18n.common.datePicker.calendarLabel}
                    />
                    <TopRightOverlayedIcon
                      icon={opt.dateRange ? faCheckCircle : faTimesCircle}
                      color={
                        opt.dateRange
                          ? theme.colors.status.success
                          : theme.colors.status.danger
                      }
                    />
                  </div>
                )}
              </FixedSpaceRow>
            ))}
            <InlineButton
              onClick={() => setOptions([...options, { name: '' }])}
              text={t.addNewOption}
            />
          </FixedSpaceColumn>
        )}

        {type === 'MULTISELECT' && (
          <FixedSpaceColumn spacing="xxs">
            <Label>{t.minSelections}</Label>
            <InputField
              value={minSelections.toString(10)}
              onChange={(v) => setMinSelections(parseInt(v))}
              type="number"
              step={1}
              min={0}
              width="s"
            />
          </FixedSpaceColumn>
        )}

        {(type === 'MULTI_FIELD' || type === 'MULTI_FIELD_LIST') && (
          <FixedSpaceColumn spacing="xxs">
            <Label>{t.keys}</Label>
            {keys.map((key, i) => (
              <ExpandingInfo
                info={
                  <TextArea
                    value={key.info ?? ''}
                    onChange={(val) =>
                      setKeys([
                        ...keys.slice(0, i),
                        { ...key, info: val || undefined },
                        ...keys.slice(i + 1)
                      ])
                    }
                  />
                }
                key={`key-${i}`}
                width="full"
              >
                <FixedSpaceRow spacing="xs" alignItems="center">
                  <InputField
                    value={key.name}
                    onChange={(val) =>
                      setKeys([
                        ...keys.slice(0, i),
                        { ...key, name: val },
                        ...keys.slice(i + 1)
                      ])
                    }
                    width="m"
                  />
                  <IconButton
                    icon={faTrash}
                    disabled={keys.length < 2}
                    onClick={(e) => {
                      e.preventDefault()
                      setKeys([...keys.slice(0, i), ...keys.slice(i + 1)])
                    }}
                    aria-label={i18n.common.remove}
                  />
                </FixedSpaceRow>
              </ExpandingInfo>
            ))}
            <InlineButton
              onClick={() => setKeys([...keys, { name: '' }])}
              text={t.addNewKey}
            />
          </FixedSpaceColumn>
        )}

        {type === 'MULTI_FIELD' && (
          <Checkbox
            label={t.multifieldSeparateRows}
            checked={separateRows}
            onChange={setSeparateRows}
          />
        )}

        {type === 'DATE' && (
          <FixedSpaceColumn spacing="xxs">
            <Checkbox
              label={t.dateIsTrackedInEvents}
              checked={trackedInEvents}
              onChange={setTrackedInEvents}
            />
            {trackedInEvents && (
              <InputField
                value={nameInEvents}
                onChange={setNameInEvents}
                width="m"
              />
            )}
          </FixedSpaceColumn>
        )}

        {type === 'FOLLOWUP' && (
          <FixedSpaceColumn spacing="xxs">
            <Checkbox
              label={t.continuesNumbering}
              checked={continuesNumbering}
              onChange={setContinuesNumbering}
            />
          </FixedSpaceColumn>
        )}

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.info}</Label>
          <InputField value={info} onChange={setInfo} width="full" />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.id}</Label>
          <InputField
            value={identifier}
            onChange={setIdentifier}
            width="full"
          />
        </FixedSpaceColumn>

        <FixedSpaceColumn spacing="xxs">
          <Label>{t.dependsOn}</Label>
          <MultiSelect
            value={dependsOn}
            options={section.questions
              .map((q) => q.id)
              .filter((id): id is string => typeof id === 'string')}
            onChange={setDependsOn}
            getOptionId={(id) => id}
            getOptionLabel={(id) => id}
            placeholder=""
          />
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </FormModal>
  )
})
