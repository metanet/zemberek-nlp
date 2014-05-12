package zemberek.core.io;

import com.google.common.base.Charsets;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ResourceUtil {

    public static List<String> readAllLines(final String fileName) throws IOException {
        return readAllLinesFromStream(new FileInputStream(fileName));
    }

    public static List<String> readAllLinesFromStream(final InputStream in) throws IOException {
        final List<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
        String newLine = System.getProperty("line.separator");
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }

        return lines;
    }


}
