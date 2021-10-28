package net.lintim.io;

import net.lintim.exception.InputFileException;
import net.lintim.exception.OutputFileException;
import net.lintim.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.BiConsumer;

/**
 * File to process a csv file. Comments in the form of "#..." are trimmed from the end of the lines. Empty lines are
 * ignored. Other lines will be split by ";" and processed by a given Builder class.
 */
public class CsvReader {
    private static final Logger logger = new Logger(CsvReader.class);

    /**
     * Read the file with the given name line by line and process each line with the given processor. The lines are
     * trimmed for comments ("#...") and whitespaces before splitted by ";". The tokens are then given to a
     * {@link BiConsumer} to process the content of the line
     * @param fileName the name of the file to read
     * @param processor the processor to process every line. First argument are the tokens of the line, second
     *                  argument the line number (used for error messages)
     */
    public static void readCsv(String fileName, BiConsumer<String[], Integer> processor) throws InputFileException {
        try {
            logger.debug("Reading file " + fileName);
            BufferedReader reader = Files.newBufferedReader(Paths.get(fileName));
            String line;
            int lineIndex = 0;
            while ((line = reader.readLine()) != null) {
                lineIndex++;
                //First trim the comments, if there are any
                int position = line.indexOf("#");
                if (position != -1) {
                    line = line.substring(0, position);
                }
                line = line.trim();
                //Empty lines can be ignored
                if (line.isEmpty()) {
                    continue;
                }
                String[] tokens = line.split(";");
                for (int i = 0; i < tokens.length; i++) {
                    tokens[i] = tokens[i].trim();
                }
                //Let the processor process the line
                processor.accept(tokens, lineIndex);
            }
            reader.close();
        }
        catch (IOException e){
            throw new InputFileException(fileName);
        }
    }

	/**
	 * Read the given file and determine the number of lines, without empty lines, lines only containing whitespaces
	 * and lines only containing whitespaces and comments
	 * @param fileName the name of the file
	 * @return the number of lines with content of this file
	 */
	public static int determineNumberOfLines(String fileName) throws OutputFileException{
	    try {
            int numberOfLines = 0;
            BufferedReader reader = Files.newBufferedReader(Paths.get(fileName));
            String line;
            while ((line = reader.readLine()) != null) {
                //First trim the comments, if there are any
                int position = line.indexOf("#");
                if (position != -1) {
                    line = line.substring(0, position);
                }
                line = line.trim();
                //Empty lines can be ignored
                if (line.isEmpty()) {
                    continue;
                }
                numberOfLines++;
            }
            reader.close();
            return numberOfLines;
        }
        catch (IOException e){
            logger.error(e.toString());
            throw new InputFileException(fileName);
        }
	}
}
