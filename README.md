# Passulo Server

* reads Passulo tokens and displays them for browsers
* stores public keys
* validates token-signatures

## Available Routes

* `/.well-known/apple-app-site-association` returns the association to the Passulo iOS App
* `/` says hello
* `/?code=PASETO_TOKEN` parses the token
