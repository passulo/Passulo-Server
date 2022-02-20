# Passulo Server

* reads Passulo tokens and displays them for browsers
* stores public keys
* validates token-signatures

## Available Routes

* `/.well-known/apple-app-site-association` returns the association to the Passulo iOS App
* `/` says hello
* `/?code=<base64 encoded token>&sig=<base64 encoded signature>&kid=<keyid>` parses and verifies the token
