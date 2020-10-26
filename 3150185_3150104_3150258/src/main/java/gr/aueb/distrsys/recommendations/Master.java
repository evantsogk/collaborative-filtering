package gr.aueb.distrsys.recommendations;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Master {

    private ArrayList<Socket> workerList = new ArrayList<Socket>(); //contains the sockets for communication with the workers
    private ArrayList<Long> workers_ram = new ArrayList<Long>(); //contains the ram of each worker
    private ArrayList<Integer> workers_cpu = new ArrayList<Integer>(); //contains the number of cpu nodes of each worker
    private long[] workLoad; //contains the percentage of workload to be distributed to each worker
    private Socket socket;
    private ServerSocket serverSocket;
    private static final int a=40;
    private static final int k=20;
    private static OpenMapRealMatrix P, C;
    private RealMatrix X;
    private RealMatrix Y;
    private static final float l=0.1f;
    private double previousScore=0;
    private double currentScore=0;
    private int recs;
    private double locationLat, locationLong;
    private int radius;
    private ArrayList<Poi> poiList;

    /* Initializes the server and handles new connections */
    private void initialize() {
        try {

            ReadCSV dataSet=new ReadCSV();
            dataSet.readFile("src\\main\\resources\\input_matrix_non_zeros.csv");
            OpenMapRealMatrix R=dataSet.getR();
            ReadPOIs readPois=new ReadPOIs();
            readPois.readPois();
            poiList=readPois.getPoiList();

            P = new OpenMapRealMatrix(R.getRowDimension(), R.getColumnDimension());
            C = new OpenMapRealMatrix(R.getRowDimension(), R.getColumnDimension());

            RandomGenerator rnd = new JDKRandomGenerator();
            rnd.setSeed(1);

            X = MatrixUtils.createRealMatrix(R.getRowDimension(), k);
            Y = MatrixUtils.createRealMatrix(R.getColumnDimension(), k);
            for (int i=0; i<R.getRowDimension(); i++ ) {
                for (int j=0; j<k; j++) {
                    X.setEntry(i, j, rnd.nextDouble());
                }
            }

            for (int i=0; i<R.getColumnDimension(); i++ ) {
                for (int j=0; j<k; j++) {
                    Y.setEntry(i, j, rnd.nextDouble());
                }
            }

            calculatePMatrix(R);
            calculateCMatrix(R);

            int port = 4200;
            serverSocket = new ServerSocket(port);
            System.out.println("Server Started and listening: "+ InetAddress.getLocalHost());

            Thread accept = new Thread() {
                ObjectInputStream in = null;
                ObjectOutputStream out = null;

                public void run() {
                    while (true) {
                        try {
                            socket = serverSocket.accept();
                            in = new ObjectInputStream(socket.getInputStream());
                            String type=(String) in.readObject();
                            if (type.equals("Worker")) {

                                workerList.add(socket);
                                in = new ObjectInputStream(socket.getInputStream());
                                long ram = (Long) in.readObject();
                                int cpu = (Integer) in.readObject();

                                workers_ram.add(ram);
                                workers_cpu.add(cpu);

                                System.out.println("Ram of Worker " + workerList.get(workerList.size() - 1).getRemoteSocketAddress().toString() + " is " + ram);
                                System.out.println("CPU Nodes of Worker " + workerList.get(workerList.size() - 1).getRemoteSocketAddress().toString() + " is "+ cpu);

                                out = new ObjectOutputStream(socket.getOutputStream());
                                out.writeObject(C);
                                out.writeObject(P);
                                out.writeObject(k);
                                out.flush();
                            }
                            else if (type.equals("Client")) {
                                in = new ObjectInputStream(socket.getInputStream());
                                int user=(Integer) in.readObject();
                                recs=(Integer) in.readObject();
                                locationLat=(Double) in.readObject();
                                locationLong=(Double) in.readObject();
                                radius=(Integer) in.readObject();
                                out = new ObjectOutputStream(socket.getOutputStream());
                                out.writeObject(calculateBestLocalPoisForUser(user));
                                out.flush();
                            }
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            accept.start();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void calculateCMatrix(RealMatrix R) {
        for (int i=0; i<R.getRowDimension(); i++) {
            for (int j = 0; j < R.getColumnDimension(); j++) {
                C.setEntry(i, j, 1+a*R.getEntry(i, j));
            }
        }
    }

    private void calculatePMatrix(RealMatrix R) {
        for (int i=0; i<R.getRowDimension(); i++) {
            for (int j=0; j<R.getColumnDimension(); j++) {
                if (R.getEntry(i, j)>0) P.setEntry(i, j, 1);
                else P.setEntry(i, j, 0);
            }
        }
    }

    /* Distributes XMatrix according to the percentage of workload for each worker */
    private void distributeXMatrixToWorkers() {
        long[] user_num = new long[workLoad.length]; //contains the number of xu to be sent to each worker
        int users=0;
        for (int i=0; i<workLoad.length-1; i++) {
            user_num[i]=(int) Math.round(workLoad[i]*C.getRowDimension()/100.0);
            users+=user_num[i];
        }
        user_num[workLoad.length-1]=C.getRowDimension()-users;

        Thread[] threads=new Thread[workerList.size()]; //starts a new thread for each worker
        int index_start=0;
        int index_end=(int) user_num[0]-1;
        threads[0]=new Training(true, index_start, index_end, workerList.get(0));
        for (int i=1; i<workerList.size(); i++) {
            index_start=index_end+1;
            index_end=index_start+(int) user_num[i]-1;
            threads[i]=new Training(true, index_start, index_end, workerList.get(i));
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        distributeYMatrixToWorkers();
    }

    /* Distributes YMatrix according to the percentage of workload for each worker */
    private void distributeYMatrixToWorkers() {
        long[] poi_num = new long[workLoad.length]; //contains the number of yi to be sent to each worker
        int pois=0;
        for (int i=0; i<workLoad.length-1; i++) {
            poi_num[i]=(int) Math.round(workLoad[i]*C.getColumnDimension()/100.0);
            pois+=poi_num[i];
        }
        poi_num[workLoad.length-1]=C.getColumnDimension()-pois;

        Thread[] threads=new Thread[workerList.size()]; //starts a new thread for each worker
        int index_start=0;
        int index_end=(int) poi_num[0]-1;
        threads[0]=new Training(false, index_start, index_end, workerList.get(0));
        for (int i=1; i<workerList.size(); i++) {
            index_start=index_end+1;
            index_end=index_start+(int) poi_num[i]-1;
            threads[i]=new Training(false, index_start, index_end, workerList.get(i));
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private double calculateError() {
        return Math.abs(previousScore-currentScore);
    }

    private void calculateScore() {
        previousScore=currentScore;
        currentScore=0;
        double sumXu, sumYi;
        for (int u=0; u<P.getRowDimension(); u++) {
            for (int i = 0; i < P.getColumnDimension(); i++) {
                currentScore+=C.getEntry(u,i)*Math.pow(P.getEntry(u,i)-(X.getRowMatrix(u).multiply(Y.getRowMatrix(i).transpose()).getEntry(0,0)), 2);//getEntry because it's an array 1x1
            }
        }
        sumXu=0;
        sumYi=0;
        for(int xu=0; xu<X.getRowDimension(); xu++) {
            sumXu+=Math.pow(X.getRowMatrix(xu).getFrobeniusNorm(), 2);
        }
        for(int pi=0; pi<Y.getRowDimension(); pi++) {
            sumYi+=Math.pow(Y.getRowMatrix(pi).getFrobeniusNorm(), 2);
        }
        currentScore+=l*(sumXu+sumYi);
    }

    private List<Poi> calculateBestLocalPoisForUser(int u) {
        RealMatrix rec=X.multiply(Y.transpose());
        double[] u_rec=rec.getRow(u);
        double[] u_recSorted=rec.getRow(u);
        Arrays.sort(u_recSorted);
        ArrayList<Poi> bestPois=new ArrayList<Poi>();
        int poi_id=-1;

        int i=1;
        while (true) {
            if (i<u_recSorted.length) {
                double value = u_recSorted[u_recSorted.length - i];
                for (int p = 0; p < u_rec.length; p++) {
                    if (u_rec[p] == value) {
                        poi_id = p;
                        if (P.getEntry(u, poi_id)==0) bestPois.add(poiList.get(poi_id));
                        break;
                    }
                }
            }
            else {
                break;
            }
            if (bestPois.size()==recs) break;
            i++;
        }
        return bestPois;
    }

    /* Calculates the percentage of workload for each worker according to the value of the fraction ram/cpu. */
    private void divideWorkload() {
        long[] fractions=new long[workerList.size()];
        long sum=0;
        for (int i=0; i<workerList.size(); i++) {
            fractions[i]=workers_ram.get(i)/workers_cpu.get(i);
            sum+=fractions[i];
        }
        workLoad=new long[workerList.size()];
        for (int i=0; i<workerList.size(); i++) {
            workLoad[i]=(fractions[i]*100)/sum;
        }
        Arrays.sort(workLoad);
    }

    /* Thread used for the training of each worker. */
    private class Training extends Thread {
        private boolean isX;
        private int index_start, index_end;
        private Socket worker;

        private Training(boolean isX, int index_start, int index_end, Socket worker) {
            this.isX=isX;
            this.index_start=index_start;
            this.index_end=index_end;
            this.worker=worker;
        }

        public void run () {
            ObjectInputStream in;
            ObjectOutputStream out;

            try {
                out = new ObjectOutputStream(worker.getOutputStream());
                out.writeObject(X);
                out.writeObject(Y);
                if (isX) out.writeObject(true);
                else out.writeObject(false);
                out.writeObject(index_start);
                out.writeObject(index_end);
                out.flush();

                in = new ObjectInputStream(worker.getInputStream());
                RealMatrix results=(RealMatrix)in.readObject();
                for (int i=0; i<results.getRowDimension(); i++) {
                    if (isX) X.setRow(index_start+i, results.getRow(i));
                    else Y.setRow(index_start+i, results.getRow(i));
                }
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Master master = new Master();
        master.initialize();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter any key to start training.");//wait for workers to connect to master
        String ans = scanner.next();

        master.divideWorkload();

        int steps = 5;
        double threshold=0.01;

        int i=1;
        double error=1;
        while (i<=steps && error>threshold) {
            master.distributeXMatrixToWorkers();
            master.calculateScore();
            error=master.calculateError();
            System.out.println(i+". error="+error);
            i++;
        }
        System.out.println("Training completed");
    }
}

