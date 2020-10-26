package gr.aueb.distrsys.recommendations;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    AlertDialog.Builder dialogBuilder, failedBuilder;
    AlertDialog alert;
    private Socket requestSocket;
    private List<Poi> recsList;
    private String user;
    private String lat;
    private String Long;
    private String radius;
    private String recs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        final EditText txtUser=findViewById(R.id.input_user);
        final EditText txtLat=findViewById(R.id.input_lat);
        final EditText txtLong=findViewById(R.id.input_long);
        final EditText txtRadius=findViewById(R.id.input_radius);
        final EditText txtRecs=findViewById(R.id.input_number);

        final Button btnShow=findViewById(R.id.btn_show);

        btnShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user=txtUser.getText().toString();
                lat=txtLat.getText().toString();
                Long=txtLong.getText().toString();
                radius=txtRadius.getText().toString();
                recs=txtRecs.getText().toString();

                if (user.trim().equals("") || lat.trim().equals("") || Long.trim().equals("") || radius.trim().equals("") || recs.trim().equals("")) {
                    Toast.makeText(getBaseContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                }
                else {
                    final View viewInflated = LayoutInflater.from(MainActivity.this).inflate(R.layout.ip_input, (ViewGroup) getWindow().getDecorView(), false);
                    final EditText input = (EditText) viewInflated.findViewById(R.id.input);
                    dialogBuilder= new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme);
                    dialogBuilder.setView(viewInflated);

                    dialogBuilder.setTitle("Master's IP");

                    dialogBuilder.setCancelable(true);

                    dialogBuilder.setPositiveButton(
                            "OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String ip=input.getText().toString();

                                    AsyncTaskRunner runner = new AsyncTaskRunner();
                                    runner.execute(ip);
                                }

                            });
                    dialogBuilder.setNegativeButton(
                            "CANCEL",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    alert = dialogBuilder.create();
                    alert.show();

                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private class AsyncTaskRunner extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;

        @Override
        protected String doInBackground(String... params) {

            ObjectOutputStream out;
            String connection="failed";
            try {

                requestSocket = new Socket(params[0].trim(), 4200);
                out= new ObjectOutputStream(requestSocket.getOutputStream());
                out.writeObject("Client");
                out.flush();
                sendQueryToServer();
                connection="successful";
            }
            catch (UnknownHostException unknownHost) {
                System.err.println("You are trying to connect to an unknown host!");
            }
            catch (IOException ioException) {
                ioException.printStackTrace();
            }

            return connection;
        }

        private void sleep(int i) {
            try {
                Thread.sleep(i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            if (result.equals("successful")) {
                Intent maps = new Intent(MainActivity.this, MapsActivity.class);
                maps.putExtra("pois", (ArrayList) recsList);
                maps.putExtra("lat", Double.parseDouble(lat));
                maps.putExtra("long", Double.parseDouble(Long));
                maps.putExtra("radius", Integer.parseInt(radius));
                startActivity(maps);
                finish();
            }
            else {
                failedBuilder= new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme);
                failedBuilder.setMessage("Connection failed!");
                failedBuilder.setPositiveButton(
                        "Ok",null);

                alert =failedBuilder.create();
                alert.show();
            }
        }


        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(MainActivity.this, R.style.Theme_AppCompat_DayNight_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Connecting...");

            View view = MainActivity.this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            progressDialog.show();
        }

    }

    /* Sends to the Master the user id and receives the recommendations. */
    private void sendQueryToServer() {
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.writeObject(Integer.parseInt(user));
            out.writeObject(Integer.parseInt(recs));
            out.writeObject(Double.parseDouble(lat));
            out.writeObject(Double.parseDouble(Long));
            out.writeObject(Integer.parseInt(radius));
            out.flush();
            getResults();
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /* Receives the list with the recommendations. */
    private void getResults() {
        ObjectInputStream in;
        try {
            in = new ObjectInputStream(requestSocket.getInputStream());
            recsList = (List) in.readObject();
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
