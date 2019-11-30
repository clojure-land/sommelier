Feature: Default tests

  @SmokeTest
  Scenario: performs health check
    Given the request
    When I request "/v1/health" using HTTP GET
    Then the response code is 200

  @IntegrationTest
  Scenario Outline: handles unauthorized attempts
    Given the request
    When I request "<uri>" using HTTP <method>
    Then the response code is 403
    Examples:
      | uri                                   | method |
      | /v1/projects                          | GET    |
      | /v1/project                           | POST   |
#      | /v1/project/5dcc0a638b7ff23914907226  | POST   |
#      | /v1/project/5dcc0a638b7ff23914907226  | GET    |
#      | /v1/project/5dcc0a638b7ff23914907226  | DELETE |
