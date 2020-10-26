package gr.aueb.distrsys.recommendations;

import org.apache.commons.math3.linear.OpenMapRealMatrix;

import java.io.*;

public class ReadCSV {

    private OpenMapRealMatrix r = new OpenMapRealMatrix(835, 1962);

    public void readFile(String dataset){

        File f = null;
        String line;
        BufferedReader reader = null;
        String[][] data=new String[1][3];

        try {
            f = new File(dataset);
        }
        catch (NullPointerException e) {
            System.err.println("File not found.");
        }

        try {
            reader = new BufferedReader(new FileReader(f));
        }
        catch (FileNotFoundException e) {
            System.err.println("Error opening file!");
        }

        try{
            while ((line = reader.readLine()) != null) {

                data[0] = line.split(",");
                r.setEntry(Integer.parseInt(data[0][0].trim()),Integer.parseInt(data[0][1].trim()),Integer.parseInt(data[0][2].trim()));

            }
        }catch (IOException e) {
            System.out.println("Error reading line.");
        }

        try {
            reader.close();
        }
        catch (IOException e) {
            System.err.println("Error closing file.");
        }


    }
    public OpenMapRealMatrix getR(){

        return r;
    }
}
