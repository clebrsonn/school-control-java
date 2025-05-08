package br.com.hyteck.school_control.usecases.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.jboss.logging.Logger;
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

@Service
@Qualifier("cloudinary")
public class CloudinaryStorage implements StorageService {

    private static Logger logger =Logger.getLogger(Cloudinary.class);

    private Cloudinary cloudinary;

    private final String cloudinaryUrl;

    public CloudinaryStorage(@Value("${cloudinary.url}") String cloudinaryUrl) {
        this.cloudinaryUrl= cloudinaryUrl;
        init();
    }

    @Override
    public void init() {
        this.cloudinary = new Cloudinary(cloudinaryUrl);
        this.cloudinary.config.secure = true;
    }

    @Override
    public String store(MultipartFile file) {
        try {
            Map result = this.cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
//            "async", true,
//                    "notification_url", "https://mysite.example.com/upload_endpoint"));

            return (String) result.get("secure_url");
        } catch (IOException e) {
            logger.error(Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).reduce("", (a, b) -> a + "\n" + b));
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        return Stream.empty();
    }

    @Override
    public Path load(String filename) {
        return null;
    }

    @Override
    public Resource loadAsResource(String filename) {
        return null;
    }

    @Override
    public void deleteAll() {
        // Placeholder for deletion logic
    }
}
