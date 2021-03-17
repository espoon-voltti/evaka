# Trusted SAML IdP certificates

Espoo AD (production):

- `espooad-internal-prod.pem`

Espoo AD (staging):

- `espooad-internal-staging.pem`

Voltti IDP (dev/test):

- `idp.test.espoon-voltti.fi.pem`

suomi.fi production environment:

- 2021: `saml-signing.idp.tunnistautuminen.suomi.fi.pem`

suomi.fi test environment:

- 2020: `saml-signing-testi.apro.tunnistus.fi.pem`

## Update list of trusted IdP certificates

1. Obtain URL for IdP metadata from the provider, for example:
    - Suomi.fi production: <https://tunnistus.suomi.fi/static/metadata/idp-metadata.xml>
    - Suomi.fi test: <https://testi.apro.tunnistus.fi/static/metadata/idp-metadata.xml>
    - Espoo AD production: <https://login.microsoftonline.com/6bb04228-cfa5-4213-9f39-172454d82584/federationmetadata/2007-06/federationmetadata.xml?appid=7d857df7-95fd-42f1-96e6-296c1094be09>
    - Espoo AD staging: <https://login.microsoftonline.com/6bb04228-cfa5-4213-9f39-172454d82584/federationmetadata/2007-06/federationmetadata.xml?appid=b73067a1-1f4c-4508-94ea-51c8eeb15793>
1. [Fetch](#fetch-saml-signing-certificates-from-metadata) certificate(s) from IdP's remote metadata
1. Update [code](./apigw/src/shared/certificates.ts) to include any new files
1. Update apigw deployment configuration to include the name of the new certificate file(s)
   - E.g. for Suomi.fi: `SFI_SAML_PUBLIC_CERT` (can contain multiple, comma-separated)

## Fetch SAML signing certificates from metadata

Requirements:

- Python 3.8 (recommended to use [pyenv](https://github.com/pyenv/pyenv))
- [pipenv](https://pipenv.pypa.io/en/latest/install/)

SAML 2.0 metadata (XML) can contain multiple entities (usually different environments) and those entities can contain
multiple signing certificates.

To fetch all signing certificate for an IdP's entity use the helper script in this directory:

```sh
# Install python dependencies
pipenv install
# Fetch and export the certificates from a IdP metadata URL:
pipenv run ./fetch-idp-certs.py <metadata url> [<entity ID>]
# For example:
pipenv run ./fetch-idp-certs.py https://tunnistus.suomi.fi/static/metadata/idp-metadata.xml
# -> will prompt you to select one of the SAML entities in the metadata and save all certificates for that entity
# or directly:
pipenv run ./fetch-idp-certs.py https://tunnistus.suomi.fi/static/metadata/idp-metadata.xml https://tunnistautuminen.suomi.fi/idp1
# -> will save all certificates for that entity
```
