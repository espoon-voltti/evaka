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
import { PreschoolAssistanceResponse } from 'lib-common/generated/api-types/assistance'
import { useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { scrollToRef } from 'lib-common/utils/scrolling'
import Title from 'lib-components/atoms/Title'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'

import { ChildContext, ChildState } from '../../../state/child'
import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import {
  createPreschoolAssistanceMutation,
  updatePreschoolAssistanceMutation
} from '../queries'

import { PreschoolAssistanceForm } from './PreschoolAssistanceForm'
import { PreschoolAssistanceRow } from './PreschoolAssistanceRow'
import { TitleRow } from './TitleRow'

export interface Props {
  childId: UUID
  rows: Result<PreschoolAssistanceResponse[]>
}

type Mode = { type: 'new' } | { type: 'edit'; id: UUID }

export const PreschoolAssistanceSection = React.memo(
  function PreschoolAssistanceSection(props: Props) {
    const { i18n } = useTranslation()
    const t = i18n.childInformation.assistance
    const { permittedActions } = useContext<ChildState>(ChildContext)
    const refSectionTop = useRef(null)

    const { mutateAsync: createPreschoolAssistance } = useMutationResult(
      createPreschoolAssistanceMutation
    )
    const { mutateAsync: updatePreschoolAssistance } = useMutationResult(
      updatePreschoolAssistanceMutation
    )
    const [mode, setMode] = useState<Mode | undefined>(undefined)
    const clearMode = useCallback(() => setMode(undefined), [setMode])

    const childId = props.childId
    const rowsResult = useMemo(
      () =>
        props.rows.map((rows) =>
          orderBy(rows, (row) => row.data.validDuring.start.formatIso(), 'desc')
        ),
      [props.rows]
    )

    return renderResult(rowsResult, (rows) => (
      <div ref={refSectionTop}>
        <TitleRow>
          <Title size={4}>{t.preschoolAssistance.title}</Title>
          {permittedActions.has('CREATE_PRESCHOOL_ASSISTANCE') && (
            <AddButton
              flipped
              text={t.preschoolAssistance.create}
              onClick={() => {
                setMode({ type: 'new' })
                scrollToRef(refSectionTop)
              }}
              disabled={mode?.type !== undefined}
              data-qa="preschool-assistance-create-btn"
            />
          )}
        </TitleRow>
        {(mode?.type || rows.length > 0) && (
          <Table>
            <Thead>
              <Tr>
                <Th style={{ width: '30%' }}>{t.fields.level}</Th>
                <Th style={{ width: '30%' }}>{t.fields.validDuring}</Th>
                <Th style={{ width: '20%' }}>{t.fields.status}</Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {mode?.type === 'new' ? (
                <PreschoolAssistanceForm
                  allRows={rows}
                  onSubmit={(body) =>
                    createPreschoolAssistance({ child: childId, body }).then(
                      () => Success.of()
                    )
                  }
                  onClose={clearMode}
                />
              ) : undefined}
              {rows.map((row) =>
                mode?.type === 'edit' && mode.id === row.data.id ? (
                  <PreschoolAssistanceForm
                    key={row.data.id}
                    preschoolAssistance={row.data}
                    allRows={rows}
                    onSubmit={(body) =>
                      updatePreschoolAssistance({
                        childId,
                        id: row.data.id,
                        body
                      })
                    }
                    onClose={clearMode}
                  />
                ) : (
                  <PreschoolAssistanceRow
                    key={row.data.id}
                    preschoolAssistance={row}
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
