package com.example.configled;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 123;
    private static final String ESP32_IP = "192.168.4.1";
    private static final int ESP32_PORT = 80;

    private List<Button> onButtonsList = new ArrayList<>();
    private List<Button> offButtonsList = new ArrayList<>();
    private Button addLedButton;

    private LinearLayout containerLayout;

    private Socket socket;
    private OutputStream outputStream;
    private BufferedReader reader;



    private static final String URL = "jdbc:mysql://localhost:3306/configled" ;
    private static final String USER = "root";
    private static final String PASSWORD = "ayoub 2001";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET},
                    MY_PERMISSIONS_REQUEST_INTERNET);
        }
        Button wificonnectButton = findViewById(R.id.wifiConnectButton);
        containerLayout = findViewById(R.id.containerLayout);
        addLedButton = findViewById(R.id.addLedButton);
        wificonnectButton.setOnClickListener(v -> {connectToESP32();});
        addLedButton.setOnClickListener(v -> addLedButtonClicked());
    }

    private void addLedButtonClicked() {
        int ledIndex = containerLayout.getChildCount()  ;
        createLedField(ledIndex ,"LED " + ledIndex );
    }
    @SuppressLint("SetTextI18n")
    private void createLedField(int ledIndex,String ledName) {
        LinearLayout ledLayout = new LinearLayout(this);
        LinearLayout.LayoutParams ledLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ledLayoutParams.width = 980;
        ledLayoutParams.height = 200;
        ledLayoutParams.setMargins(0, 10, 16, 0);
        ledLayout.setLayoutParams(ledLayoutParams);
        ledLayout.setOrientation(LinearLayout.HORIZONTAL);
        ledLayout.setGravity(Gravity.CENTER);
        ledLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_shape));
        ledLayout.setPadding(0, 10, 20, 16);

        TextView ledTextView = new TextView(this);
        LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textViewParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        textViewParams.setMargins(140, 0, 200, 0); // Set margins as per your requirement
        ledTextView.setLayoutParams(textViewParams);
        ledTextView.setText(ledName);
        ledTextView.setTextColor(Color.BLACK);
        ledTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

        Button onButton = new Button(this);
        onButton.setOnClickListener(v -> sendCommand("ON_" + ledIndex + "_" + ledName + "\n"));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.width = 230;
        buttonParams.height = 140;
        buttonParams.setMargins(0, 0, 40, 0);
        onButton.setLayoutParams(buttonParams);
        onButton.setText("ON");
        onButton.setTextColor(Color.WHITE);
        onButton.setBackground(ContextCompat.getDrawable(this, R.drawable.selector_button));

        Button offButton = new Button(this);
        offButton.setOnClickListener(v -> sendCommand("OFF_" + ledIndex + "_" + ledName + "\n"));

        buttonParams.width =230;
        buttonParams.height =140;
        offButton.setLayoutParams(buttonParams);
        offButton.setText("OFF");
        offButton.setTextColor(Color.WHITE);
        offButton.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_shape_off));

        ledLayout.addView(ledTextView);
        ledLayout.addView(onButton);
        ledLayout.addView(offButton);

        containerLayout.addView(ledLayout, ledIndex);

        onButtonsList.add(onButton);
        offButtonsList.add(offButton);

        onButton.setOnClickListener(v -> sendCommand("ON_" + ledIndex + "\n"));
        offButton.setOnClickListener(v -> sendCommand("OFF_" + ledIndex + "\n"));
    }


    private void connectToESP32() {
        new Thread(() -> {
            try {
                socket = new Socket(ESP32_IP, ESP32_PORT);
                outputStream = socket.getOutputStream();
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                runOnUiThread(() -> {
                    addLedButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Connected to ESP32", Toast.LENGTH_SHORT).show();
                });
            } catch (final IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to connect to ESP32: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendCommand(final String command) {
        new Thread(() -> {
            if (socket != null && socket.isConnected()) {
                try {
                    outputStream.write(command.getBytes());
                    outputStream.flush();

                    final String response = reader.readLine();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Response: " + response, Toast.LENGTH_SHORT).show());
                } catch (final IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send command: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            } else {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Command: " + command, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (socket != null) {
                socket.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void insertLedInfo(String ledName) {
        String url = "jdbc:mysql://localhost:3306/configled" ;
        String username = "root";
        String password = "ayoub 2001";

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(url, username, password);
            System.out.println("Connected to the database");

            Statement statement = connection.createStatement();
            //PreparedStatement insertQuery = connection.prepareStatement("insert into configled.led values('3','LED 3','ON');");
            PreparedStatement insertQuery = connection.prepareStatement("INSERT INTO `configled`.`led` (`ledName`, `status`) VALUES ('" + ledName + "', 'OFF')");


            int status = insertQuery.executeUpdate();
            if (status != 0){
                System.out.println("Database was Connection");
            }
            System.out.println("Successfully inserted LED info");
            statement.close();
            connection.close();


        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Failed to load MySQL driver", Toast.LENGTH_SHORT).show();
        } catch (SQLException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Failed to insert LED info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }


    }
    private void connectToMyDataBase() {
        Connection connection = null;
        String url = "jdbc:mysql://localhost:3306/configled";
        String username = "root";
        String password = "ayoub 2001";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection(url, username, password);
            Toast.makeText(MainActivity.this, "Connected to DB", Toast.LENGTH_SHORT).show();
        } catch (SQLException e) {
            Toast.makeText(MainActivity.this, "SQLException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            Toast.makeText(MainActivity.this, "ClassNotFoundException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Toast.makeText(MainActivity.this, "IllegalAccessException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (InstantiationException e) {
            Toast.makeText(MainActivity.this, "InstantiationException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}