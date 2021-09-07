ARG PLAYWRIGHT_VERSION=v1.14.1

FROM mcr.microsoft.com/playwright:${PLAYWRIGHT_VERSION}-focal

RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.38.0/install.sh | bash \
 && . "$HOME/.nvm/nvm.sh" \
 && nvm install 14.15

RUN curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - \
 && echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list \
 && apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get --yes install --no-install-recommends libgbm1 yarn='1.22.*' sudo \
 && rm -rf $HOME/.cache/pip /var/lib/apt/lists/*

COPY ./playwright/bin/run-tests.sh ./entrypoint.sh /bin/

#ENTRYPOINT ["/bin/entrypoint.sh"]
CMD ["/bin/run-tests.sh"]
