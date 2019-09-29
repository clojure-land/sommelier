package com.sommelier;


import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;

public class StepDefinitions
{
    private static final String CONTENT_TYPE = "application/json";

    private RequestSpecification when;
    private ValidatableResponse then;

    private String body;
    private HashMap<String, Object> attributes = new HashMap<>();

    @Before
    public void before() {
        this.body = null;
        this.attributes.clear();
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

    @When("I request {string} using HTTP GET")
    public void i_request_using_HTTP_GET(String uri) {
        then = given()
                .contentType(CONTENT_TYPE)
                .when()
                .get(Helper.replacePlaceholders(this.attributes, uri))
                .then();
    }

    @When("I request {string} using HTTP POST")
    public void i_request_using_HTTP_POST(String uri) {
        then = given()
                .contentType(CONTENT_TYPE)
                .body(this.body)
                .when()
                .post(Helper.replacePlaceholders(this.attributes, uri))
                .then();
    }

    @When("I request {string} using HTTP DELETE")
    public void i_request_using_HTTP_DELETE(String uri) {
        then = given()
                .contentType(CONTENT_TYPE)
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

    @Then("the response body matches schema:")
    public void the_response_body_matches_schema(String expected) throws ParseException {
//        then.assertThat().body(JsonSchemaValidator.matchesJsonSchema(schema));

        System.out.println(then.extract().response());

        JSONParser parser = new JSONParser();

        this.validateResponseBody((JSONObject) parser.parse(expected));
    }

    private void validateResponseBody(JSONObject expected) throws ParseException {
        Iterator entries = expected.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            Object key = entry.getKey();
            Object value = entry.getValue();

            if(value.getClass().isArray()) {

//                JSONArray c = entry.getJSONArray(value);

//                for (int i = 0 ; i < c.length(); i++) {
//                    JSONObject obj = c.getJSONObject(i);
//                    String A = obj.getString("A");
//                    String B = obj.getString("B");
//                    String C = obj.getString("C");
//                    System.out.println(A + " " + B + " " + C);
//                }


                System.out.println("Key = " + key + ", Value = " + value);


//            if(expected.get(x).getClass().isArray()){
//            }


//            Object keyvalue = json.get(x);
//            System.out.println("key: "+ x + " value: " + keyvalue);

            }
        }
    }
}

//System.out.println(
