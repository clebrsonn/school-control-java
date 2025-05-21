package br.com.hyteck.school_control.usecases.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Service implementation for file storage using Cloudinary.
 * Provides methods to upload files to Cloudinary and implements the StorageService interface.
 *
 * @author YourName
 * @since 23
 */
@Log4j2
@Service
@Qualifier("cloudinary")
public class CloudinaryStorage implements StorageService {

    /**
     * Cloudinary client instance.
     */
    private Cloudinary cloudinary;

    /**
     * Cloudinary URL for authentication and configuration.
     */
    private final String cloudinaryUrl;

    /**
     * Constructs the CloudinaryStorage service with the given Cloudinary URL.
     *
     * @param cloudinaryUrl the Cloudinary URL from application properties
     */
    public CloudinaryStorage(@Value("${cloudinary.url}") String cloudinaryUrl) {
        this.cloudinaryUrl = cloudinaryUrl;
        init();
    }

    /**
     * Initializes the Cloudinary client with secure configuration.
     */
    @Override
    public void init() {
        this.cloudinary = new Cloudinary(cloudinaryUrl);
        this.cloudinary.config.secure = true;
    }

    /**
     * Uploads a file to Cloudinary and returns the secure URL.
     *
     * @param file the file to upload
     * @return the secure URL of the uploaded file
     * @throws RuntimeException if the upload fails
     */
    @Override
    public String store(MultipartFile file) {
        try {
            Map result = this.cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            log.error(Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).reduce("", (a, b) -> a + "\n" + b));
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    /**
     * Not implemented for Cloudinary storage. Returns an empty stream.
     *
     * @return an empty stream
     */
    @Override
    public Stream<Path> loadAll() {
        return Stream.empty();
    }

    /**
     * Not implemented for Cloudinary storage. Returns null.
     *
     * @param filename the name of the file
     * @return always null
     */
    @Override
    public Path load(String filename) {
        return null;
    }

    /**
     * Not implemented for Cloudinary storage. Returns null.
     *
     * @param filename the name of the file
     * @return always null
     */
    @Override
    public Resource loadAsResource(String filename) {
        return null;
    }

    /**
     * Not implemented for Cloudinary storage.
     */
    @Override
    public void deleteAll() {
        // Placeholder for deletion logic
    }
}
