import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;


public class FillOD {
	static double[][] od_matrix;
	static boolean values_integral;
	static String header;
	
	public static void main(String[] args) {
		if(args.length != 1){
            throw new RuntimeException("Error: number of arguments invalid; first " +
                    "argument must be the path to the configuration file.");
        }

        try {
            File config_file = new File(args[0]);

            System.err.print("Loading Configuration... ");
            Config config = new Config(config_file);
            System.err.println("done!");
            
            System.err.print("Set variables... ");
            File od_file=new File(config.getStringValue("default_od_file"));
            header=config.getStringValue("od_header");
            values_integral=config.getBooleanValue("od_values_integral");
            System.err.println("done!");
            
            System.err.print("Reading OD-Matrix... ");
            ODFromFile(od_file);
            System.err.println("done!");
            
            System.err.print("Writing OD-Matrix... ");
            ODToFile(od_file);
            System.err.println("done!");
            
        }
        catch(IOException e){
        	System.err.println("An error occurred while reading a file.");
        	throw new RuntimeException(e);
        }  
	}
	
//private mathods---------------------------------------------------------------------------------------
	
	private static void ODFromFile(File od_file)throws IOException{
		int max_index=0;
		int from_index;
		int to_index;
		String line;
		String[] values;
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
				from_index=Integer.parseInt(values[0].trim());
				to_index=Integer.parseInt(values[1].trim());
				if(from_index>max_index)
					max_index=from_index;
				if(to_index>max_index)
					max_index=to_index;
			}
		}
		scan.close();
		
		od_matrix=new double[max_index][max_index];
		
		scan = new Scanner(new BufferedReader(new FileReader(od_file)));
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
				from_index=Integer.parseInt(values[0].trim());
				to_index=Integer.parseInt(values[1].trim());
				passengers=Double.parseDouble(values[2].trim());
				od_matrix[from_index-1][to_index-1]=passengers;
				od_matrix[to_index-1][from_index-1]=passengers;
			}
		}
		scan.close();
	}
	
	
	private static void ODToFile(File od_file)throws IOException{
		FileWriter writer = new FileWriter(od_file);
		writer.write("#"+header + "\n");
		for(int i=0; i<od_matrix.length; i++){
			for(int j=0; j<od_matrix.length; j++){
				if (values_integral)
					writer.append((i+1) +"; " +(j+1) + "; " + (int) od_matrix[i][j] +"\n");
				else
					writer.append((i+1) +"; " +(j+1) + "; " +  od_matrix[i][j] +"\n");
				writer.flush();
			}
		}
		writer.close();
	}
}
