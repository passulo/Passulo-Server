# Quick Start for a Server with Database in Scala

uses / includes

* [akka-http](https://github.com/akka/akka-http) as server-framework
* [slick](https://github.com/slick/slick/) to access the database
* [circe](https://github.com/circe/circe) to serialize to JSON and back
* PostgreSQL as Database
* GitHub Actions and CircleCI definition for CI/CD
* [Scalafmt](https://scalameta.org/scalafmt/) for formatting
* a lot of sane, tried-and-tested defaults

## Example usage

Spin up a PostgreSQL database, for example:

```shell
$ brew install postgresql
$ brew services start postgresql
```

Run `sbt`, then use `reStart` (from https://github.com/spray/sbt-revolver) to run it (without blocking the console):

```shell
$ sbt
[info] welcome to sbt 1.5.7 (Homebrew Java 11.0.12)
[info] loading global …
[info] …
[info] started sbt server

sbt:scala-start> reStart
[info] compiling 2 Scala sources...
[info] Application root not yet started
[info] Starting application root in the background ...
root Starting Main.main()
[success] Total time: 10 s
root [INFO ] Slf4jLogger started
root [INFO ] Server online at http://0:0:0:0:0:0:0:0:8080/

sbt:scala-start>
```

Talk to the server:

```shell
$ curl -X POST localhost:8080 -d '{"operation":"add","value1":12,"value2":13}' -H "content-type: application/json"
{"result":25}

$ curl -X POST localhost:8080 -d '{"operation":"multiply","value1":12,"value2":13}' -H "content-type: application/json"
{"result":156}

$ curl -X POST localhost:8080 -d '{"operation":"subtract","value1":12,"value2":13}' -H "content-type: application/json"
"Not suported"

$ curl localhost:8080 -s | jq
[
  {
    "timestamp": "2021-12-19T13:28:20.310609Z",
    "operation": "add",
    "value1": 12,
    "value2": 13,
    "result": "25"
  },
  {
    "timestamp": "2021-12-19T14:12:11.484108Z",
    "operation": "multiply",
    "value1": 12,
    "value2": 13,
    "result": "156"
  },
  {
    "timestamp": "2021-12-19T14:12:16.253103Z",
    "operation": "subtract",
    "value1": 12,
    "value2": 13,
    "result": "Not suported"
  }
```

## Dependencies

This project is automatically updated by [scala-steward](https://github.com/fthomas/scala-steward)) 🚀
