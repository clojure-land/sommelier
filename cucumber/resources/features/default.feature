Feature: Default tests

  @SmokeTest
  Scenario: get health check
    Given the request
    When I request "/v1/health" using HTTP GET
    Then the response code is 200
