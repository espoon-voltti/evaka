// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { CreateElement } from 'vue'
import Component from 'vue-class-component'
import { Prop } from 'vue-property-decorator'
import { set, get } from 'lodash'
import { uuid } from '../../utils/helpers'
import styled from 'vue-styled-components'

import BaseInput from './base-input'

@Component
class Checkbox extends BaseInput {
  public id: string = uuid()

  @Prop()
  public name: string | string[]

  @Prop()
  public value: object

  @Prop()
  public label: string

  get inputName() {
    return this.name instanceof Array ? this.name.join('.') : this.name
  }

  get inputValue(): boolean {
    return get(this.value, this.name)
  }

  public render(h: CreateElement) {
    return (
      <CheckboxWrapper class="checkbox-wrapper">
        <div
          class="checkbox"
          tabindex="0"
          onkeyup={this.handleKeyUp}
          role="checkbox"
          aria-checked={this.inputValue}
        >
          <input
            class="input"
            type="checkbox"
            id={this.id}
            value={this.inputValue}
            name={this.inputName}
            checked={this.inputValue}
            onChange={this.handleInput}
            ref="checkbox"
          />

          <label class="label" for={this.id}>
            <div class="tick">
              <i class="fal fa-check" aria-hidden></i>
            </div>
            {this.label}
          </label>
          {this.$slots.default}
        </div>
      </CheckboxWrapper>
    )
  }

  public handleKeyUp(e: any) {
    if (e.keyCode === 13) {
      const el = this.$refs.checkbox as HTMLElement
      el.click()
    }
  }

  public handleInput(e: any) {
    this.$emit('input', set({ ...this.value }, this.name, e.target.checked))
  }
}

const CheckboxWrapper = styled.div`
  .checkbox {
    position: relative;
    padding-left: 42px;
    display: flex;
    flex-direction: row;
    align-items: center;
    min-height: 30px;

    .label {
      padding-left: 2px;

      &:before {
        position: absolute;
        left: 0;
        top: 0;
      }
    }

    input[type='checkbox'] {
      display: none;
    }

    .base-tooltip {
      align-self: start;
      margin-left: 5px;
      padding: 0;
    }
  }
`

export default Checkbox
