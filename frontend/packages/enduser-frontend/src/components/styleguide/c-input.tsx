// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { CreateElement } from 'vue'
import { Prop } from 'vue-property-decorator'
import Component from 'vue-class-component'
import classNames from 'classnames'
import styled from 'vue-styled-components'

import BaseInput from './base-input'

type InputType = 'email' | 'text' | 'tel' | 'number' | 'password'
@Component
export default class CInput extends BaseInput {
  @Prop()
  public name: string | string[]

  @Prop({
    required: true,
    default: () => ({})
  })
  public value: object

  @Prop()
  public type: InputType

  @Prop({
    default: false
  })
  public showErrorIcon: boolean

  @Prop({
    default: () => []
  })
  public render(h: CreateElement) {
    const {
      id,
      required,
      disabled,
      labelWithAsterisk,
      inputIcon,
      leftIcon,
      state,
      type,
      placeholder,
      _inputValue,
      inputName,
      handleInput,
      handleFocus,
      handleBlur,
      message,
      showErrorIcon
    } = this

    return (
      <InputContainer class={classNames('field', { required, disabled })}>
        <label for={id}>
          {labelWithAsterisk}
          {this.$slots.default}
        </label>
        <div
          class={classNames(
            'control',
            { 'has-icons-right': inputIcon !== undefined },
            { 'has-icons-left': leftIcon }
          )}
        >
          {leftIcon && (
            <span class="icon is-left">
              <font-awesome-icon icon={leftIcon} />
            </span>
          )}
          <input
            id={id}
            class={classNames('input', state ? `is-${state}` : undefined)}
            disabled={disabled}
            type={type}
            placeholder={placeholder || labelWithAsterisk}
            value={_inputValue}
            onInput={handleInput}
            name={inputName}
            onFocus={handleFocus}
            onBlur={handleBlur}
            aria-required={required}
          />
          {inputIcon && showErrorIcon && (
            <span class="icon is-right">
              <font-awesome-icon icon={inputIcon} />
            </span>
          )}
          {message && (
            <p class={classNames('help', state ? `is-${state}` : undefined)}>
              {this.$t(message)}
            </p>
          )}
        </div>
      </InputContainer>
    )
  }
}

const InputContainer = styled.div`
  &.disabled {
    input {
      background-color: #f5f5f58a;
    }
  }

  span.icon.is-right {
    top: 1.2rem !important;
  }
`
