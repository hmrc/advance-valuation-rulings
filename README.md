
# Advance Valuation Ruling Backend

This service provides the backend for the Advance Valuation Ruling Frontend.

## Frontend

[Advance Valuation Rulings Frontend](https://github.com/hmrc/advance-valuation-rulings-frontend)

## Persistence
This service uses mongodb to persist user answers.

## Requirements
This service is written in Scala using the Play framework, so needs at least a JRE to run.

JRE/JDK 11 is recommended.

The service also depends on mongodb.

## Running the service
Using service manager (sm or sm2)
Use the ARS_ALL profile to bring up all services using the latest tagged releases
``sm2 --start ARS_ALL``

Run `sm2 -s` to check what services are running.

## Launching the service locally
To bring up the service on the configured port 12600, use
``sbt run``.

## Testing the service
This service uses sbt-scoverage to provide test coverage reports.

## Scalafmt
To prevent formatting failures in a GitHub pull request,
run the command ``sbt scalafmtAll`` before pushing to the remote repository.
