package cc.vips_is.rest.mapper;

import cc.vips_is.service.image.exceptions.ImageProcessingException;
import cc.vips_is.service.image.exceptions.InvalidParameterException;
import cc.vips_is.service.storage.exceptions.ImageNotFoundException;
import cc.vips_is.service.storage.exceptions.StorageException;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
public class CustomExceptionMapperTest {

    /**
     * Mock Resource to trigger the exceptions.
     * Quarkus will deploy this alongside the test.
     */
    @Path("/test-exception")
    public static class TestResource {
        @GET
        @Path("/not-found")
        public void throwNotFound() {
            throw new ImageNotFoundException("Image 123 not found");
        }

        @GET
        @Path("/invalid-param")
        public void throwInvalidParam() {
            throw new InvalidParameterException("Invalid rotation value");
        }
        @GET
        @Path("/storage-error")
        public void throwStorage() {
            throw new StorageException("Disk failure or S3 timeout");
        }

        @GET
        @Path("/security-error")
        public void throwSecurity() {
            throw new SecurityException("Direct access not allowed");
        }

        @GET
        @Path("/processing-error")
        public void throwProcessing() {
            throw new ImageProcessingException("VIPS resize failed");
        }
    }

    @Test
    @DisplayName("Should return 404 and plain text when ImageNotFoundException is thrown")
    void testNotFoundMapper() {
        given()
                .when().get("/test-exception/not-found")
                .then()
                .statusCode(404)
                .contentType(ContentType.TEXT)
                .body(containsString("Resource Not Found"))
                .body(containsString("Image 123 not found"));
    }

    @Test
    @DisplayName("Should return 400 and plain text when InvalidParameterException is thrown")
    void testInvalidParameterMapper() {
        given()
                .when().get("/test-exception/invalid-param")
                .then()
                .statusCode(400)
                .contentType(ContentType.TEXT)
                .body(containsString("Invalid IIIF Parameter"))
                .body(containsString("Invalid rotation value"));
    }

    @Test
    @DisplayName("Should return 500 for StorageException")
    void testStorageMapper() {
        given()
                .when().get("/test-exception/storage-error")
                .then()
                .statusCode(500)
                .contentType(ContentType.TEXT)
                .body(containsString("Storage Error"));
    }

    @Test
    @DisplayName("Should return 403 for SecurityException")
    void testSecurityMapper() {
        given()
                .when().get("/test-exception/security-error")
                .then()
                .statusCode(403)
                .contentType(ContentType.TEXT)
                .body(containsString("Access Denied"));
    }

    @Test
    @DisplayName("Should return 500 for ImageProcessingException")
    void testProcessingMapper() {
        given()
                .when().get("/test-exception/processing-error")
                .then()
                .statusCode(500)
                .contentType(ContentType.TEXT)
                .body(containsString("Processing Error"));
    }
}
