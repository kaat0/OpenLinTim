import sys

if __name__ == '__main__':
    if len(sys.argv) < 3:
        raise RuntimeError("Program needs two parameters, the two files to compare.")
    expected_file_name = sys.argv[1]
    given_file_name = sys.argv[2]
    with open(expected_file_name, encoding="utf-8") as expected_file:
        with open(given_file_name, encoding="utf-8") as given_file:
            difference = set(expected_file).difference(given_file)
    if difference:
        print("Found different lines in files {} and {}".format(expected_file_name, given_file_name))
        for line in difference:
            print(line)
        exit(1)