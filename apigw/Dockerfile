# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

ARG BASE_IMAGE=evaka-yarn:latest
FROM "${BASE_IMAGE}" AS builder

USER root

WORKDIR /project

COPY ./.yarn ./.yarn
COPY ./package.json ./yarn.lock ./.yarnrc.yml ./

RUN yarn install --immutable

COPY . .

RUN yarn build

FROM builder AS test

RUN yarn lint
RUN yarn test-ci

FROM "${BASE_IMAGE}"

WORKDIR /home/evaka

COPY --from=builder --chown=evaka:evaka /project .

ENV NODE_ENV production
RUN yarn workspaces focus --production \
 && yarn cache clean --all

ARG build=none
ARG commit=none

ENV APP_BUILD "$build"
ENV APP_COMMIT "$commit"

LABEL fi.espoo.build="$build" \
      fi.espoo.commit="$commit"

ENTRYPOINT ["./entrypoint.sh"]
CMD ["node", "dist/index.js"]
