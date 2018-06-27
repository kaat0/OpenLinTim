# How to create new CI tests

- Copy the content of the `template` folder into a new folder in `/ci/`
- Modify the run.sh file
    - Name the `TARGET_DATASET` variable
    - Enter the commands to run into the `COMMANDS_TO_RUN` variable
- Enter the necessary config data into basis/Private-Config.cnf
- Enter the expected outcome into expected-statistic.sta