#!/usr/bin/env python3
# -*- coding: utf-8 -*-
__all__ = [
        "Dataset", 
        "LinTimCSVWriter",
    ]

import os, os.path, subprocess
import csv, shutil, tempfile, re

class Dataset:
    """
    Models a LinTim dataset and takes care of all input/output operations.
    
    :example: Dataset('toy','LinTim/datasets')
    
    :param name: name of the dataset, i.e. name of the directory in which the dataset is stored
    :param path: path to the dataset
    :param env: system environment which should be used when communicating with LinTim
    :param silent: whether the entire LinTim output should be printed to stdout
    """
    
    def __init__(self, name : str, path : str, env=None, silent : bool = False):
        if not os.path.isdir(path + "/" + name):
            raise Exception(path + "/" + name+ " must be a directory")
        self.name = name
        self.path = path
        if env is None:
            self.env = os.environ.copy()
        else:
            self.env = env
        self.silent = silent
    
    def make(self, target : str):
        """
        Executes make on this dataset with the target specified. 
        The custom system environment is used. Outputs and errors are send to stdout.
        Verbose outputs are only written if the dataset was initialized with silent == False (default).
        If an error occurs an exception is raised. 
        
        :example: make('tim-timetable')
        
        :param target: make target
        
        :raises Exception: if the execution failed
        """
        cmd = ["/usr/bin/make", "-C", "{}/{}".format(self.path, self.name), target]
        print("[Dataset]", " ".join(cmd), "...", end='')
        p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, bufsize=-1, env=self.env)
        output, error = p.communicate()
        output, error = output.decode("utf-8"), error.decode("utf-8")
        if not self.silent or p.returncode != 0:
            print()
            print("[Dataset]", output)
            print("[Dataset]", error)
        if p.returncode != 0:
            raise Exception('Execution of make {} on dataset {} failed: {}'.format(target, self.name, error))
        else:
            print(" done.")
            
    def prepareFor(self, target : str) :
        """
        Prepares the dataset for executing the specified make target.
        This means that all targets in the following list are executed until the specified target is reached.
        The specified target will not be executed.
        
        1. "lc-line-concept"
        2. "ean"
        3. "tim-timetable"
        4. "ro-rollout"
        5. "dm-disposition-timetable"
        
        :example: prepareFor('tim-timetable') will call make('lc-line-concept') and make('ean')
        
        :param target: target which the dataset should be prepared for
        """
        for t in ["lc-line-concept", "ean", "tim-timetable", "ro-rollout", "dm-disposition-timetable"]:
            if t != target:
                self.make(t)
            else:
                return
    
    def statistics(self, filename : str ="statistic.sta"):
        """
        Reads a statistic file from LinTim.
        If no file name is specified 'statistics/statistics.sta' in the dataset directory will be read
        Returns are dict with strings, floats or booleans as values
        
        :param filename: filename of the statistical file or 'statistics/statistics.sta'
        :returns: dict of statistical values        
        """
        res = {}
        with open(self.path+"/"+self.name+"/statistic/"+filename, 'r') as csvfile:
            reader = csv.reader(csvfile, delimiter=";", quotechar='"')
            for row in reader:
                value = row[1].strip()
                if value.replace('.','',1).replace('-','',1).isdigit():
                    value = float(value)
                elif value == 'true':
                    value = True
                elif value == 'false':
                    value = False
                res[row[0]] = value 
        return res
    
    def applyConfig(self, config : dict, unset : list =[], filename : str="basis/Private-Config.cnf"):
        """
        Applies configuration to config files.
        
        :param conf: a dict containing config keys and their values
        :param unset: an optional list of config keys which should be removed from the file
        :param filename: path to the config file, default 'basis/Private-Config.cnf'
        """
        conf = dict(config) # take a copy        
        fn = self.path + "/" + self.name + "/" + filename
        if not os.path.isfile(fn):
            with open(fn, 'wt') as ff :
                ff.write("# ===========================================================================\n")
                ff.write("# === Settings for Robust Timetabling =======================================\n")
                ff.write("# ===========================================================================\n")

        tf = tempfile.NamedTemporaryFile(delete=False, mode="w+t")
        
        with open(fn, 'rt') as csvFile, tf:
            reader = csv.reader(csvFile, delimiter=';', quotechar='"')
            writer = LinTimCSVWriter(tf)
            
            for row in reader:
                if len(row) >= 1 and not row[0].startswith("#"):
                    if row[0] in unset:
                        continue
                    row[1] = row[1].strip()
                    if row[0] in conf:
                        row = [row[0], conf[row[0]]]
                        conf[row[0]] = None
                    writer.write([row[0], writer.escape(row[1])], escape=False)
                else:
                    writer.write(row[0], escape=False)
            for c in conf:
                if c in unset:
                    continue
                if conf[c] is not None:
                    writer.write([c, writer.escape(conf[c])])
        shutil.move(tf.name, fn)
        
    def resetConfig(self, filename : str ="basis/Private-Config.cnf"):
        """
        Deletes the given config file
        
        :param filename: config file which should be deleted or "basis/Private-Config.cnf"
        """
        self.delete(filename)
            
    def delete(self, filename : str):
        """
        Deletes the given file from the dataset's directory
        
        :param filename: relative path of the file in the dataset's directory
        """
        path = self.realPath(filename)
        if os.path.isfile(path):
            os.remove(path)
            
    def copy(self, origin : str, target : str):
        """
        Copies a file in the dataset's directory
        
        :param origin: relative path of the file in the dataset's directory which should be copied
        :param target: relative path of the target file in the dataset's directory
        """
        shutil.copyfile(self.realPath(origin), self.realPath(target))
        
    def realPath(self, filename : str = ""):
        """
        Returns the absolute path of a given file in the dataset's directory
        or of the dataset itself if filename == ""
        
        :param filename: relative path of the file
        :returns: absolute path of filename
        """
        return "/".join([self.path, self.name, filename])
    
    def readCSV(self, filename : str, columns : list = None ):
        """
        Reads a CSV file from the dataset's directory.
        
        :param filename: relative path
        :param columns: list of indices of columns which should be stored. If columns == None all columns will be included
        
        :returns: list of rows. Every row is a list which contains the i-th column's value at the i-th position
        """
        def unescape(s : str):
            s = s.strip()
            s = re.sub(r"^\"(.*)\"$", "\\1", s)
            if s.isdigit():
                return int(s)
            elif s.replace('.','',1).replace('-','',1).isdigit():
                return float(s)
            return s
            
        res = []
        with open(self.realPath(filename), "rt") as f:
            reader = csv.reader(f, delimiter=';', quotechar='"', quoting=csv.QUOTE_NONE)
            for row in reader:
                if row[0].startswith("#"):
                      continue
                if columns is not None:
                    row = [row[i] for i in columns]
                row = list(map(unescape, row))
                res.append(row)
        return res

        
class LinTimCSVWriter:
    """
    writes LinTim CSV files
    
    :param file: file object, e.g. returned by open()
    """
    
    def __init__(self, file):
        self.file = file
    
    def escape(self, s : str) -> str:
        """
        escapes values
        
        :param s: value
        """
        if isinstance(s, bool):
            s = "true" if s else "false"
        elif isinstance(s, float) or isinstance(s, int):
            s = str(s)
        elif re.match(r"^\"(.*)\"$", s) is None: # and (";" in s or "\"" in s):
            s = '"{}"'.format(s)
        return str(s)
    
    def write(self, x, escape : bool = True) -> None:
        """
        writes plain string or list of values which are then escaped according to
        CSV file definition
        
        If x is of type str it will be written to the file directly.
        Otherwise x is assumed to be iterable and treated as a list of values in a row.
        
        Set escape = False if you do not want all values to be escaped by :func:`escape`.
        
        :param x: row or plain string
        :param escape: whether values should be escaped
        """
        if type(x) is str:
            self.file.write(x + "\n")
            return
        if escape:
            x = list(map(self.escape, x))
        self.file.write("; ".join(x)+ "\n")
