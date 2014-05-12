package zemberek.core.io;

import com.google.common.base.Charsets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class ResourceUtil {

    public static List<String> readAllLines(final String fileName) throws IOException {
        return readAllLines(fileName, ResourceUtil.class.getClassLoader());
    }

    public static List<String> readAllLines(final String fileName, final ClassLoader classLoader) throws IOException {
        final InputStream in = getResource(fileName, classLoader).openStream();
        final List<String> lines = readAllLinesFromStream(in);
        in.close();
        return lines;
    }

    public static List<String> readAllLinesFromStream(final InputStream in) throws IOException {
        final List<String> lines = new ArrayList<>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }

        return lines;
    }

    public static URL getResource(String resourceName) {
        return getResource(resourceName, ResourceUtil.class.getClassLoader());
    }

    public static URL getResource(final String resourceName, final ClassLoader loader) {
        final URL url = loader.getResource(resourceName);
        checkArgument(url != null, "resource %s not found.", resourceName);
        return url;
    }
}
