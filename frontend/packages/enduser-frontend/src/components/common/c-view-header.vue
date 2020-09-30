<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="view-header">
    <c-title :size="2">{{ title }}</c-title>
    <c-text-content :asHtml="true" :text="text" />
  </div>
</template>

<script>
  export default {
    props: {
      title: String,
      text: [String, Array]
    }
  }
</script>

<style lang="scss" scoped>
  .view-header {
    margin-bottom: 3rem;
  }

  .view-headerText-item {
    &:not(:first-of-type) {
      margin-top: 3rem;
    }
  }
</style>
