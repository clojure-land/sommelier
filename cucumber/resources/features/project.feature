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
        When the "Authorization" header is "Bearer :token"
        And I request "/v1/project" using HTTP POST
        Then the response code is 201
        Then the response body attribute "data[0].attributes.name" = "some project"
        Then the response body attribute "data[0].attributes.description" = "a test project."
        Then the response body attribute "data[0].attributes.window" = "0"
        Then the response body attribute "data[0].attributes.min-support" = "0"
        Then the response body attribute "data[0].attributes.min-confidence" = "0"

#    @IntegrationTest
#    Scenario Outline: handles bad requests when attempting to create a project
#        Given the request body is:
#        """
#        {
#          "name": "<name>",
#          "description": "<description>",
#          "window": <window>,
#          "min-support": <min-support>,
#          "min-confidence": <min-confidence>
#        }
#        """
#        When I request "/v1/project" using HTTP POST
#        Then the response code is 400
#        Then the response body attribute "data" contains "<fields>"
#        Examples:
#            | fields            | name           | description            | window | min-support | min-confidence |
#            | name              | 111            | "valid description."   | 0      | 0           | 0              |
#            | name              | "invalid-name" | "valid description."   | 0      | 0           | 0              |
#            | description       | "valid name"   | 111                    | 0      | 0           | 0              |
#            | description       | "valid name"   | "invalid-description." | 0      | 0           | 0              |
#            | window            | "valid name"   | "valid description."   | 100    | 0           | 0              |
#            | min-support       | "valid name"   | "valid description."   | 0      | 2           | 0              |
#            | min-confidence    | "valid name"   | "valid description."   | 0      | 0           | 2              |

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
        And the "Authorization" header is "Bearer :token"
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
        And the "Authorization" header is "Bearer :token"
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
        And the "Authorization" header is "Bearer :token"
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
        And the "Authorization" header is "Bearer :token"
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
        And the "Authorization" header is "Bearer :token"
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
        And the "Authorization" header is "Bearer :token"
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
        And the "Authorization" header is "Bearer :token"
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
        And the "Authorization" header is "Bearer :token"
        When I request "/v1/project/:projectId/transactions" using HTTP POST
        Then the response code is 202
        Then the response body attribute "data[0].attributes.project-id" = ":projectId"
        Then the response body attribute "data[0].attributes.state" = "scheduled"
        Then the response body attribute "data[0].attributes.transactions" = "9"

    @IntegrationTest
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
        And the "Authorization" header is "Bearer :token"
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
        And the "Authorization" header is "Bearer :token"
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
        And the "Authorization" header is "Bearer :token"
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
        And the "Authorization" header is "Bearer :token"
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
        And the "Authorization" header is "Bearer :token"
        When I request "/v1/project/:projectId/transactions" using HTTP POST
        Then extract "data[0].id" as placeholder ":jobId"
        When I request "/v1/project/:projectId/jobs" using HTTP GET
        Then the response code is 200
        Then the response body attribute "data[0].id" = ":jobId"
        Then the response body attribute "data[0].attributes.project-id" = ":projectId"
        Then the response body attribute "data[0].attributes.state" = "scheduled"
        Then the response body attribute "data[0].attributes.transactions" = "1"