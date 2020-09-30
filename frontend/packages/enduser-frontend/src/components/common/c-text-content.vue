<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="text-content">
    <div v-if="asHtml">
      <p
        class="text-content-item"
        v-for="(t, index) in viewText"
        :key="index"
        v-html="t"
      ></p>
    </div>
    <div v-else>
      <p class="text-content-item" v-for="(t, index) in viewText" :key="index">
        {{ t }}
      </p>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      text: [String, Array],
      asHtml: {
        type: Boolean,
        default: false
      }
    },

    computed: {
      viewText() {
        return Array.isArray(this.text) ? this.text : [this.text]
      }
    }
  }
</script>

<style lang="scss" scoped>
  .text-content {
    margin-bottom: 3rem;
  }

  .text-content-item {
    &:not(:first-of-type) {
      margin-top: 2rem;
    }
  }
</style>
