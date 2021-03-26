import React, { useEffect, useState } from 'react'
import { UUID } from 'employee-frontend/types'
import styled from 'styled-components'
import { Loading, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { H1 } from 'lib-components/typography'
import { Table, Tr, Th, Td, Thead, Tbody } from 'lib-components/layout/Table'
import { defaultMargins } from 'lib-components/white-space'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Button from 'lib-components/atoms/buttons/Button'
import colors from 'lib-components/colors'
import { ReceiverTriplet, ReceiverGroup } from './types'
import { useTranslation } from '../../state/i18n'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faAngleDown, faAngleUp } from 'lib-icons'
import { getReceivers } from 'employee-frontend/components/messages/api'
import {
  getSelectorStatus,
  getReceiverTriplets,
  SelectorNode,
  updateSelector
} from 'employee-frontend/components/messages/receiver-selection-utils'

interface Props {
  unitId: UUID
  onCreateNew: (receivers: ReceiverTriplet[]) => void
  setReceiverTriplets: (value: ReceiverTriplet[]) => void
}

export default React.memo(function ReceiverSelection({
  unitId,
  onCreateNew,
  setReceiverTriplets
}: Props) {
  const { i18n } = useTranslation()

  const [receiversResult, setReceiversResult] = useState<
    Result<ReceiverGroup[]>
  >(Loading.of())

  const loadReceivers = useRestApi(getReceivers, setReceiversResult)
  useEffect(() => loadReceivers(unitId), [])

  const [collapsedGroups, setCollapsedGroups] = useState<UUID[]>([])

  const isCollapsed = (groupId: UUID) => collapsedGroups.indexOf(groupId) > -1

  const setCollapsed = (groupId: UUID) =>
    setCollapsedGroups((old) => [...old, groupId])

  const setNotCollapsed = (groupId: UUID) =>
    setCollapsedGroups((old) => old.filter((id) => id !== groupId))

  const toggleCollapsed = (groupId: UUID) =>
    isCollapsed(groupId) ? setNotCollapsed(groupId) : setCollapsed(groupId)

  const [selectorChange, setSelectorChange] = useState<SelectorNode>()
  const [receiverSelection, setReceiverSelection] = useState<SelectorNode>()

  useEffect(() => {
    if (receiversResult.isSuccess) {
      setReceiverSelection({
        selectorId: unitId,
        selected: false,
        childNodes: receiversResult.value.map((group) => ({
          selectorId: group.groupId,
          selected: false,
          childNodes: group.receivers.map((child) => ({
            selectorId: child.childId,
            selected: false,
            childNodes: undefined
          }))
        }))
      })
    }
  }, [receiversResult])

  useEffect(() => {
    if (receiverSelection && selectorChange) {
      setReceiverSelection(updateSelector(receiverSelection, selectorChange))
    }
  }, [selectorChange])

  useEffect(() => {
    if (receiverSelection) {
      setReceiverTriplets(getReceiverTriplets(receiverSelection))
    }
  }, [receiverSelection])

  return (
    <Container>
      <H1>{i18n.messages.receiverSelection.title}</H1>
      <Table>
        <Thead>
          <Tr>
            <Th>{i18n.messages.receiverSelection.childName}</Th>
            <Th>{i18n.messages.receiverSelection.childDob}</Th>
            <Th>{i18n.messages.receiverSelection.receivers}</Th>
            <Th>
              <Checkbox
                label={''}
                checked={
                  receiverSelection
                    ? getSelectorStatus(unitId, receiverSelection)
                    : false
                }
                onChange={(checked) =>
                  setSelectorChange({ selectorId: unitId, selected: checked })
                }
              />
            </Th>
          </Tr>
        </Thead>
        {receiversResult.isSuccess &&
          receiverSelection &&
          receiversResult.value.map((receiverGroup) => (
            <Tbody key={receiverGroup.groupId}>
              <Tr>
                <Td
                  colSpan={3}
                  onClick={() => toggleCollapsed(receiverGroup.groupId)}
                >
                  <IconWrapper>
                    <FontAwesomeIcon
                      icon={
                        isCollapsed(receiverGroup.groupId)
                          ? faAngleUp
                          : faAngleDown
                      }
                    />
                  </IconWrapper>
                  <strong>{receiverGroup.groupName}</strong>
                </Td>
                <Td>
                  <Checkbox
                    label={''}
                    checked={getSelectorStatus(
                      receiverGroup.groupId,
                      receiverSelection
                    )}
                    onChange={(checked) =>
                      setSelectorChange({
                        selectorId: receiverGroup.groupId,
                        selected: checked
                      })
                    }
                  />
                </Td>
              </Tr>
              {!isCollapsed(receiverGroup.groupId) &&
                receiverGroup.receivers.map((receiverChild) => (
                  <Tr key={receiverChild.childId}>
                    <Td>
                      {receiverChild.childFirstName}{' '}
                      {receiverChild.childLastName}
                    </Td>
                    <Td>{receiverChild.childDateOfBirth.format()}</Td>
                    <Td>
                      {receiverChild.receiverPersons.map(
                        ({
                          receiverId,
                          receiverFirstName,
                          receiverLastName
                        }) => (
                          <div key={receiverId}>
                            {receiverFirstName} {receiverLastName}
                          </div>
                        )
                      )}
                    </Td>
                    <Td>
                      <Checkbox
                        label={''}
                        checked={getSelectorStatus(
                          receiverChild.childId,
                          receiverSelection
                        )}
                        onChange={(checked) =>
                          setSelectorChange({
                            selectorId: receiverChild.childId,
                            selected: checked
                          })
                        }
                        dataQa={`check-receiver-${receiverChild.childId}`}
                      />
                    </Td>
                  </Tr>
                ))}
            </Tbody>
          ))}
      </Table>
      <Button
        text={i18n.messages.receiverSelection.confirmText}
        primary
        onClick={() =>
          receiverSelection &&
          onCreateNew(getReceiverTriplets(receiverSelection))
        }
      />
    </Container>
  )
})

const Container = styled.div`
  flex-grow: 1;
  min-height: 500px;
  overflow-y: auto;
  padding: ${defaultMargins.m};
  background-color: ${colors.greyscale.white};
  display: flex;
  flex-direction: column;
`

const IconWrapper = styled.span`
  color: ${colors.greyscale.medium};
  margin-right: ${defaultMargins.s};
`
