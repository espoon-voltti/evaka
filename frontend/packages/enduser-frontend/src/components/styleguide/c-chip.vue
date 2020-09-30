<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div
    class="c-chip"
    @click="onClick"
    @keyup.enter="onClick"
    :class="{ 'c-chip-selected': isSelected }"
    tabindex="0"
  >
    <font-awesome-icon
      :icon="['fal', 'check']"
      v-if="isSelected"
    ></font-awesome-icon>
    <span>{{ text }}</span>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'

  export default Vue.extend({
    name: 'c-chip',
    props: {
      value: {
        type: Array,
        required: true
      },
      text: {
        type: String,
        required: true
      },
      inputValue: {
        type: String,
        required: true
      }
    },
    computed: {
      isSelected(): boolean {
        return (this as any).value.includes((this as any).inputValue)
      }
    },
    methods: {
      onClick(): void {
        let values
        if ((this as any).isSelected) {
          values = (this as any).value.filter(
            (val) => val !== (this as any).inputValue
          )
        } else {
          values = Array.from((this as any).value)
          values.push((this as any).inputValue)
        }
        ;(this as any).$emit('input', values)
      }
    }
  })
</script>

<style lang="scss">
  .c-chip {
    align-items: center;
    border: 1px solid #487ac8;
    border-radius: 30px;
    color: #487ac8;
    cursor: pointer;
    display: flex;
    font-size: 14px;
    font-weight: 600;
    height: 30px;
    justify-content: center;
    min-width: 100px;
    padding: 0 1rem;
    text-align: center;
  }

  .c-chip-selected {
    background-color: #487ac8;
    color: white;
    justify-content: space-between;
  }
</style>
