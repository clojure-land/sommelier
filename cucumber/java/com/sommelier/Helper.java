package com.sommelier;

import com.auth0.client.auth.AuthAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.net.AuthRequest;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.baseURI;

class Helper {
    private static final String HOST = System.getenv("cucumber.server");

    /**
     * Configures the REST assured static properties.
     */
    public static void configureRestAssured() {
        baseURI = HOST;
    }

    /**
     * Replaces placeholder values in string with actual values.
     */
    public static String replacePlaceholders(HashMap<String, Object> attributes, String string)
    {
        for (Map.Entry attr : attributes.entrySet()) {
            string = string.replaceAll(attr.getKey().toString(), attr.getValue().toString());
        }

        return string;
    }

    /**
     * Requests an access token from Auth0.
     */
    public static String RequestToken()
    {
        AuthAPI auth = new AuthAPI(
                System.getenv("DOMAIN"),
                System.getenv("CLIENT_ID"),
                System.getenv("CLIENT_SECRET")
        );

        AuthRequest request = auth.requestToken("https://"+System.getenv("DOMAIN")+"/api/v2/");
        try {
            return request.execute().getAccessToken();
        } catch (Auth0Exception ex) {
            System.out.println(ex.getMessage());
        }

        return "";
    }
}
