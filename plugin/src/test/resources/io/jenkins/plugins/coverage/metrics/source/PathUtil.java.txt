package edu.hm.hafner.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Utilities for {@link Path} instances.
 *
 * @author Ullrich Hafner
 */
public class PathUtil {
    private static final String BACK_SLASH = "\\";
    private static final String SLASH = "/";
    private static final String DRIVE_LETTER_PREFIX = "^[a-z]:/.*";

    /**
     * Tests whether a file exists.
     *
     * <p>
     * Note that the result of this method is immediately outdated. If this method indicates the file exists then there
     * is no guarantee that a subsequence access will succeed. Care should be taken when using this method in security
     * sensitive applications.
     * </p>
     *
     * @param fileName
     *         the absolute path of the file
     *
     * @return {@code true} if the file exists; {@code false} if the file does not exist or its existence cannot be
     *         determined.
     */
    public boolean exists(final String fileName) {
        try {
            return Files.exists(Paths.get(fileName));
        }
        catch (IllegalArgumentException ignore) {
            return false;
        }
    }

    /**
     * Tests whether a file exists.
     *
     * <p>
     * Note that the result of this method is immediately outdated. If this method indicates the file exists then there
     * is no guarantee that a subsequence access will succeed. Care should be taken when using this method in security
     * sensitive applications.
     * </p>
     *
     * @param fileName
     *         the file name
     * @param directory
     *         the directory that contains the file
     *
     * @return {@code true} if the file exists; {@code false} if the file does not exist or its existence cannot be
     *         determined.
     */
    public boolean exists(final String fileName, final String directory) {
        return exists(createAbsolutePath(directory, fileName));
    }

    /**
     * Returns the string representation of the specified path. The path will be actually resolved in the file system
     * and will be returned as fully qualified absolute path. In case of an error, i.e. if the file is not found, the
     * provided {@code path} will be returned unchanged (but normalized using the UNIX path separator and upper case
     * drive letter).
     *
     * @param path
     *         the path to get the absolute path for
     *
     * @return the absolute path
     */
    public String getAbsolutePath(final String path) {
        try {
            return getAbsolutePath(Paths.get(path));
        }
        catch (IllegalArgumentException ignored) {
            return makeUnixPath(path);
        }
    }

    /**
     * Returns the string representation of the specified path. The path will be actually resolved in the file system
     * and will be returned as fully qualified absolute path. In case of an error, i.e. if the file is not found, the
     * provided {@code path} will be returned unchanged (but normalized using the UNIX path separator and upper case
     * drive letter).
     *
     * @param path
     *         the path to get the absolute path for
     *
     * @return the absolute path
     */
    public String getAbsolutePath(final Path path) {
        try {
            return makeUnixPath(normalize(path).toString());
        }
        catch (IOException | IllegalArgumentException ignored) {
            return makeUnixPath(path.toString());
        }
    }

    /**
     * Returns the relative path of specified path with respect to the provided base directory. The given path will be
     * actually resolved in the file system (which may lead to a different fully qualified absolute path). Then the base
     * directory prefix will be removed (if possible). In case of an error, i.e., if the file is not found or could not
     * be resolved in the parent, then the provided {@code path} will be returned unchanged (but normalized using the
     * UNIX path separator and upper case drive letter).
     *
     * @param base
     *         the base directory that should be  to get the absolute path for
     * @param path
     *         the path to get the absolute path for
     *
     * @return the relative path
     */
    public String getRelativePath(final Path base, final String path) {
        try {
            return getRelativePath(base, Paths.get(path));
        }
        catch (IllegalArgumentException ignored) {
            return makeUnixPath(path);
        }
    }

    /**
     * Returns the relative path of specified path with respect to the provided base directory. The given path will be
     * actually resolved in the file system (which may lead to a different fully qualified absolute path). Then the base
     * directory prefix will be removed (if possible). In case of an error, i.e., if the file is not found or could not
     * be resolved in the parent, then the provided {@code path} will be returned unchanged (but normalized using the
     * UNIX path separator and upper case drive letter).
     *
     * @param base
     *         the base directory that should be  to get the absolute path for
     * @param path
     *         the path to get the absolute path for
     *
     * @return the relative path
     */
    public String getRelativePath(final String base, final String path) {
        try {
            return getRelativePath(Paths.get(base), Paths.get(path));
        }
        catch (IllegalArgumentException ignored) {
            return makeUnixPath(path);
        }
    }

    /**
     * Returns the relative path of specified path with respect to the provided base directory. The given path will be
     * actually resolved in the file system (which may lead to a different fully qualified absolute path). Then the base
     * directory prefix will be removed (if possible). In case of an error, i.e., if the file is not found or could not
     * be resolved in the parent, then the provided {@code path} will be returned unchanged (but normalized using the
     * UNIX path separator and upper case drive letter).
     *
     * @param base
     *         the base directory that should be  to get the absolute path for
     * @param path
     *         the path to get the absolute path for
     *
     * @return the relative path
     */
    public String getRelativePath(final Path base, final Path path) {
        try {
            Path normalizedBase = normalize(base);
            if (path.isAbsolute()) {
                return makeUnixPath(normalizedBase.relativize(normalize(path)).toString());
            }
            return makeUnixPath(normalizedBase.relativize(normalize(base.resolve(path))).toString());

        }
        catch (IOException | IllegalArgumentException ignored) {
            // ignore and return the path as such
        }
        return makeUnixPath(path.toString());
    }

    /**
     * Returns a normalized relative path of specified path. The given path will be actually resolved in the file system
     * (which may lead to a different path). In case of an error, i.e., if the file is not found or could not be
     * resolved in the parent, then the provided {@code path} will be returned unchanged (but normalized using the UNIX
     * path separator and upper case drive letter).
     *
     * @param relative
     *         the path to get the normalized path for
     *
     * @return the normalized relative path
     */
    public String getRelativePath(final Path relative) {
        return makeUnixPath(relative.normalize().toString());
    }

    /**
     * Returns a normalized relative path of specified path. The given path will be actually resolved in the file system
     * (which may lead to a different path). In case of an error, i.e., if the file is not found or could not be
     * resolved in the parent, then the provided {@code path} will be returned unchanged (but normalized using the UNIX
     * path separator and upper case drive letter).
     *
     * @param relative
     *         the path to get the normalized path for
     *
     * @return the normalized relative path
     */
    public String getRelativePath(final String relative) {
        try {
            return getRelativePath(Paths.get(relative));
        }
        catch (IllegalArgumentException ignored) {
            // ignore and return the path as such
        }
        return makeUnixPath(relative);
    }

    /**
     * Returns the absolute path of the specified file in the given directory.
     *
     * @param directory
     *         the directory that contains the file
     * @param fileName
     *         the file name
     *
     * @return the absolute path
     */
    public String createAbsolutePath(@CheckForNull final String directory, final String fileName) {
        if (isAbsolute(fileName) || StringUtils.isBlank(directory)) {
            return makeUnixPath(fileName);
        }
        String path = makeUnixPath(Objects.requireNonNull(directory));

        String separator;
        if (path.endsWith(SLASH)) {
            separator = StringUtils.EMPTY;
        }
        else {
            separator = SLASH;
        }

        try {
            String normalized = FilenameUtils.normalize(String.join(separator, path, fileName));
            return makeUnixPath(normalized == null ? fileName : normalized);
        }
        catch (IllegalArgumentException ignored) {
            return makeUnixPath(fileName);
        }
    }

    /**
     * Returns whether the specified file name is an absolute path.
     *
     * @param fileName
     *         the file name to test
     *
     * @return {@code true} if this path is an absolute path, {@code false} if a relative path
     */
    public boolean isAbsolute(final String fileName) {
        return FilenameUtils.getPrefixLength(fileName) > 0;
    }

    private Path normalize(final Path path) throws IOException {
        return path.toAbsolutePath().normalize().toRealPath(LinkOption.NOFOLLOW_LINKS);
    }

    private String makeUnixPath(final String fileName) {
        String unixStyle = fileName.replace(BACK_SLASH, SLASH);
        if (unixStyle.matches(DRIVE_LETTER_PREFIX)) {
            unixStyle = StringUtils.capitalize(unixStyle);
        }
        return unixStyle;
    }
}
