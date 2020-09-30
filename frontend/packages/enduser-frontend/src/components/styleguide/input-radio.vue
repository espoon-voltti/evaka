<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <label
    class="radio"
    :class="{ active: isActive, disabled: disabled }"
    tabindex="0"
  >
    <input
      type="radio"
      class="input"
      v-model="radioValue"
      :value="inputValue"
      :name="inputValue"
      :disabled="disabled"
      tabindex="-1"
    />
    <div class="label">
      <span class="tick">
        <font-awesome-icon :icon="['far', 'check']"></font-awesome-icon>
      </span>
      <span>
        {{ label }}
        <c-in v-if="instruction" :instruction="instruction" />
      </span>
      <span class="description">
        {{ description }}
      </span>
    </div>
  </label>
</template>

<script>
  export default {
    props: [
      'value',
      'inputValue',
      'label',
      'disabled',
      'description',
      'instruction'
    ],
    computed: {
      radioValue: {
        get() {
          return this.value
        },
        set() {
          this.$emit('change', this.inputValue)
        }
      },
      isActive() {
        return this.value === this.inputValue
      }
    }
  }
</script>

<style lang="scss" scoped>
  .radio {
    .label {
      display: flex;
      min-height: 35px;

      &:before {
        position: absolute;
        left: 0px;
        top: 0px;
      }
    }
  }
</style>
