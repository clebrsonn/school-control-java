package br.com.hyteck.school_control.usecases.storage;

import br.com.hyteck.school_control.exceptions.StorageException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Service responsible for storing, loading, and deleting files in the local file system.
 * Implements the StorageService interface for file operations using Spring's Resource abstraction.
 */
@Service
@Qualifier("file-system")
public class FileStorageService implements StorageService {

    private final Path rootLocation = Paths.get("uploads");

    /**
     * Initializes the storage directory structure.
     *
     * @throws StorageException if the storage directory cannot be created
     */
    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        }
        catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }

    }

    /**
     * Stores a file in the storage location.
     *
     * @param file the file to store
     * @return the stored file name
     * @throws StorageException if the file is empty, outside the allowed directory, or cannot be stored
     */
    @Override
    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file.");
            }
            Path destinationFile = this.rootLocation.resolve(
                            Paths.get(file.getOriginalFilename()))
                    .normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                // This is a security check
                throw new StorageException(
                        "Cannot store file outside current directory.");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            return destinationFile.getFileName().toString();
        }
        catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }


    }

    /**
     * Loads all file paths from the storage location.
     *
     * @return a stream of relative file paths
     * @throws StorageException if the files cannot be read
     */
    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(this.rootLocation::relativize);
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
    }

    /**
     * Loads a specific file path from the storage location.
     *
     * @param filename the name of the file to load
     * @return the resolved file path
     */
    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    /**
     * Loads a file as a Spring Resource for download or streaming.
     *
     * @param filename the name of the file to load
     * @return the file as a Resource
     * @throws StorageException if the file cannot be read or does not exist
     */
    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageException(
                        "Could not read file: " + filename);

            }
        }
        catch (MalformedURLException e) {
            throw new StorageException("Could not read file: " + filename, e);
        }
    }

    /**
     * Deletes all files and directories in the storage location.
     */
    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());

    }
}
