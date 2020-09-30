<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <validation :name="label" :value="value" :validators="getValidators">
    <div class="form-field">
      <label
        for="phoneNumber"
        class="label"
        :class="{ 'input-active': active }"
      >
        {{ label }}<span v-if="label">:</span> <span v-show="required">*</span>
      </label>
      <p class="control" :class="{ 'has-icons-left': icon }">
        <input
          class="input"
          name="phoneNumber"
          id="phoneNumber"
          v-model="inputValue"
          type="tel"
          placeholder="+358401234567"
          @focus="active = true"
          @blur="handleBlur"
          :validators="required + 'phoneNumber'"
          aria-required="true"
        />
        <span v-if="icon" class="icon is-small is-left">
          <font-awesome-icon :icon="['far', icon]"></font-awesome-icon>
        </span>
      </p>
    </div>
  </validation>
</template>

<script>
  import Validation from '@/components/validation/validation.vue'
  import {
    required as requiredValidator,
    phoneNumber,
    isValidPhoneNumber
  } from '@/components/validation/validators.js'

  export default {
    data() {
      return {
        active: false
      }
    },
    components: {
      Validation
    },
    props: {
      value: String,
      required: {
        type: Boolean,
        default: false
      },
      icon: {
        type: String,
        default: 'mobile'
      },
      validators: {
        type: Array,
        default: () => [phoneNumber]
      },
      placeholder: {
        type: String,
        default: '+358401234567'
      },
      label: {
        type: String,
        default: 'Puhelinnumero'
      }
    },
    computed: {
      getValidators() {
        return this.required
          ? [requiredValidator, ...this.validators]
          : this.validators
      },

      inputValue: {
        get() {
          return this.value
        },

        set(value) {
          if (isValidPhoneNumber(value)) {
            this.$emit('input', value)
          }
        }
      }
    },
    methods: {
      handleBlur(e) {
        this.active = false
        this.$emit('input', e.target.value)
      }
    }
  }
</script>
