// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'

import classNames from 'classnames'

interface LayoutProps {
  flex?: 1 | 0
  alignItems?: 'center' | 'start' | 'end'
  justifyContent?: 'center' | 'start' | 'end'
  className?: string
}

const Row: React.FC<LayoutProps> = (props) => (
  <div
    className={classNames(props.className, 'layout-row', {
      [`-flex-${props.flex ?? ''}`]: props.flex !== undefined,
      [`flex-align-items-${props.alignItems ?? ''}`]: props.alignItems,
      [`flex-justify-content-${
        props.justifyContent ?? ''
      }`]: props.justifyContent
    })}
  >
    {props.children}
  </div>
)

const Column: React.FC<LayoutProps> = (props) => (
  <div
    className={classNames(props.className, 'layout-column', {
      [`-flex-${props.flex ?? ''}`]: props.flex !== undefined,
      [`flex-align-items-${props.alignItems ?? ''}`]: props.alignItems,
      [`flex-justify-content-${
        props.justifyContent ?? ''
      }`]: props.justifyContent
    })}
  >
    {props.children}
  </div>
)

interface FlexProps {
  flex: 1 | 0
  alignItems?: 'center' | 'start' | 'end'
  justifyContent?: 'center' | 'start' | 'end'
  className?: string
}

const FlexImpl: React.FC<FlexProps> = (props) => (
  <div
    className={classNames(props.className, 'layout-flex', {
      [`-flex-${props.flex}`]: props.flex !== undefined,
      [`flex-align-items-${props.alignItems ?? ''}`]: props.alignItems,
      [`flex-justify-content-${
        props.justifyContent ?? ''
      }`]: props.justifyContent
    })}
  >
    {props.children}
  </div>
)

export const Flex = {
  Row,
  Column,
  Flex: FlexImpl
}

type Margin =
  | 'margin--1'
  | 'margin--2'
  | 'margin--3'
  | 'margin-top--1'
  | 'margin-top--2'
  | 'margin-top--3'
  | 'margin-bottom--1'
  | 'margin-bottom--2'
  | 'margin-bottom--3'
  | 'margin-left--1'
  | 'margin-left--2'
  | 'margin-left--3'
  | 'margin-right--1'
  | 'margin-right--2'
  | 'margin-right--3'

type Padding =
  | 'padding--1'
  | 'padding--2'
  | 'padding--3'
  | 'padding-top--1'
  | 'padding-top--2'
  | 'padding-top--3'
  | 'padding-bottom--1'
  | 'padding-bottom--2'
  | 'padding-bottom--3'
  | 'padding-left--1'
  | 'padding-left--2'
  | 'padding-left--3'
  | 'padding-right--1'
  | 'padding-right--2'
  | 'padding-right--3'

type Spacings = Array<Margin | Padding>

export const Spacing: React.FC<{
  children: React.ReactElement
  spacing: Spacings
}> = ({ children, spacing }) =>
  React.cloneElement(children, {
    className: classNames(...spacing)
  })
