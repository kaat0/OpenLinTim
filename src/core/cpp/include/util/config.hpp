#ifndef CONFIG_HPP
#define CONFIG_HPP

/**
 * Implementation of a config class, handling all the config interaction. Based on the "old" LinTim implementation.
 */

#include "../exception/exceptions.hpp"
#include "../util/SolverType.hpp"
#include "../util/LogLevel.hpp"
#include <string>
#include <algorithm>
#include <map>


class Config{

private:
    std::map <std::string, std::string> data;

    //static Logger logger = Logger.getLogger("net.lintim.util.Config");

public:
    /**
     * Initialize an empty config. Can be filled manually with {@link #put(std::string, std::string)} or automatically with
     * {@link #readConfig(std::string)} or {@link #readConfig(std::string, bool)}.
     */
    Config(){
    }

    /**
     * Get the std::string value of the given config key.
     *
     * @param key the key to look for in the config
     * @return the value to the given key
     */
    std::string getStringValue(std::string key){
        if(key == ("default_activities_periodic_file")
           && !data["use_buffered_activities"].empty()
           && getBooleanValue("use_buffered_activities")){
            key = "default_activity_buffer_file";
        } else if(key == "default_activities_periodic_unbuffered_file"){
            if(!data["use_buffered_activities"].empty()
               && getBooleanValue("use_buffered_activities")){
                key = "default_activity_relax_file";
            } else{
                key = "default_activities_periodic_file";
            }
        }
        std::string value = data[key];
        if(value.empty()){
            throw ConfigKeyNotFoundException(key);
        }
        //logger.log(LogLevel.DEBUG, "Read key " + key + " with value " + value + " from config");
        return value;
    }

    /**
     * Get the double value of the given config key
     *
     * @param key the key to look for
     * @return the double value to the given key
     * @throws ConfigKeyNotFoundException  if the queried key cannot be found
     * @throws ConfigTypeMismatchException if the queried value cannot be parsed as double
     */
    double getDoubleValue(std::string key){
        std::string value = getStringValue(key);
        try{
            return std::stod(value);
        } catch(std::exception& e){
            throw ConfigTypeMismatchException(key, "double", value);
        }
    }

    /**
     * Get the bool value of the given config key
     *
     * @param key the key to look for
     * @return the bool value to the given key
     * @throws ConfigKeyNotFoundException if the queried key cannot be found
     */
    bool getBooleanValue(std::string key){
        std::string value = getStringValue(key);
        transform(value.begin(), value.end(), value.begin(), ::tolower);
        if(value.find("true") != std::string::npos){
            return true;
        } else if(value.find("false") != std::string::npos){
            return false;
        } else{
            throw ConfigTypeMismatchException(key, "bool", value);
        }
    }

    /**
     * Get the integer value of the given config key
     *
     * @param key the key to look for
     * @return the integer value to the given key
     * @throws ConfigKeyNotFoundException  if the queried key cannot be found
     * @throws ConfigTypeMismatchException if the queried value cannot be parsed as integer
     */
    int getIntegerValue(std::string key){
        std::string value = getStringValue(key);
        try{
            return std::stoi(value);
        } catch(std::exception& e){
            throw ConfigTypeMismatchException(key, "integer", value);
        }
    }

    /**
     * Get the solver type value of the given config key
     *
     * @param key the key to look for
     * @return the solver type of the given key
     * @throws ConfigKeyNotFoundException  if the queried key cannot be found
     * @throws ConfigTypeMismatchException if the queried value cannot be parsed as a solver type
     */
    SolverType getSolverType(std::string key){
        std::string value = getStringValue(key);
        transform(value.begin(), value.end(), value.begin(), ::toupper);
        if(value.find("XPRESS") != std::string::npos) return XPRESS;
        else if(value.find("GUROBI") != std::string::npos) return GUROBI;
        else if(value.find("CPLEX") != std::string::npos) return CPLEX;
        else if(value.find("GLPK") != std::string::npos) return GLPK;
        else throw ConfigTypeMismatchException(key, "XPRESS/GUROBI/CPLEX/GLPK", value);
    }

    /**
     * Get the log level value of the given config key
     *
     * @param key the key to look for
     * @return the log level of the given key
     * @throws ConfigKeyNotFoundException  if the queried key cannot be found
     * @throws ConfigTypeMismatchException if the queried value cannot be parsed as a solver type
     */
    LogLevel getLogLevel(std::string key){
        std::string value = getStringValue(key);
        transform(value.begin(), value.end(), value.begin(), ::toupper);
        if(value.find("FATAL") != std::string::npos) return FATAL;
        else if(value.find("ERROR") != std::string::npos) return ERROR;
        else if(value.find("WARN") != std::string::npos) return WARN;
        else if(value.find("INFO") != std::string::npos) return INFO;
        else if(value.find("DEBUG") != std::string::npos) return DEBUG;
        else throw ConfigTypeMismatchException(key, "FATAL/ERROR/WARN/INFO/DEBUG", value);
    }

    /**
     * Put the specified data into the config collection. Content with the same key will be overwritten
     *
     * @param key   the key to add
     * @param value the value to add
     */
    void put(std::string key, std::string value){
        this->data[key] = value;
    }

};

#endif
