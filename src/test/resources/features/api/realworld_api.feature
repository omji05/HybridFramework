# @api @realworld @epic-RW-100
# Feature: RealWorld Platform - User and Article Management API
#   As a RealWorld platform user
#   I want to manage my account and publish articles
#   So that I can interact with the RealWorld community
#
#   @smoke @registration @JIRA-RW-001
#   Scenario: Successfully register a new user
#     When I register a new user using the "register_user.json" payload
#     Then the response status code should be 201
#     And the response body should match the "user-response.json" schema
#     And the response should contain a valid auth token
#
#   @smoke @auth @JIRA-RW-002
#   Scenario: Successfully log in with valid credentials
#     When I log in using the "login_user.json" payload
#     Then the response status code should be 200
#     And the response body should match the "user-response.json" schema
#     And the response should contain a valid auth token
#     And the auth token should be stored for subsequent requests
#
#   @regression @articles @JIRA-RW-003
#   Scenario: Create a new article as an authenticated user
#     Given I am authenticated using the "login_user.json" payload
#     When I create an article using the "create_article.json" payload
#     Then the response status code should be 201
#     And the response body should match the "article-response.json" schema
#     And the response should contain a valid article slug
#
#   @regression @e2e @JIRA-RW-004
#   Scenario: End-to-end - Authenticate, publish and retrieve an article
#     Given I am authenticated using the "login_user.json" payload
#     When I create an article using the "create_article.json" payload
#     Then the response status code should be 201
#     And the response body should match the "article-response.json" schema
#     And the article slug should be stored for subsequent requests
#     When I fetch the article using the stored slug
#     Then the response status code should be 200
#     And the response body should match the "article-response.json" schema
#     And the response should contain a valid article slug
#
#   @regression @negative @JIRA-RW-005
#   Scenario Outline: Login is rejected for invalid or incomplete credentials
#     When I attempt to log in using the "login_user.json" payload with email "<email>" and password "<password>"
#     Then the response status code should be <status>
#     And the API error response should be present
#
#     Examples: Both invalid
#       | email             | password  | status |
#       | bad@example.com   | wrongpass | 401    |
#
#     Examples: Missing credential(s)
#       | email                   | password  | status |
#       | <missing>               | fixscal   | 422    |
#       | nijik45957@fixscal.com  | <missing> | 422    |
#       | <missing>               | <missing> | 422    |
#
#     Examples: Empty string credential(s)
#       | email                   | password | status |
#       | <empty>                 | fixscal  | 422    |
#       | nijik45957@fixscal.com  | <empty>  | 422    |
#       | <empty>                 | <empty>  | 422    |
#
#   @regression @json-mutation @JIRA-RW-006
#   Scenario: Mutate complex template using mixed object/array paths
#     When I post a complex payload using the "complex_request_template.json" payload with JSON overrides
#       | path                                 | value              |
#       | customer.profile.email              | mutated@example.com |
#       | customer.preferences.channels[0]   | push               |
#       | shippingAddresses[1].city         | Mumbai             |
#       | orders[0].items[0].sku            | SKU-UPDATED       |
#       | orders[0].items[1].attributes.color | <empty>            |
#       | tags[2]                             | smoke              |
#       | description                         | this is a sample description              |
#       | notes                                | <null>          |
#       | version                              | 2                  |
#       | amount                               | 199.99             |
#       | isActive                             | false              |
#       | flags[1]                             | true               |
#       | scores[2]                            | 4.2                |
#       | metadata                             | <missing>         |
#     And the API error response should be present
