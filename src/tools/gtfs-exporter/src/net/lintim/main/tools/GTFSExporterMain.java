package net.lintim.main.tools;

import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.io.*;
import net.lintim.io.tools.GTFSWriter;
import net.lintim.model.*;
import net.lintim.util.Config;

import java.util.Collection;

public class GTFSExporterMain {
    public static void main(String[] args) {
        if (args.length < 1) {
            throw new ConfigNoFileNameGivenException();
        }
        Config config = new ConfigReader.Builder(args[0]).build().read();
        Graph<Stop, Link> ptn = new PTNReader.Builder().readLinks(true).build().read();
        LinePool lines = new LineReader.Builder(ptn).readCosts(false).readFrequencies(true).build().read();
        Graph<AperiodicEvent, AperiodicActivity> aperiodicEan = new AperiodicEANReader.Builder().build().read()
            .getFirstElement();
        Collection<Trip> trips = new TripReader.Builder().build().read();
        new GTFSWriter.Builder(ptn, lines, trips, aperiodicEan).build().write();
    }
}
