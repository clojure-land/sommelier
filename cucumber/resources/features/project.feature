Feature: Project tests

    @IntegrationTest
    Scenario: creates a project
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project.",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project" using HTTP POST
        Then the response code is 201
        Then the response body attribute "data[0].attributes.name" = "some project"
        Then the response body attribute "data[0].attributes.description" = "a test project."
        Then the response body attribute "data[0].attributes.window" = "0"
        Then the response body attribute "data[0].attributes.min-support" = "0"
        Then the response body attribute "data[0].attributes.min-confidence" = "0"

    @IntegrationTest
    Scenario: retrieves a project
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project.",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project" using HTTP POST
        Then extract "data[0].id" as placeholder ":projectId"
        When I request "/v1/project/:projectId" using HTTP GET
        Then the response code is 200
        Then the response body attribute "data[0].id" = ":projectId"
        Then the response body attribute "data[0].attributes.name" = "some project"
        Then the response body attribute "data[0].attributes.description" = "a test project."
        Then the response body attribute "data[0].attributes.window" = "0"
        Then the response body attribute "data[0].attributes.min-support" = "0"
        Then the response body attribute "data[0].attributes.min-confidence" = "0"

    @IntegrationTest
    Scenario: modifies a project
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project.",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project" using HTTP POST
        Then extract "data[0].id" as placeholder ":projectId"
        Given the request body is:
        """
        {
          "name": "some important project",
          "description": "an important test project.",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project/:projectId" using HTTP POST
        Then the response code is 200
        Then the response body attribute "data[0].id" = ":projectId"
        Then the response body attribute "data[0].attributes.name" = "some important project"
        Then the response body attribute "data[0].attributes.description" = "an important test project."
        Then the response body attribute "data[0].attributes.window" = "0"
        Then the response body attribute "data[0].attributes.min-support" = "0"
        Then the response body attribute "data[0].attributes.min-confidence" = "0"

    @IntegrationTest
    Scenario: deletes a project
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project.",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project" using HTTP POST
        Then extract "data[0].id" as placeholder ":projectId"
        When I request "/v1/project/:projectId" using HTTP DELETE
        Then the response code is 204
        When I request "/v1/project/:projectId" using HTTP GET
        Then the response code is 404

    @IntegrationTest
    Scenario: appends transaction to project
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project.",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project" using HTTP POST
        Then extract "data[0].id" as placeholder ":projectId"
        Given the request body is:
        """
        {
          "transactions": [
            ["a","b","c"]
          ]
        }
        """
        When I request "/v1/project/:projectId/transactions" using HTTP POST
        Then the response code is 202
        Then the response body attribute "data[0].attributes.project-id" = ":projectId"
        Then the response body attribute "data[0].attributes.state" = "scheduled"
        Then the response body attribute "data[0].attributes.transactions" = "1"

    @IntegrationTest
    Scenario: appends transactions to project
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project.",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project" using HTTP POST
        Then extract "data[0].id" as placeholder ":projectId"
        Given the request body is:
        """
        {
          "transactions": [
            ["a","b","c"],
            ["d","e","f"],
            ["g","h","i"],
            ["j","k","l"],
            ["m","n","o"],
            ["p","q","r"],
            ["s","t","u"],
            ["v","w","x"],
            ["y","z"]
          ]
        }
        """
        When I request "/v1/project/:projectId/transactions" using HTTP POST
        Then the response code is 202
        Then the response body attribute "data[0].attributes.project-id" = ":projectId"
        Then the response body attribute "data[0].attributes.state" = "scheduled"
        Then the response body attribute "data[0].attributes.transactions" = "9"

    @IntegrationTest
    @RegressionTest
    Scenario: updates an existing transaction
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project.",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project" using HTTP POST
        Then extract "data[0].id" as placeholder ":projectId"
        Given the request body is:
        """
        {
          "transactions": [
            ["a","b","c"]
          ]
        }
        """
        When I request "/v1/project/:projectId/transactions" using HTTP POST
        Then the response code is 202
        Then the response body attribute "data[0].attributes.transactions" = "1"
        Given the request body is:
        """
        {
          "transactions": [
            ["a","b","c"]
          ]
        }
        """
        When I request "/v1/project/:projectId/transactions" using HTTP POST
        Then the response code is 202
        Then the response body attribute "data[0].attributes.transactions" = "2"

    @IntegrationTest
    Scenario: retrieves a job
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project.",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project" using HTTP POST
        Then extract "data[0].id" as placeholder ":projectId"
        Given the request body is:
        """
        {
          "transactions": [
            ["a","b","c"]
          ]
        }
        """
        When I request "/v1/project/:projectId/transactions" using HTTP POST
        Then extract "data[0].id" as placeholder ":jobId"
        When I request "/v1/project/:projectId/jobs" using HTTP GET
        Then the response code is 200
        Then the response body attribute "data[0].id" = ":jobId"
        Then the response body attribute "data[0].attributes.project-id" = ":projectId"
        Then the response body attribute "data[0].attributes.state" = "scheduled"
        Then the response body attribute "data[0].attributes.transactions" = "1"