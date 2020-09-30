// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mount, Wrapper } from '@vue/test-utils'

import cInput from './c-input'
import { required } from '../validation/input-validation'

describe('form', () => {
  let w: Wrapper<cInput>
  let model: { value: { test: string } }

  beforeEach(() => {
    model = {
      value: {
        test: 'test input value'
      }
    }

    w = mount(cInput, {
      propsData: {
        name: 'value.test',
        value: model,
        required: true
      },
      stubs: {
        FontAwesomeIcon: true
      }
    })
  })

  it('should mount and display given values ', () => {
    const input = w.find('input').element as HTMLInputElement
    expect(input.value).toEqual('test input value')
  })

  it('should change model value, when input is changed', () => {
    const input = w.find('input').element as HTMLInputElement
    input.value = 'value'

    w.vm.handleInput({ target: { value: input.value } })
    expect(model.value.test).toEqual(input.value)
  })

  it('should add required validator, when required attribute is defined', () => {
    expect(w.vm.inputValidators.indexOf(required)).toBeGreaterThan(-1)
  })

  // it('should display error message, when input is flagged as touched', () => {
  //   w.vm.$t = (t) => t
  //   w.vm.touched = true

  //   expect(w.vm.message.length).toBeGreaterThan(1)
  // })
})
