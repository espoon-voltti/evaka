<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div
    :class="{
      field: true,
      required: required,
      disabled: disabled
    }"
  >
    <label class="label" :for="id">
      {{ label }}
      <slot />
    </label>
    <div
      id="flatpickr-time-input"
      :class="{
        control: true,
        'has-icons-right': !!inputIcon
      }"
    >
      <flat-pickr
        v-model="datePickerValue"
        :config="config"
        @on-open="onOpen"
        :placeholder="placeholder"
        ref="flatPickrHiddenInput"
      >
      </flat-pickr>

      <span
        :class="[
          'clear-time',
          hasValue ? 'clear-time-show' : 'clear-time-hide'
        ]"
        @click="clearTime"
      >
        &Cross;
      </span>
    </div>
    <p :class="['help', state ? `is-${state}` : undefined]" v-show="message">
      {{ message }}
    </p>
  </div>
</template>

<script>
  import flatPickr from 'vue-flatpickr-component'
  import 'flatpickr/dist/flatpickr.css'
  import BaseInput from './base-input'
  const altInput = true

  export default {
    extends: BaseInput,
    components: {
      flatPickr
    },
    props: {
      value: {
        type: [String, Object],
        required: true
      },
      defaultHour: {
        type: Number,
        default: 8
      },
      defaultMinute: {
        type: Number,
        default: 0
      },
      minuteIncrement: {
        type: Number,
        default: 10
      }
    },
    data() {
      return {
        config: {
          allowInput: false,
          altFormat: 'H:i',
          altInput,
          dateFormat: 'H:i',
          defaultHour: this.defaultHour,
          enableTime: true,
          enableSeconds: false,
          minuteIncrement: this.minuteIncrement,
          noCalendar: true,
          time_24hr: true
        }
      }
    },
    computed: {
      datePickerValue: {
        get() {
          return this._inputValue
        },

        set(value) {
          this.$emit('input', this.getUpdateValue(value))
        }
      },
      hasValue() {
        return !_.isEmpty(this._inputValue)
      }
    },
    methods: {
      onOpen(date) {
        // The defaultHour is inserted into the textfield the first time
        // timepicker is opened --> save this value also to the model
        if (date && date.length === 0) {
          const hour =
            this.defaultHour < 10
              ? `0${this.defaultHour}`
              : `${this.defaultHour}`
          const minutes =
            this.defaultMinute < 10
              ? `0${this.defaultMinute}`
              : `${this.defaultMinute}`
          this.datePickerValue = `${hour}:${minutes}`
        }
      },
      clearTime() {
        this.$emit('input', this.getUpdateValue(null))
      }
    },
    // this is a workaround for flatpickr bug https://github.com/ankurk91/vue-flatpickr-component/issues/104
    // we need this if we are going to use labels for accessibility
    mounted() {
      if (altInput) {
        const flatPickrInputElement = this.$refs.flatPickrHiddenInput.getElem()
          .nextSibling
        flatPickrInputElement.id = this.id
        if (!this.label) {
          flatPickrInputElement.setAttribute('aria-label', this.placeholder)
        }
      }
    }
  }
</script>

<style lang="scss" scoped>
  .has-errors {
    .flatpickr-input {
      border-color: $color-error-border;
    }
  }

  .clear-time {
    cursor: pointer;
    font-size: 28px;
    padding: 0 20px;
    position: absolute;
    right: 20px;
    top: -4px;
    z-index: 2;

    &-show {
      visibility: visible;
    }
    &-hide {
      visibility: hidden;
    }
  }
</style>
