package cc.vips_is.service.storage.s3;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class S3ClientProducer {

    @ConfigProperty(name = "storage.s3.region", defaultValue = "us-east-1")
    String region;

    @ConfigProperty(name = "storage.s3.endpoint")
    Optional<String> endpointOverride;

    @Produces
    @ApplicationScoped
    public S3Client produce() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());

        endpointOverride.ifPresent(ep -> {
            log.info("S3 endpoint override: {}", ep);
            builder.endpointOverride(URI.create(ep));
        });

        return builder.build();
    }
}
