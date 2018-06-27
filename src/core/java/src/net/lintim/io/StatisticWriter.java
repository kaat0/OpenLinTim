package net.lintim.io;

import net.lintim.exception.OutputFileException;
import net.lintim.util.Config;
import net.lintim.util.Statistic;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

/**
 * Class to write statistic files
 *
 * Use {@link Builder#build()} on a {@link Builder} object to create the writer and use {@link #write()} afterwards.
 */
public class StatisticWriter {
    private final String fileName;
    private final Statistic statistic;

    private StatisticWriter(Builder builder) {
        this.statistic = builder.statistic == null ? Statistic.getDefaultStatistic() : builder.statistic;
        this.fileName = "".equals(builder.fileName) ? builder.config.getStringValue("default_statistic_file") :
            builder.fileName;
    }

    /**
     * Start the writing process. The behavior is controlled by the {@link Builder} object, this object was created
     * with.
     */
    public void write() {
        CsvWriter writer = new CsvWriter(fileName);
        Function<Map.Entry<String, String>, String[]> outputFunction = (Map.Entry<String, String> entry) -> new
            String[]{entry.getKey(), CsvWriter.shortenDecimalValueIfItsDecimal(entry.getValue())};
        try {
            writer.writeCollection(statistic.getData().entrySet(), outputFunction, Comparator.comparing(Map
                .Entry::getKey));
            writer.close();
        } catch (IOException e) {
            throw new OutputFileException(fileName);
        }
    }

    /**
     * Builder object for a statistic writer.
     *
     * Use {@link #Builder()} to create a builder with default options, afterwards use the setter to adapt it.
     * The setters return this object, therefore they can be chained.
     *
     * For the possible parameters and their default values, see {@link #Builder()}. To create a writer object,
     * use {@link #build()} after setting all parameters.
     */
    public static class Builder {
        private String fileName = "";
        private Statistic statistic = Statistic.getDefaultStatistic();
        private Config config = Config.getDefaultConfig();

        /**
         * Create a default builder object. Possible parameters for this class are (with the default in parentheses):
         * <ul>
         *     <li>
         *          statistic ({@link Statistic#getDefaultStatistic()}) - the statistic to write
         *     </li>
         *     <li>
         *          config ({@link Config#getDefaultConfig()}) - the config to read the file name from.
         *          This will only happen, if the file name is not given, but queried.
         *     </li>
         *     <li>
         *          file name (dependent on config) - the file name to write the statistic to
         *     </li>
         * </ul>
         * All values can be set using the corresponding setters of this class. If you are ready, call
         * {@link #build()} to create a writer with the given parameters.
         */
        public Builder() { }

        /**
         * Set the statistic file name
         * @param fileName the file to write the statistic to
         * @return this object
         */
        public Builder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * Set the statistic to write
         * @param statistic the statistic to write
         * @return this object
         */
        public Builder setStatistic(Statistic statistic) {
            this.statistic = statistic;
            return this;
        }

        /**
         * Set the config. The config is used to read the file name, if it is queried but not given.
         * @param config the config
         * @return this object
         */
        public Builder setConfig(Config config) {
            this.config = config;
            return this;
        }

        /**
         * Create a new statistic writer with the current builder settings
         * @return the new writer. Use {@link #write()} for the writing process.
         */
        public StatisticWriter build() {
            return new StatisticWriter(this);
        }
    }
}
