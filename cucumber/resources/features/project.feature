Feature: Project tests

    @IntegrationTest
    Scenario: create project
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project" using HTTP POST
        Then the response code is 201
        Then the response body matches schema:
        """
        {
          "meta": {
            "status": "created",
            "timestamp": "@string",
            "request_id": "@string"
          },
          "data": [
            {
              "type": "project",
              "id": "@string",
              "attributes": {
                "name": "some project",
                "description": "a test project",
                "window": 0,
                "min-support": 0,
                "min-confidence": 0
              }
            }
          ]
        }
        """

    @IntegrationTest
    Scenario: get project
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project" using HTTP POST
        Then extract "data[0].id" as placeholder ":projectId"
        When I request "/v1/project/:projectId" using HTTP GET
        Then the response code is 200

    @IntegrationTest
    Scenario: modify a project
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project",
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
          "description": "an important test project",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project/:projectId" using HTTP POST
        Then the response code is 200

    @IntegrationTest
    Scenario: delete projects
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project",
          "window": 0,
          "min-support": 0,
          "min-confidence": 0
        }
        """
        When I request "/v1/project" using HTTP POST
        Then extract "data[0].id" as placeholder ":projectId"
        When I request "/v1/project/:projectId" using HTTP DELETE
        Then the response code is 204

    @IntegrationTest
    Scenario: append transactions to project
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project",
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
