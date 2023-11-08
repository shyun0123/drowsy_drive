package org.techtown.login_register;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.os.AsyncTask;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class DriveStart extends AppCompatActivity {
    private Button btn_stop;
    private TextView tv_co2, tv_message;
    private MediaPlayer mediaPlayer;
    private Switch mswitch;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private String selectedAudio, co2Level = "0000", aceDe, drowsyTime;
    private Socket socket;
    private static final String PREFS_NAME = "AlarmPrefs";
    private double winCount = 0, forCount = 0;
    private int port = 12345, driveNo=0, sleepIng = 0, backCount=0;
    private String userID ;
    private String  formattedTime="0";
    private Thread mthread = null;
    private boolean forAlarm=true;
    private static final int PERMISSION_REQUEST_CODE = 1; // 아무 숫자나 사용해도 상관 없습니다.

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_start);

        if (mthread != null)
        {
            return;
        }

        Intent receivedIntent = getIntent();
        userID = receivedIntent.getStringExtra("userID");
        String userPass = receivedIntent.getStringExtra("userPass");

        if (receivedIntent != null) {
            selectedAudio = receivedIntent.getStringExtra("selectedAudio");
        }

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedAudio = preferences.getString("selectedAudio", "기본 알람음 (전자 알람)");

        btn_stop = findViewById(R.id.btn_stop);
        tv_co2 = findViewById(R.id.tv_co2);
        tv_message = findViewById(R.id.tv_message);

        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        formattedTime = currentTime.format(formatter);

        mthread = new Thread(() -> {
            Connection connection = DatabaseConnector.connect();
            try {
                if (connection != null) {
                    System.out.println("성공");

                    if (Thread.interrupted()) {
                        // 인터럽트 상태에 따른 처리
                        return;
                    }

                    String query1 = "INSERT INTO drive_info (user_id, start_time) VALUES (?, ?)";
                    PreparedStatement preparedStatement1 = connection.prepareStatement(query1);
                    preparedStatement1.setString(1, userID);
                    preparedStatement1.setString(2, formattedTime);
                    preparedStatement1.executeUpdate();
                    preparedStatement1.close();
                } else {
                    System.out.println("실패");
                    connection.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            try {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                //마지막 위치 받아오기
                // 권한 체크 및 요청
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
                    return;
                }
                Location loc_Current = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                double cur_lat = loc_Current.getLatitude(); //위도
                double cur_lon = loc_Current.getLongitude(); //경도
                if (connection != null) {
                    String query = "SELECT drive_no FROM drive_info WHERE user_id = ? AND start_time = ?";
                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                    preparedStatement.setString(1, userID);
                    preparedStatement.setString(2, formattedTime);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.next()) {
                        driveNo = resultSet.getInt("drive_no");
                    }

                    String query2 = "INSERT INTO location_info (user_id, Latitude, Longitude, gps_time) VALUES (?, ?, ?, ?)";
                    PreparedStatement preparedStatement2 = connection.prepareStatement(query2);
                    preparedStatement2.setString(1, userID);
                    preparedStatement2.setDouble(2, cur_lat);
                    preparedStatement2.setDouble(3, cur_lon);
                    preparedStatement2.setString(4, formattedTime);
                    preparedStatement2.executeUpdate();
                    preparedStatement2.close();

                    String query3 = "UPDATE location_info SET drive_no = ? WHERE drive_no IS NULL";
                    PreparedStatement preparedStatement3 = connection.prepareStatement(query3);
                    preparedStatement3.setInt(1, driveNo);
                    preparedStatement3.executeUpdate();
                    preparedStatement3.close();
                } else {
                    System.out.println("실패");
                    connection.close();
                    mthread.stop();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        mthread.start();



        Thread mthread3 = new Thread(() -> {//2번 쓰레드
            Connection connection = DatabaseConnector.connect();
            try {
                mthread.sleep(300);
                if (connection != null) {
                    System.out.println("성공");
                    String querySelect = "SELECT drive_no FROM drive_info WHERE user_id = ? AND start_time = ?";
                    PreparedStatement preparedStatement = connection.prepareStatement(querySelect);
                    if (userID != null) {
                        Log.w("서버 userID 커넥트 클래스에서", "" + userID);
                    } else {
                        Log.w("서버 userID 커넥트 클래스에서", "userID is null");
                    }
                    preparedStatement.setString(1, userID);
                    Log.w("서버 userDate 커넥트 클래스에서", "" + formattedTime);
                    preparedStatement.setString(2, formattedTime);

                    ResultSet resultSet = preparedStatement.executeQuery();
                    Log.w("서버 driveno 커넥트 클래스에서", "" + resultSet);

                    if (resultSet.first()) {
                        driveNo = resultSet.getInt("drive_no");
                        Log.w("서버 driveno 커넥트 클래스에서", "" + driveNo);
                    } else {
                        Log.w("서버 driveno 커넥트 클래스에서", "결과 없음");
                    }
                    connection.close();
                } else {
                    System.out.println("실패");
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        mthread3.start();

        Log.w("서버 현재시간", "" + formattedTime);
        connectSocekt();

        tv_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        // 데이터베이스 연결 및 쿼리 작업을 비동기로 처리
                        Thread mthread2 = new Thread(() -> {//6번 쓰레드
                            Connection connection = DatabaseConnector.connect();
                            try {
                                if (connection != null) {
                                    LocalDateTime currentTime = LocalDateTime.now();
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                    String endDrowsyTime = currentTime.format(formatter);

                                    // 추가: window_count 및 forward_vision 값을 설정
                                    String query = "UPDATE drowsy_info SET drowsy_end_time = ?, drowsy_distance = ? WHERE drowsy_start_time = ? AND drowsy_end_time IS NULL AND drowsy_distance IS NULL";
                                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                                    preparedStatement.setString(1, endDrowsyTime);
                                    preparedStatement.setInt(2, 0);
                                    preparedStatement.setString(3, drowsyTime);
                                    preparedStatement.executeUpdate();
                                    connection.close();
                                } else {
                                    connection.close();
                                }
                            } catch (SQLException e) {
                                Log.w("서버 반응", "데이터 베이스 연결 실패");
                                e.printStackTrace();
                            }
                        });
                        mthread2.start();
                        sleepIng = 0;
                        tv_message.setEnabled(false);
                        tv_message.setClickable(false);
                        tv_message.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });

        long totalMillis = 2 * 60 * 60 * 1000;
        progressBar = findViewById(R.id.progressBar);
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) (millisUntilFinished / 1000);
                progressBar.setProgress(60 - progress);
            }

            @Override
            public void onFinish() {
                if (sleepIng == 0) {
                    sleepIng = 1;
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.rest);
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            sleepIng = 0;
                        }
                    });
                    mediaPlayer.start();
                    progressBar.setProgress(0); // 프로그레스 바를 0으로 설정

                    // 타이머를 다시 시작
                    startCountdownTimer();
                }
                Toast.makeText(DriveStart.this, "가까운 쉼터에서 휴식을 취하세요.", Toast.LENGTH_SHORT).show();
            }
        };

// 타이머 시작
        countDownTimer.start();

        mswitch = findViewById(R.id.switch1);
        mswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    forAlarm = false;
                } else {
                    forAlarm = true;
                }
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("yun", "데이터베이스 btn_stop 클릭");
                mthread.interrupt();
                backCount++;
                if (countDownTimer != null) {
                    progressBar.setProgress(0);
                    mediaPlayer.stop();
                    countDownTimer.cancel();
                }
                mthread = new Thread(() -> {
                    Connection connection = DatabaseConnector.connect();
                    try {
                        if (connection != null) {
                            LocalDateTime currentTime = LocalDateTime.now();
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            String endTime = currentTime.format(formatter);

                            // 추가: window_count 및 forward_vision 값을 설정
                            String query = "UPDATE drive_info SET end_time = ?, window_count = ?, forward_vision = ? WHERE user_id = ? AND end_time IS NULL AND window_count IS NULL AND forward_vision IS NULL";
                            PreparedStatement preparedStatement = connection.prepareStatement(query);
                            preparedStatement.setString(1, endTime);
                            preparedStatement.setDouble(2, winCount);
                            preparedStatement.setDouble(3, forCount);
                            preparedStatement.setString(4, userID);
                            preparedStatement.executeUpdate();
                            connection.close();
                        } else {
                            connection.close();
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("yun", "화면전환 시작");
                            try {
                                socket.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            Intent intent = new Intent(DriveStart.this, MainMenu.class);
                            Log.d("yun", userID);
                            intent.putExtra("userID", userID);
                            startActivity(intent);
                            finish();
                        }
                    });
                });
                mthread.start();
            }
        });

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //마지막 위치 받아오기
        // 권한 체크 및 요청
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
            return;
        }
        Location loc_Current = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 위치 업데이트를 위한 요청 설정
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000); // 0.5초마다
        locationRequest.setFastestInterval(500); // 최소 0.1초 간격으로
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // 위치가 업데이트될 때 호출되는 콜백
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // 위치 정보를 처리하는 부분
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Toast.makeText(DriveStart.this, "위도: " + latitude + ", 경도: " + longitude, Toast.LENGTH_SHORT).show();
                    mthread.stop();
                    mthread = new Thread(() -> {
                        Connection connection = DatabaseConnector.connect();
                        try {
                            if (connection != null) {
                                System.out.println("성공");
                                LocalDateTime currentTime = LocalDateTime.now();
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                String formattedTime = currentTime.format(formatter);

                                if (Thread.interrupted()) {
                                    // 인터럽트 상태에 따른 처리
                                    return;
                                }

                                String query1 = "INSERT INTO location_info (user_id, Latitude, Longitude, gps_time) VALUES (?, ?, ?, ?)";
                                PreparedStatement preparedStatement1 = connection.prepareStatement(query1);
                                preparedStatement1.setString(1, userID);
                                preparedStatement1.setDouble(2, latitude);
                                preparedStatement1.setDouble(3, longitude);
                                preparedStatement1.setString(4, formattedTime);
                                preparedStatement1.executeUpdate();
                                preparedStatement1.close();

                                String query2 = "UPDATE location_info SET drive_no = ? WHERE drive_no IS NULL";
                                PreparedStatement preparedStatement2 = connection.prepareStatement(query2);
                                preparedStatement2.setInt(1, driveNo);
                                preparedStatement2.executeUpdate();
                                preparedStatement2.close();

                                connection.close();

                            } else {
                                System.out.println("실패");
                                connection.close();
                            }
                        }catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    mthread.start();
                }
            }
        };

        // 위치 권한 체크
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            startLocationUpdates();
        }

    }

    private void startLocationUpdates() {
        // 권한 체크 및 요청
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    void connectSocekt(){
// 받아오는거
        Thread checkUpdate = new Thread() {// 3번 쓰레드
            public void run() {
// ip받기
                String newip = "192.168.0.235";
// 서버 접속
                try {
                    socket = null;
                    socket = new Socket(newip, port);
                } catch (IOException e1) {
                    Log.w("서버접속못함", "서버접속못함");
                    e1.printStackTrace();
                }
                while (backCount == 0) {
                    try {
                        // 송신
                        if (socket != null) {
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            out.println("AndroidMessage");
                        }
                        Log.w("서버","AndroidMessage");
                        // 수신
                        if (socket != null) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            aceDe = in.readLine();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.w("버퍼", "버퍼생성 잘못됨");
                    }
                    // 서버에서 계속 받아옴 - 한번은 문자, 한번은 숫자를 읽음. 순서 맞춰줘야 함.
                    try {
                        int line2;
                        line2 = Integer.parseInt(aceDe);
                        Log.w("서버 line2 버퍼", line2+"");
                        if (line2 <= 3) {
                            Log.w("서버 line2 3이하 버퍼", line2 + "");
                        }
                        if (line2 == 1) {
                            if (sleepIng == 0) {
                                playMusic();
                                // 데이터베이스 연결 및 쿼리 작업을 비동기로 처리
                                Log.w("서버 반응", "플레이 뮤직 이후" + "");
                                Thread mthread5 = new Thread(() -> { // 5번 쓰레드
                                    Log.w("서버 반응", "쓰레드 들어옴" + "");
                                    Connection connection = null;
                                    try {
                                        Log.w("서버 반응", "트라이 들어옴1" + "");
                                        connection = DatabaseConnector.connect();
                                        Log.w("서버 반응", "트라이 들어옴2" + "");
                                        if (connection != null) {
                                            Log.w("서버 반응", "조건문 들어옴" + "");
                                            LocalDateTime currentTime = LocalDateTime.now();
                                            DateTimeFormatter drowsyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                            drowsyTime = currentTime.format(drowsyFormatter);
                                            String query = "INSERT INTO drowsy_info (drive_no, drowsy_start_time,user_id) VALUES (?, ?, ?)";
                                            PreparedStatement preparedStatement = connection.prepareStatement(query);
                                            preparedStatement.setInt(1, driveNo);
                                            Log.w("서버 반응", "drive_no 검사 통과서버" + "");
                                            preparedStatement.setString(2, drowsyTime);
                                            preparedStatement.setString(3, userID);
                                            preparedStatement.executeUpdate();
                                            connection.close();
                                        } else {
                                            Log.w("서버 반응", "데이터 베이스 연결 실패");
                                        }
                                    } catch (SQLException e) {
                                        Log.w("서버 반응", "데이터 베이스 예외 발생");
                                        e.printStackTrace();
                                    } finally {
                                        if (connection != null) {
                                            try {
                                                connection.close();
                                            } catch (SQLException e) {
                                                Log.w("서버 반응", "데이터 베이스 연결 닫기 실패");
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                });
                                mthread5.start();

                                // 데이터베이스 연결이 시작되면 sleepIng 값을 업데이트
                            }
                        }
                        else if (line2 == 2) {
                            winCount++;
                        } else if (line2 == 3) {
                            if (forAlarm){
                                if (sleepIng ==0) {
                                    sleepIng = 1;
                                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.front);
                                    mediaPlayer.start();
                                    forCount++;
                                }
                            }
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    sleepIng = 0;
                                }
                            });
                        } else if (line2 > 0 ) {
                            co2Level = String.valueOf(line2);
                            // UI 업데이트를 메인 스레드에서 실행
                            runOnUiThread(new Runnable() {//4번 쓰레드
                                @Override
                                public void run() {
                                    tv_co2.setText(co2Level+" ppm");
                                }
                            });
                        }
                    } catch (Exception e) {
                        // 예외 처리 코드
                    }
                }
            }
        };
// 소켓 접속 시도, 버퍼생성
        checkUpdate.start();
        if (backCount ==1){
            checkUpdate.interrupt();
        }
    }
    private void startCountdownTimer() {
        countDownTimer= new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) (millisUntilFinished / 1000);
                progressBar.setProgress(60 - progress);
            }

            @Override
            public void onFinish() {
                if (sleepIng == 0) {
                    sleepIng = 1;
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.rest);
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            sleepIng = 0;
                        }
                    });
                    mediaPlayer.start();
                    progressBar.setProgress(0); // 프로그레스 바를 0으로 설정

                    // 타이머를 다시 시작
                    startCountdownTimer();
                }
                Toast.makeText(DriveStart.this, "가까운 쉼터에서 휴식을 취하세요.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void playMusic() {
        Intent receivedIntent = getIntent();
        Log.w("서버 ", "뮤직 스타트" );
        if (receivedIntent != null) {
            selectedAudio = receivedIntent.getStringExtra("selectedAudio");
        }
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedAudio = preferences.getString("selectedAudio", "기본 알람음 (전자 알람)");
        sleepIng = 1;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_message.setEnabled(true);
                tv_message.setClickable(true);
                tv_message.setVisibility(View.VISIBLE);
            }
        });
        if (selectedAudio != null) {
            if (selectedAudio.equals("기본 알람음 (기상 나팔)")) {
                mediaPlayer = MediaPlayer.create(this, R.raw.basic1);
            } else if (selectedAudio.equals("기본 알람음 (전자 알람)")) {
                mediaPlayer = MediaPlayer.create(this, R.raw.basic2);
            } else if (selectedAudio.equals("기본 알람음 (아날로그 알람)")) {
                mediaPlayer = MediaPlayer.create(this, R.raw.basic3);
            } else {
                // 선택한 알람음이 사용자가 추가한 경우, 해당 파일을 재생합니다.
                Uri selectedFileUri = Uri.fromFile(new File(getFilesDir(), selectedAudio));
                mediaPlayer = MediaPlayer.create(this, selectedFileUri);
            }
        } else {
            mediaPlayer = MediaPlayer.create(this, R.raw.basic1);
        }
        mediaPlayer.setLooping(true);
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}