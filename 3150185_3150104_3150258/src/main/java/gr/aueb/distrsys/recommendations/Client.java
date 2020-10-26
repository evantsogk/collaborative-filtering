package gr.aueb.distrsys.recommendations;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;

/* Connects to Master and asks for the recommendations for the given user */
public class Client {
    private Socket requestSocket;
    private int user;
    private String ip; //ip address of master
    private List<Poi> recs; //list with the recommendations

    private Client(int user, String ip) {
        this.user=user;
        this.ip=ip;
        initializeClient();
    }

    /* Creates the connection with the Master. */
    private void initializeClient() {
        ObjectOutputStream out;
        try {

            requestSocket = new Socket(ip, 4200);
            out= new ObjectOutputStream(requestSocket.getOutputStream());
            out.writeObject("Client");
            out.flush();


        }
        catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /* Sends to the Master the user id and receives the recommendations. */
    private void sendQueryToServer() {
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.writeObject(user);
            out.writeObject(3);
            out.writeObject(40.731356448341);
            out.writeObject(-73.988671302795);
            out.writeObject(10);
            out.flush();
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
        getResults();
    }

    /* Receives the list with the recommendations. */
    private void getResults() {
        ObjectInputStream in;
        try {
            in = new ObjectInputStream(requestSocket.getInputStream());
            recs = (List) in.readObject();
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /* Prints the ID of the recommended pois. */
    private void showResults() {
        for (Poi poi : recs) {
            System.out.println(poi.getId());
        }
    }

    public static void main(String args[]) {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Insert IP address of Master: ");
        String ip = scanner.next();

        int user=0;
        while (user>-1) {
            System.out.println("Insert user for query or exit with -1):");
            user = scanner.nextInt();
            if (user==-1) break;
            Client client=new Client(user, ip);
            client.sendQueryToServer();
            client.showResults();
        }
    }
}

