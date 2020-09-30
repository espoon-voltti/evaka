<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div
    :class="{
      field: true,
      'field-datepicker': true,
      'is-horizontal': horizontal,
      'is-vertical': !horizontal,
      required: required
    }"
  >
    <label :for="id" v-if="label">
      {{ label }}
      <slot />
    </label>
    <div class="control">
      <div :class="['date-picker-container', { border: border }]">
        <v-date-picker
          class="date-picker"
          mode="single"
          :min-date="getMinDate"
          :max-date="getMaxDate"
          :disabled-dates="getDisabledDays"
          v-model="datePickerValue"
          popover-visibility="focus"
          popoverExpanded
          popoverDirection="top"
          show-caps
          :isRequired="required"
          :show-day-popover="false"
          :theme-styles="datePickerStyles"
        >
          <span class="date-input-wrapper">
            <span class="icon is-right" v-if="iconRight">
              <font-awesome-icon
                :icon="['far', 'calendar']"
              ></font-awesome-icon>
            </span>
            <masked-input
              :id="id"
              type="text"
              :class="[
                'date-input',
                'date-picker-input',
                'input',
                state ? `is-${state}` : undefined
              ]"
              :value="formattedInputDate"
              :placeholder="
                placeholder !== null
                  ? placeholder
                  : $t('general.input.pick-date')
              "
              @input="onManualInput"
              :mask="getMask"
              :guide="true"
              :keepCharPositions="true"
              :aria-label="
                placeholder !== null
                  ? placeholder
                  : $t('general.input.pick-date-aria-label')
              "
            >
              <!--
              Doesn't yet support placeholder with different chars (e.g pp.kk.vvvv):
              https://github.com/text-mask/text-mask/issues/316
              -->
            </masked-input>
            <span class="icon is-left" v-if="iconLeft">
              <font-awesome-icon
                :icon="['fal', 'calendar']"
              ></font-awesome-icon>
            </span>
          </span>
        </v-date-picker>
        <span
          :class="[
            'clear-dates',
            hasValue ? 'clear-dates-show' : 'clear-dates-hide'
          ]"
          @click="clearDate"
        >
          <font-awesome-icon :icon="['fal', 'times']"></font-awesome-icon>
        </span>
      </div>
    </div>
    <p v-show="message" :class="['help', state ? `is-${state}` : undefined]">
      {{ $t('validation.errors.required-field') }}
    </p>
  </div>
</template>

<script>
  import { setupCalendar } from 'v-calendar'
  import { formatDate } from '@/utils/date-utils'
  import { parseTzDate } from '@evaka/lib-common/src/utils/date'
  import parse from 'date-fns/parse'
  import {
    DATE_PICKER_STYLE,
    DATE_FORMAT,
    DATE_FORMAT_MASK,
    DATE_FORMAT_REGEX
  } from '@/constants'
  import BaseInput from './base-input'
  import MaskedInput from 'vue-text-mask'

  export default {
    extends: BaseInput,
    components: {
      MaskedInput
    },
    props: {
      required: {
        type: Boolean,
        default: false
      },
      placeholder: {
        type: String,
        default: null
      },
      horizontal: {
        type: Boolean,
        default: false
      },
      iconLeft: {
        type: Boolean,
        default: false
      },
      iconRight: {
        type: Boolean,
        default: false
      },
      border: {
        type: Boolean,
        default: false
      },
      minDate: {
        type: String,
        default: null
      },
      maxDate: {
        type: String,
        default: null
      },
      disableWeekendDays: {
        type: Boolean,
        default: false
      },
      locale: {
        type: String,
        default: 'fi'
      }
    },
    computed: {
      datePickerValue: {
        get() {
          return this._inputValue !== null
            ? new Date(this._inputValue)
            : new Date()
        },
        set(value) {
          this.$emit('input', this.getUpdateValue(parseTzDate(value)))
        }
      },
      getMask() {
        return DATE_FORMAT_MASK
      },
      datePickerStyles() {
        return DATE_PICKER_STYLE
      },
      formattedInputDate() {
        return formatDate(this._inputValue, DATE_FORMAT) || ''
      },
      getMinDate() {
        return this.minDate ? new Date(this.minDate) : null
      },
      getMaxDate() {
        return this.maxDate ? new Date(this.maxDate) : null
      },
      getDisabledDays() {
        return this.disableWeekendDays ? { weekdays: [1, 7] } : null
      },
      hasValue() {
        return !_.isEmpty(this._inputValue)
      }
    },
    methods: {
      clearDate() {
        this.$emit('input', this.getUpdateValue(null))
      },
      onManualInput(value) {
        // TODO : Improve check
        // It is now checked that the user types in "dd.MM.yyyy" format
        if (value && value.length === 10 && value.match(DATE_FORMAT_REGEX)) {
          this.datePickerValue = parse(value, DATE_FORMAT, new Date())
        }
      }
    },
    watch: {
      // This is a "hack" to enforce the new locale into v-date-picker
      locale: {
        handler(val, oldVal) {
          setupCalendar({
            locale: val
          })
        },
        immediate: true
      }
    }
  }
</script>

<style lang="scss" scoped>
  .field-datepicker {
    display: flex;
    align-items: center;

    .date-picker-container {
      background-color: $white;
      display: flex;
      position: relative;
      width: 100%;

      .date-picker {
        width: 100%;
      }
    }

    &.is-vertical {
      flex-direction: column;
      align-items: normal;
    }

    &.is-horizontal {
      flex-direction: row;
      justify-content: flex-start;
      margin-top: 1.4rem;
    }
  }

  label {
    color: $color-form-label;
    font-weight: 500;
    padding-right: 2rem;
  }

  .icon {
    align-self: center;
    color: $grey-lighter;
  }

  .icon,
  .is-right {
    margin-left: 0.4rem;
  }

  .control {
    display: flex;
  }

  .clear-dates {
    cursor: pointer;
    font-size: 22px;
    padding: 0 0.75rem;
    position: absolute;
    right: 4px;
    z-index: 1;

    &-show {
      visibility: visible;
    }
    &-hide {
      visibility: hidden;
    }
  }

  .date-picker-input {
    padding: 5px 0 5px 9px;
    width: 10rem;
  }

  .border {
    border-bottom: 1px solid $grey-light;

    input {
      border: none;
    }
  }

  p.help {
    padding-right: 0 !important;
    position: relative;
  }

  .date-input-wrapper {
    display: flex;
  }
</style>
