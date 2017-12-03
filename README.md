# Apriori


## Requrirments 
* [Docker](https://www.docker.com/)
* [Gradle](https://gradle.org/) 
* [Leiningen](https://leiningen.org/)

## Installation

```bash
// start
gradle build run

// stop
gradle stop
```

# Usage

## Api

## Cli
```bash

apriori api --start --port=PORT

apriori api --stop

apriori data-set --upload=PATH --product=ID

apriori data-set --empty --product=ID

apriori project --create --name=NAME

apriori project --update=ID --name=NAME

apriori project --delete=ID
``` 