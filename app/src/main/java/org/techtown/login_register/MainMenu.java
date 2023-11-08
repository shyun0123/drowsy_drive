package org.techtown.login_register;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MainMenu extends AppCompatActivity {
    private Button btn_logout, btn_id;
    private ImageButton ibtn_start, ibtn_info,ibtn_alarm,ibtn_record;
    private String userName;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // 인텐트로부터 사용자 아이디를 받아옴


        Intent receivedIntent = getIntent();
        String userID = receivedIntent.getStringExtra("userID");
        String userPass = receivedIntent.getStringExtra("userPass");

        Thread mthread = new Thread(() -> {
            Connection connection = DatabaseConnector.connect();
            try {
                if (connection != null) {
                    System.out.println("성공");
                    String querySelect = "SELECT user_name FROM user_info WHERE user_id = ?";
                    PreparedStatement preparedStatement = connection.prepareStatement(querySelect);
                    preparedStatement.setString(1, userID);

                    ResultSet resultSet = preparedStatement.executeQuery();

                    if (resultSet.first()){
                        userName = resultSet.getString("user_name");
                    }
                    connection.close();

                } else {
                    System.out.println("실패");
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        mthread.start();

        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION ,
                Manifest.permission.ACCESS_COARSE_LOCATION ,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
        };
        checkPermissions(permissions);

        btn_logout = findViewById(R.id.btn_logout);
        ibtn_start = findViewById(R.id.ibtn_start);
        ibtn_info = findViewById(R.id.ibtn_info);
        ibtn_alarm = findViewById(R.id.ibtn_alarm);
        ibtn_record = findViewById(R.id.ibtn_record);
        btn_id = findViewById(R.id.btn_id);

        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenu.this, loginactivity.class);
                intent.putExtra("userID", userID);
                startActivity(intent);
                finish();
            }
        });

        btn_id.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainMenu.this);
                builder.setTitle("아이디");
                builder.setMessage(String.format("아이디 : %s\n이름 : %s", userID, userName));
                builder.setPositiveButton("확인", null);
                builder.create().show();
            }
        });

        ibtn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenu.this, DriveStart.class);
                intent.putExtra("userID", userID);
                intent.putExtra("userPass", userPass);
                startActivity(intent);
            }
        });

        ibtn_alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenu.this, AlarmOption.class);
                intent.putExtra("userID", userID);
                startActivity(intent);
            }
        });

        ibtn_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenu.this, DriveInfo.class);
                intent.putExtra("userID", userID);
                intent.putExtra("userPass", userPass);
                startActivity(intent);
            }
        });

        ibtn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenu.this, DriveRecord.class);
                intent.putExtra("userID", userID);
                intent.putExtra("userPass", userPass);
                startActivity(intent);
            }
        });
    }

    public void checkPermissions(String[] permissions) {
        ArrayList<String> targetList = new ArrayList<>();

        for (String curPermission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, curPermission);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                targetList.add(curPermission);
            }
        }

        if (!targetList.isEmpty()) {
            String[] targets = new String[targetList.size()];
            targetList.toArray(targets);
            ActivityCompat.requestPermissions(this, targets, 101);
        }
    }

    private double waitTime = 0;
    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - waitTime <= 1500) {
            // 한 번 더 뒤로가기 버튼을 눌렀을 때의 작업 (종료 등)
            super.onBackPressed();
        } else {
            // 한 번 더 뒤로가기 버튼을 누를 것을 안내
            Toast.makeText(this, "뒤로가기 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
        }

        waitTime = currentTime;
    }
}