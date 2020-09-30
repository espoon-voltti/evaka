<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div>
    <label
      class="checkbox"
      :class="{ active: value }"
      tabindex="0"
      role="checkbox"
      :aria-checked="value"
      @keyup.enter="handleKeyUp"
    >
      <input
        type="checkbox"
        :checked="value"
        @change="valueChanged"
        tabindex="-1"
        ref="checkbox"
      />
      {{ label }}<span v-if="label && !noLabelDots">:</span>
      <slot></slot>
    </label>
  </div>
</template>

<script>
  export default {
    props: {
      value: Boolean,
      name: String,
      label: String,
      noLabelDots: Boolean
    },
    methods: {
      valueChanged(event) {
        this.$emit('input', event.target.checked)
      },
      handleKeyUp(e) {
        const el = this.$refs.checkbox
        el.click()
      }
    }
  }
</script>

<style lang="scss" scoped>
  .checkbox {
    display: inline-block;
  }
</style>
