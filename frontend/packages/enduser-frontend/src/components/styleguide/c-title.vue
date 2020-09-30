<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<script>
  export default {
    name: 'c-title',

    props: {
      size: {
        type: Number,
        validator: (value) => value >= 0 && value <= 6,
        required: true
      },
      isGreyDark: {
        type: Boolean,
        default: false
      },
      textAlign: {
        type: String,
        default: '',
        validator: (value) => ['left', 'center', 'right', ''].includes(value)
      }
    },

    render(createElement) {
      return createElement(
        `h${this.size}`,
        {
          class: [
            `title is-${this.size} ${
              this.isGreyDark ? 'grey-dark' : ''
            } text-align-${this.textAlign}`
          ]
        },
        [this.$slots.default]
      )
    }
  }
</script>

<style lang="scss" scoped>
  .text-align {
    &-left {
      text-align: left;
    }
    &-center {
      text-align: center;
    }
    &-right {
      text-align: right;
    }
  }

  .grey-dark {
    color: $grey-dark;
  }
</style>
