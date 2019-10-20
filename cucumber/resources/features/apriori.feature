Feature: Apriori tests

    @IntegrationTest
    @AcceptanceTest
    Scenario: generates association rules
        Given the request body is:
        """
        {
          "name": "some project",
          "description": "a test project.",
          "window": 30,
          "min-support": 0.2,
          "min-confidence": 0.2
        }
        """
        When I request "/v1/project" using HTTP POST
        Then extract "data[0].id" as placeholder ":projectId"
        Given the request body is:
        """
        {
          "transactions": [
            ["a","b","c"],
            ["a","b","c"],
            ["a","b","c"],
            ["a","b"],
            ["a","b","c"],
            ["a","b","c","d","e"]
          ]
        }
        """
        When I request "/v1/project/:projectId/transactions" using HTTP POST
        Then extract "data[0].id" as placeholder ":jobId"
        Then start processing job ":jobId"
        Then wait "10" seconds

