import java.io.*;
import java.util.*;

public class ODCSV {
	static String od_header;
	
	public static void setHeader(String header){
		od_header=header;
	}
	
	public static void fromFile(PTN ptn, OD od, File od_file) throws IOException{
		TreeMap<Integer, Stop> stops_by_index =new TreeMap<Integer,Stop>();
		for(Stop stop: ptn.getStops()){
			stops_by_index.put(stop.getIndex(),stop);
		}
		
		String line;
		String[] values;
		Stop origin;
		Stop destination;
		double passengers;

		Scanner scan = new Scanner(new BufferedReader(new FileReader(od_file)));
		while (scan.hasNext()) {
			line = scan.nextLine().trim();
			if (line.indexOf("#") == 0)
				continue;
			if (line.indexOf("#") > -1) {
				line = line.substring(0, line.indexOf("#") - 1);
			}
			if (line.contains(";")) {
				values = line.split(";");
				if (values.length != 3) {
					throw new IOException(
							"Wrong number of entries in line!");
				}
				origin=stops_by_index.get(Integer.parseInt(values[0].trim()));
				destination=stops_by_index.get(Integer.parseInt(values[1].trim()));
				passengers=Double.parseDouble(values[2].trim());
				od.setPassengersAt(origin, destination, passengers);
			}
		}
		scan.close();
	}
	
	public static void toFile(OD od, PTN ptn,  File od_file) throws IOException{
		
		double[][] od_matrix= new double [ptn.getStops().size()][ptn.getStops().size()];
		
		for(Stop origin:od.getOrigins()){
			for(Map.Entry<Stop,Double> entry: od.getPassengersByOrigin(origin).entrySet()){
				od_matrix[origin.getIndex()-1][entry.getKey().getIndex()-1]=entry.getValue();
			}
		}
		
		FileWriter writer = new FileWriter(od_file);
		writer.write("#"+od_header + "\n");
		for(int i=0; i<ptn.getStops().size(); i++){
			for(int j=0; j<ptn.getStops().size(); j++){
				writer.append((i+1) +";" +(j+1) + "; " + (int) od_matrix[i][j] +"\n");
				writer.flush();
			}
		}
		writer.close();
	}
	
}
