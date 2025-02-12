package com.tibudget.plugins.csv;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CsvFileReader implements Closeable {

    private final BufferedReader reader;
    private final char separator;

    public CsvFileReader(String filePath, char separator) throws IOException {
        this.reader = new BufferedReader(new FileReader(filePath));
        this.separator = separator;
    }

    /**
     * Reads the next line from the CSV file and splits it into an array of strings.
     * Returns null if end of file is reached.
     */
    public String[] readNext() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            return null; // End of file
        }
        return parseLine(line, separator);
    }

    /**
     * Parses a line from the CSV file, considering quotes.
     */
    private String[] parseLine(String line, char separator) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean insideQuotes = false;

        for (char ch : line.toCharArray()) {
            if (ch == '\"') {
                insideQuotes = !insideQuotes; // Toggle the insideQuotes flag
            } else if (ch == separator && !insideQuotes) {
                tokens.add(currentToken.toString().trim());
                currentToken.setLength(0); // Reset buffer
            } else {
                currentToken.append(ch);
            }
        }
        tokens.add(currentToken.toString().trim()); // Add last token

        return tokens.toArray(new String[0]);
    }

    /**
     * Closes the file reader.
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }

    public static void main(String[] args) {
        String filePath = "path/to/your/file.csv";
        char separator = ','; // Change separator if needed

        try (CsvFileReader csvReader = new CsvFileReader(filePath, separator)) {
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                System.out.println(String.join(" | ", line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
