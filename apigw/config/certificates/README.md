# Trusted SAML IdP certificates

suomi.fi production environment:

- 2024: `saml-signing.idp.tunnistautuminen.suomi.fi.2024.pem`

suomi.fi test environment:

- 2024: `saml-signing-testi.apro.tunnistus.fi.2024.pem`


## Update list of trusted IdP certificates

1. Obtain URL for IdP metadata from the provider, for example:
    - Suomi.fi production: <https://tunnistus.suomi.fi/static/metadata/idp-metadata.xml>
    - Suomi.fi test: <https://static.apro.tunnistus.fi/static/metadata/idp-metadata.xml>
1. [Fetch](#fetch-saml-signing-certificates-from-metadata) certificate(s) from IdP's remote metadata
1. Update [code](https://github.com/espoon-voltti/evaka/blob/master/apigw/src/shared/certificates.ts) to include any new files
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
