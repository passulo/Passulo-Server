# Passulo Server

* reads Passulo tokens and displays them for browsers
* stores public keys
* validates token-signatures
* provides an API for programmatic access

## Available Routes

* `/` says hello
* `/.well-known/apple-app-site-association` returns the association to the Passulo iOS App
* `/?code=<base64 encoded token>&v=1&sig=<base64 encoded signature>&kid=<keyid>` parses and verifies the token
* `/v1/` provides a json-api for programmatic access:
  * `/v1/keys` lists all available keys
  * `/v1/key/<public-key-id>` returns the Public Key (base64 encoded) for the given KeyID
  * `/v1/allowed-associations-for-key-id/<public-key-id>` returns names of the associations this key is allowed to sign

## Public Deployment

For the public Passulo instance, this server is deployed at [app.passulo.com](https://app.passulo.com).

## Build

To build, run

```shell
sbt docker:stage
docker build . -t ghcr.io/passulo/passulo-server:latest --platform linux/amd64
docker push ghcr.io/passulo/passulo-server:latest
````

Docker image at https://github.com/orgs/passulo/packages/container/package/passulo-server

## Docs & more

See [passulo.com](https://www.passulo.com) for more information.
