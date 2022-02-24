// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext } from 'react'
import styled from 'styled-components'

import { AdRole } from 'lib-common/api-types/employee-auth'
import Tooltip from 'lib-components/atoms/Tooltip'
import colors from 'lib-customizations/common'
import { faCopy, faPen, faSync, faTrash } from 'lib-icons'

import StatusLabel from '../../components/common/StatusLabel'
import { UserContext } from '../../state/user'
import { DateRangeOpen, getStatusLabelByDateRange } from '../../utils/date'
import { requireRole } from '../../utils/roles'

const ToolbarWrapper = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
`

const ToolbarButton = styled.button`
  cursor: ${(p) => (p.disabled ? 'not-allowed' : 'pointer')};
  margin-left: 10px;
  background: none;
  border: none;
`

const ToolbarStatus = styled.div`
  width: 100px;
  margin-left: 20px;
  display: flex;
  justify-content: flex-end;
`

interface ToolbarProps {
  dateRange?: DateRangeOpen
  conflict?: boolean
  warning?: string
  disableAll?: boolean
  onEdit?: () => undefined | void
  onDelete?: () => undefined | void
  onRetry?: () => undefined | void
  onCopy?: () => undefined | void
  editable?: boolean
  /**
   * @deprecated use editable-prop with actions
   */
  editableFor?: AdRole[]
  deletable?: boolean
  /**
   * @deprecated use deletable-prop with actions
   */
  deletableFor?: AdRole[]
  dataQaEdit?: string
  dataQaDelete?: string
  dataQaRetry?: string
  dataQaCopy?: string
  dataQa?: string
}
function Toolbar({
  dateRange,
  onEdit,
  onDelete,
  onRetry,
  onCopy,
  warning,
  conflict,
  disableAll,
  editable,
  editableFor,
  deletable,
  deletableFor,
  dataQaEdit,
  dataQaDelete,
  dataQaRetry,
  dataQaCopy,
  dataQa
}: ToolbarProps) {
  const { roles } = useContext(UserContext)

  const editAllowed: boolean =
    editable ||
    (editableFor === undefined && editable === undefined) ||
    (editableFor !== undefined && requireRole(roles, ...editableFor))
  const deleteAllowed: boolean =
    deletable ||
    (deletableFor === undefined && deletable === undefined) ||
    (deletableFor !== undefined && requireRole(roles, ...deletableFor))

  return (
    <ToolbarWrapper data-qa={dataQa}>
      {warning && (
        <Tooltip tooltip={warning} position="right">
          <FontAwesomeIcon
            icon={faExclamationTriangle}
            size="1x"
            color={colors.status.warning}
          />
        </Tooltip>
      )}

      {onRetry && editAllowed && (
        <ToolbarButton
          onClick={() => onRetry()}
          data-qa={dataQaRetry}
          disabled={disableAll}
        >
          <FontAwesomeIcon
            icon={faSync}
            color={disableAll ? colors.grayscale.g35 : colors.main.m2}
            size="lg"
          />
        </ToolbarButton>
      )}

      {onCopy && editAllowed && (
        <ToolbarButton
          onClick={() => onCopy()}
          data-qa={dataQaCopy}
          disabled={disableAll}
        >
          <FontAwesomeIcon
            icon={faCopy}
            color={disableAll ? colors.grayscale.g35 : colors.main.m2}
            size="lg"
          />
        </ToolbarButton>
      )}

      {onEdit && editAllowed && (
        <ToolbarButton
          onClick={() => onEdit()}
          data-qa={dataQaEdit}
          disabled={disableAll}
        >
          <FontAwesomeIcon
            icon={faPen}
            color={disableAll ? colors.grayscale.g35 : colors.main.m2}
            size="lg"
          />
        </ToolbarButton>
      )}

      {onDelete && deleteAllowed && (
        <ToolbarButton
          onClick={() => onDelete()}
          data-qa={dataQaDelete}
          disabled={disableAll}
        >
          <FontAwesomeIcon
            icon={faTrash}
            color={disableAll ? colors.grayscale.g35 : colors.main.m2}
            size="lg"
          />
        </ToolbarButton>
      )}

      {dateRange && (
        <ToolbarStatus>
          <StatusLabel
            status={
              conflict ? 'conflict' : getStatusLabelByDateRange(dateRange)
            }
          />
        </ToolbarStatus>
      )}
    </ToolbarWrapper>
  )
}

export default Toolbar
