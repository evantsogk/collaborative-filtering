package gr.aueb.distrsys.recommendations;

import org.apache.commons.math3.linear.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Worker extends Thread {
    private long freeMemory; //ram of worker
    private int cpuNodes; //cpu cores
    private Socket requestSocket;
    private OpenMapRealMatrix C, P, I, LI;
    private RealMatrix X, Y, results;
    private static final float l=0.1f;

    private Worker() {
        freeMemory = Runtime.getRuntime().freeMemory();
        cpuNodes =  Runtime.getRuntime().availableProcessors();
    }

    public void run() {
        ObjectInputStream in;
        ObjectOutputStream out;

        try {

            Scanner scanner = new Scanner(System.in);
            System.out.println("Insert IP address of Master: ");
            String ip = scanner.next();

            requestSocket=new Socket(ip,4200);

            out= new ObjectOutputStream(requestSocket.getOutputStream());
            out.writeObject("Worker");
            out.flush();
            out= new ObjectOutputStream(requestSocket.getOutputStream());
            out.writeObject(freeMemory);
            out.writeObject(cpuNodes);
            out.flush();

            in =new ObjectInputStream(requestSocket.getInputStream());
            C=(OpenMapRealMatrix) in.readObject();
            P=(OpenMapRealMatrix) in.readObject();
            int k=(Integer) in.readObject();

            LI=new OpenMapRealMatrix(k, k);
            for (int i=0; i<k; i++) {
                for (int j=0; j<k; j++) {
                    if (i==j) {
                        LI.setEntry(i, j, l);
                    }
                }
            }

            boolean isX;
            int index_start, index_end;
            while (true) {
                in =new ObjectInputStream(requestSocket.getInputStream());
                X=(RealMatrix) in.readObject();
                Y=(RealMatrix) in.readObject();
                isX=(Boolean) in.readObject();
                index_start=(Integer) in.readObject();
                index_end=(Integer)in.readObject();

                results=MatrixUtils.createRealMatrix(index_end-index_start+1, k);

                final RealMatrix XX=preCalculateXX();
                final RealMatrix YY=preCalculateYY();

                int dimension;
                if (isX) dimension=C.getColumnDimension();
                else dimension=C.getRowDimension();

                //C-I
                I=new OpenMapRealMatrix(dimension, dimension);
                for (int i=0; i<dimension; i++) {
                    for (int j=0; j<dimension; j++) {
                        if (i==j) {
                            I.setEntry(i, j, 1);
                        }
                    }
                }

                if (isX) { //if the training is for X
                    final int quantity=(index_end-index_start)/4;
                    final int index1=index_start;
                    final int index2=index1+quantity;
                    final int index3=index2+1;
                    final int index4=index3+quantity;
                    final int index5=index4+1;
                    final int index6=index5+quantity;
                    final int index7=index6+1;
                    final int index8=index_end;
                    Thread thread1 = new Thread(){
                        public void run(){
                            for (int i = index1; i <= index2; i++) {
                                RealMatrix Cu = calculateCuMatrix(i);
                                RealMatrix x_u = calculate_x_u(i, YY, Cu);
                                System.out.println("Calculated xu: "+i);
                                results.setRowMatrix(i - index1, x_u);
                            }
                        }
                    };
                    thread1.start();

                    Thread thread2 = new Thread(){
                        public void run(){
                            for (int i = index3; i <= index4; i++) {
                                RealMatrix Cu = calculateCuMatrix(i);
                                RealMatrix x_u = calculate_x_u(i, YY, Cu);
                                System.out.println("Calculated xu: "+i);
                                results.setRowMatrix(i - index1, x_u);
                            }
                        }
                    };
                    thread2.start();

                    Thread thread3 = new Thread(){
                        public void run(){
                            for (int i = index5; i <= index6; i++) {
                                RealMatrix Cu = calculateCuMatrix(i);
                                RealMatrix x_u = calculate_x_u(i, YY, Cu);
                                System.out.println("Calculated xu: "+i);
                                results.setRowMatrix(i - index1, x_u);
                            }
                        }
                    };
                    thread3.start();

                    Thread thread4 = new Thread(){
                        public void run(){
                            for (int i = index7; i <= index8; i++) {
                                RealMatrix Cu = calculateCuMatrix(i);
                                RealMatrix x_u = calculate_x_u(i, YY, Cu);
                                System.out.println("Calculated xu: "+i);
                                results.setRowMatrix(i - index1, x_u);
                            }
                        }
                    };
                    thread4.start();

                    try {
                        thread1.join();
                        thread2.join();
                        thread3.join();
                        thread4.join();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


                else {
                    final int quantity=(index_end-index_start)/4;
                    final int index1=index_start;
                    final int index2=index1+quantity;
                    final int index3=index2+1;
                    final int index4=index3+quantity;
                    final int index5=index4+1;
                    final int index6=index5+quantity;
                    final int index7=index6+1;
                    final int index8=index_end;
                    Thread thread1 = new Thread(){
                        public void run(){
                            for (int i = index1; i <= index2; i++) {
                                RealMatrix Ci=calculateCiMatrix(i);
                                RealMatrix y_i=calculate_y_i(i, XX, Ci);
                                System.out.println("Calculated yi: "+i);
                                results.setRowMatrix(i-index1, y_i);
                            }
                        }
                    };
                    thread1.start();

                    Thread thread2 = new Thread(){
                        public void run(){
                            for (int i = index3; i <= index4; i++) {
                                RealMatrix Ci=calculateCiMatrix(i);
                                RealMatrix y_i=calculate_y_i(i, XX, Ci);
                                System.out.println("Calculated yi: "+i);
                                results.setRowMatrix(i-index1, y_i);
                            }
                        }
                    };
                    thread2.start();

                    Thread thread3 = new Thread(){
                        public void run(){
                            for (int i = index5; i <= index6; i++) {
                                RealMatrix Ci=calculateCiMatrix(i);
                                RealMatrix y_i=calculate_y_i(i, XX, Ci);
                                System.out.println("Calculated yi: "+i);
                                results.setRowMatrix(i-index1, y_i);
                            }
                        }
                    };
                    thread3.start();

                    Thread thread4 = new Thread(){
                        public void run(){
                            for (int i = index7; i <= index8; i++) {
                                RealMatrix Ci=calculateCiMatrix(i);
                                RealMatrix y_i=calculate_y_i(i, XX, Ci);
                                System.out.println("Calculated yi: "+i);
                                results.setRowMatrix(i-index1, y_i);
                            }
                        }
                    };
                    thread4.start();

                    try {
                        thread1.join();
                        thread2.join();
                        thread3.join();
                        thread4.join();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }


                sendResultsToMaster();
            }
        }
        catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            try {
                requestSocket.close();
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private RealMatrix calculateCuMatrix(int user) {
        return MatrixUtils.createRealDiagonalMatrix(C.getRow(user));

    }
    private RealMatrix calculateCiMatrix(int poi) {
        return MatrixUtils.createRealDiagonalMatrix(C.getColumn(poi));
    }
    private RealMatrix preCalculateXX() {
        RealMatrix X_T=X.transpose();
        return X_T.multiply(X);
    }
    private RealMatrix preCalculateYY() {
        RealMatrix Y_T=Y.transpose();
        return Y_T.multiply(Y);
    }
    private RealMatrix calculate_x_u(int indexPu, RealMatrix YY, RealMatrix Cu) {
        RealMatrix Y_TxCu_minus_IxY=(YY.add((Y.transpose().multiply(Cu.subtract(I))).multiply(Y))).add(LI);
        RealMatrix inverse=new LUDecomposition(Y_TxCu_minus_IxY).getSolver().getInverse();
        return (((inverse.multiply(Y.transpose())).multiply(Cu)).multiply((P.getRowMatrix(indexPu)).transpose())).transpose();
    }
    private RealMatrix calculate_y_i(int indexPi, RealMatrix XX, RealMatrix Ci) {
        RealMatrix X_TxCi_minus_IxX=(XX.add((X.transpose().multiply(Ci.subtract(I))).multiply(X))).add(LI);
        RealMatrix inverse=new LUDecomposition(X_TxCi_minus_IxX).getSolver().getInverse();
        return (((inverse.multiply(X.transpose())).multiply(Ci)).multiply(P.getColumnMatrix(indexPi))).transpose();
    }
    private void sendResultsToMaster() {
        try {
            ObjectOutputStream out;
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.writeObject(results);
            out.flush();
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static void main(String args[]) {
        new Worker().start();
    }
}

