// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, {
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState
} from 'react'

import { Result, Success } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import { AssistanceFactorResponse } from 'lib-common/generated/api-types/assistance'
import { useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { scrollToRef } from 'lib-common/utils/scrolling'
import Title from 'lib-components/atoms/Title'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  ExpandingInfoBox,
  InlineInfoButton
} from 'lib-components/molecules/ExpandingInfo'

import { ChildContext, ChildState } from '../../../state/child'
import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import {
  createAssistanceFactorMutation,
  updateAssistanceFactorMutation
} from '../queries'

import { AssistanceFactorForm } from './AssistanceFactorForm'
import { AssistanceFactorRow } from './AssistanceFactorRow'
import { TitleRow } from './TitleRow'

export interface Props {
  childId: UUID
  rows: Result<AssistanceFactorResponse[]>
}

type Mode = { type: 'new' } | { type: 'edit'; id: UUID }

export const AssistanceFactorSection = React.memo(
  function AssistanceFactorSection(props: Props) {
    const { i18n } = useTranslation()
    const t = i18n.childInformation.assistance
    const { permittedActions } = useContext<ChildState>(ChildContext)
    const refSectionTop = useRef(null)

    const { mutateAsync: createAssistanceFactor } = useMutationResult(
      createAssistanceFactorMutation
    )
    const { mutateAsync: updateAssistanceFactor } = useMutationResult(
      updateAssistanceFactorMutation
    )
    const [mode, setMode] = useState<Mode | undefined>(undefined)
    const clearMode = useCallback(() => setMode(undefined), [setMode])

    const [infoOpen, { off: closeInfo, toggle: toggleInfo }] = useBoolean(false)

    const childId = props.childId
    const rowsResult = useMemo(
      () =>
        props.rows.map((rows) =>
          orderBy(rows, (row) => row.data.validDuring.start.formatIso(), 'desc')
        ),
      [props.rows]
    )

    const info = t.assistanceFactor.info()
    return renderResult(rowsResult, (rows) => (
      <div ref={refSectionTop}>
        <TitleRow>
          <Title size={4}>
            {t.assistanceFactor.title}
            {info ? (
              <InlineInfoButton
                onClick={toggleInfo}
                aria-label={i18n.common.openExpandingInfo}
              />
            ) : undefined}
          </Title>
          {permittedActions.has('CREATE_ASSISTANCE_FACTOR') && (
            <AddButton
              flipped
              text={t.assistanceFactor.create}
              onClick={() => {
                setMode({ type: 'new' })
                scrollToRef(refSectionTop)
              }}
              disabled={mode?.type !== undefined}
              data-qa="assistance-factor-create-btn"
            />
          )}
        </TitleRow>
        {infoOpen && (
          <ExpandingInfoBox
            info={t.assistanceFactor.info()}
            close={closeInfo}
          />
        )}
        {(mode?.type || rows.length > 0) && (
          <Table>
            <Thead>
              <Tr>
                <Th style={{ width: '20%' }}>{t.fields.capacityFactor}</Th>
                <Th style={{ width: '30%' }}>{t.fields.validDuring}</Th>
                <Th style={{ width: '20%' }}>{t.fields.status}</Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {mode?.type === 'new' ? (
                <AssistanceFactorForm
                  allRows={rows}
                  onSubmit={(body) =>
                    createAssistanceFactor({ child: childId, body }).then(() =>
                      Success.of()
                    )
                  }
                  onClose={clearMode}
                />
              ) : undefined}
              {rows.map((row) =>
                mode?.type === 'edit' && mode.id === row.data.id ? (
                  <AssistanceFactorForm
                    key={row.data.id}
                    assistanceFactor={row.data}
                    allRows={rows}
                    onSubmit={(body) =>
                      updateAssistanceFactor({
                        childId,
                        id: row.data.id,
                        body
                      })
                    }
                    onClose={clearMode}
                  />
                ) : (
                  <AssistanceFactorRow
                    key={row.data.id}
                    assistanceFactor={row}
                    onEdit={() => {
                      setMode({ type: 'edit', id: row.data.id })
                      scrollToRef(refSectionTop)
                    }}
                  />
                )
              )}
            </Tbody>
          </Table>
        )}
      </div>
    ))
  }
)
