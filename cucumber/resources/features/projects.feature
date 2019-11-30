Feature: Projects tests

    @IntegrationTest
    Scenario: retrieves projects
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
        When I request "/v1/projects" using HTTP GET
        Then the response code is 200