package com.sommelier;

import com.mongodb.MongoClient;
import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

public class StepDefinitions
{
    private static final String TOKEN = Helper.RequestToken();
    private static final String CONTENT_TYPE = "application/json";

    private RequestSpecification when;
    private ValidatableResponse then;

    private String body;
    private Map<String, String> headers = new HashMap<>();
    private HashMap<String, Object> attributes = new HashMap<>();


    @Before
    public void before() {
        this.body = null;
        this.headers.clear();
        this.attributes.clear();
    }

    @After
    public void after() throws UnknownHostException {
        MongoClient mongoClient = new MongoClient( "localhost");
        mongoClient.getDatabaseNames().forEach(db -> {
            if(db.equals("sommelier") || db.contains("job_")) {
                mongoClient.getDB(db).dropDatabase();
            }
        });
    }

    @Given("^the request$")
    public void the_request() {
        Helper.configureRestAssured();
        when = Optional.ofNullable(when).orElse(given());
    }

    @Given("the request body is:")
    public void the_request_body_is(String body) {
        this.the_request();
        this.body = body;
    }

    @Given("the {string} header is {string}")
    public void the_header_is(String header, String value) {
        this.attributes.put(":token", TOKEN);
        this.headers.put(header, Helper.replacePlaceholders(this.attributes, value));
    }

    @When("I request {string} using HTTP GET")
    public void i_request_using_HTTP_GET(String uri) {
        then = given()
                .contentType(CONTENT_TYPE)
                .headers(this.headers)
                .when()
                .get(Helper.replacePlaceholders(this.attributes, uri))
                .then();
    }

    @When("I request {string} using HTTP POST")
    public void i_request_using_HTTP_POST(String uri) {
        then = given()
                .contentType(CONTENT_TYPE)
                .headers(this.headers)
                .body(this.body)
                .when()
                .post(Helper.replacePlaceholders(this.attributes, uri))
                .then();
    }

    @When("I request {string} using HTTP DELETE")
    public void i_request_using_HTTP_DELETE(String uri) {
        then = given()
                .contentType(CONTENT_TYPE)
                .headers(this.headers)
                .when()
                .delete(Helper.replacePlaceholders(this.attributes, uri))
                .then();
    }

    @Then("extract {string} as placeholder {string}")
    public void extract_as_placeholder(String path, String as) {
        this.attributes.put(as, then.extract().path(path).toString());
    }

    @Then("the response code is (\\d+)")
    public void the_response_code_is(int status) {
        then.statusCode(status);
    }

    @Then("the response body attribute {string} = {string}")
    public void response_body_equals(String path, String expected) {
        assertEquals(Helper.replacePlaceholders(this.attributes, expected), then.extract().path(path).toString());
    }
}
