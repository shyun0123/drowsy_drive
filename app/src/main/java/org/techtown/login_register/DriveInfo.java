package org.techtown.login_register;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.lifecycle.viewmodel.CreationExtras;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.ParseException;
import java.util.stream.Collectors;

public class DriveInfo extends AppCompatActivity implements OnMapReadyCallback {
    private Button btn_back, btn_forward, btn_drowsy, btn_window, btn_total;
    private ImageButton imageButton;
    private EditText editText;
    private Spinner spinner;
    private GoogleMap mMap;
    private Thread dthread;
    private SupportMapFragment mapFragment;
    private ArrayAdapter<Integer> spinnerAdapter;
    private Handler databaseHandler;
    private Map<Integer, List<LatLng>> drivePaths = new HashMap<>();
    private List<Integer> driveNumbers = new ArrayList<>();
    private String userID;
    private List<String> location_time = new ArrayList<>();
    private List<String> drowsy_start_time = new ArrayList<>();
    private List<String> drowsy_end_time = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_info);

        btn_drowsy = findViewById(R.id.btn_drowsy);
        btn_total = findViewById(R.id.btn_total);
        btn_forward = findViewById(R.id.btn_forward);
        btn_window = findViewById(R.id.btn_window);

        Intent intent = getIntent();
        if (intent.hasExtra("userID")) {
            userID = intent.getStringExtra("userID");
        }

        // 리스트 초기화를 onCreate에서 수행
        location_time = new ArrayList<>();
        drowsy_start_time = new ArrayList<>();
        drowsy_end_time = new ArrayList<>();

        spinner = findViewById(R.id.sp_no);
        spinner.setVisibility(View.GONE);

        mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getView().setVisibility(View.GONE);

        Log.d("MapReady", "Map is ready!"); // 디버그 로그 추가

        editText = findViewById(R.id.et_date);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        imageButton = findViewById(R.id.ibtn_date);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText != null) {
                    spinner.setVisibility(View.VISIBLE);
                    mapFragment.getView().setVisibility(View.VISIBLE);
                    mapFragment.getMapAsync(DriveInfo.this);

                    String userDate = editText.getText().toString();

                    // 데이터베이스와 연결하여 운전 기록 검색하는 스레드 시작
                    new Thread(new ConnectToDatabaseTask(userDate)).start();
                }
                else {
                    Toast.makeText(DriveInfo.this, "날짜를 입력해주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedItem = spinner.getSelectedItem().toString();

                // 선택한 아이템에서 운전 번호 추출
                int driveNo = Integer.parseInt(selectedItem.split("\\[")[0]);

                // 데이터베이스에서 운전 정보 가져오는 스레드 시작
                new Thread(new FetchDriveInfoTask(driveNo)).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 선택한 것이 없을 때는 아무 작업도 하지 않음
            }
        });

        btn_back = findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DriveInfo.this, MainMenu.class);
                intent.putExtra("userID", userID);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        // 데이터베이스 작업을 수행할 핸들러 초기화
        databaseHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        // 데이터베이스 작업이 완료되면 UI 업데이트
                        List<String> driveRecords = (List<String>) msg.obj;

                        if (!driveRecords.isEmpty()) {
                            // 스피너에 운전 기록 표시
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(DriveInfo.this, android.R.layout.simple_spinner_dropdown_item, driveRecords);
                            spinner.setAdapter(adapter);
                        } else {
                            // 해당 사용자와 날짜에 대한 기록이 없는 경우
                            Toast.makeText(getApplicationContext(), "운전 기록이 없습니다.", Toast.LENGTH_SHORT).show();
                            spinner.setAdapter(null); // 스피너 초기화
                        }
                        break;
                    case 2:
                        // 데이터베이스 작업이 완료되면 UI 업데이트
                        DriveInfoData driveInfo = (DriveInfoData) msg.obj;

                        if (driveInfo != null) {
                            // 버튼 텍스트 업데이트
                            btn_forward.setText(driveInfo.getForwardVision() + "회");
                            btn_total.setText(driveInfo.getTotalDistance() + "km");
                            btn_window.setText(driveInfo.getWindowCount() + "회");
                            btn_drowsy.setText(driveInfo.getDrowsyDistance() + "m");
                        }
                        break;
                }
            }
        };
    }

    private void showDatePickerDialog() {
        // 현재 날짜 설정
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // DatePickerDialog 생성
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int selectedYear, int selectedMonth, int selectedDay) {
                        // 사용자가 선택한 날짜를 EditText에 표시
                        calendar.set(selectedYear, selectedMonth, selectedDay);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        editText.setText(dateFormat.format(calendar.getTime()));
                    }
                }, year, month, day);

        // DatePickerDialog를 표시
        datePickerDialog.show();
    }

    int driveNo;

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();

        Intent receivedIntent = getIntent();
        String userID = receivedIntent.getStringExtra("userID");

        editText = findViewById(R.id.et_date);
        String gps_time = editText.getText().toString();

        btn_total = findViewById(R.id.btn_total);

        dthread = new Thread(() -> {
            Connection connection = null;
            try {
                connection = DatabaseConnector.connect();

                if (connection != null) {
                    Log.d("DriveInfo", "데이터베이스 연결 성공");

                    Map<Integer, List<LatLng>> drivePaths = new HashMap<>(); // 각 drive_no에 대한 위치 정보를 저장할 맵

                    String query = "SELECT Latitude, Longitude, drive_no, gps_time FROM location_info WHERE user_id = ? AND DATE(gps_time) = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        preparedStatement.setString(1, userID);
                        preparedStatement.setString(2, gps_time);
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            while (resultSet.next()) {
                                driveNo = resultSet.getInt("drive_no");
                                double latitude = resultSet.getDouble("Latitude");
                                double longitude = resultSet.getDouble("Longitude");
                                location_time.add(resultSet.getTimestamp("gps_time").toString());

                                String time_query = "SELECT drowsy_start_time, drowsy_end_time FROM drowsy_info WHERE user_id = ? AND Date(drowsy_start_time) = ? AND drive_no = ?";
                                try (PreparedStatement preparedStatement2 = connection.prepareStatement(time_query)) {
                                    preparedStatement2.setString(1, userID);
                                    preparedStatement2.setString(2, gps_time);
                                    preparedStatement2.setInt(3, driveNo); // Add the driveNo condition
                                    try (ResultSet resultSet2 = preparedStatement2.executeQuery()) {
                                        while (resultSet2.next()) {
                                            drowsy_start_time.add(resultSet2.getTimestamp("drowsy_start_time").toString());
                                            drowsy_end_time.add(resultSet2.getTimestamp("drowsy_end_time").toString());
                                        }
                                    }
                                }

                                drowsy_start_time = drowsy_start_time.stream().distinct().collect(Collectors.toList());
                                drowsy_end_time = drowsy_end_time.stream().distinct().collect(Collectors.toList());

                                // drive_no를 기준으로 맵에 위치 정보 추가
                                if (!drivePaths.containsKey(driveNo)) {
                                    drivePaths.put(driveNo, new ArrayList<>());
                                }
                                drivePaths.get(driveNo).add(new LatLng(latitude, longitude));
                            }
                        }
                    }

//                    Map<Integer, PolylineOptions> drivePolylineOptions = new HashMap<>();
                    List<Double> drowsy_check = new ArrayList<>();

                    runOnUiThread(() -> {
                        double totalDistance = 0.0;
                        int color_time = 0;
                        double All_drowsy_km = 0.0;
                        double totalkm = 0.0;
                        double drowsy_km = 0.0;
                        drowsy_check.add(0.0);

                        if (drivePaths.isEmpty()) {
                            Log.d("DriveInfo", "No data available for the selected date");
                            Toast.makeText(this, "주행정보가 없습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        DecimalFormat decimalFormat = new DecimalFormat("#.##");
                        // 각 drive_no에 대한 위치 정보를 지도에 표시
                        for (Map.Entry<Integer, List<LatLng>> entry : drivePaths.entrySet()) {
                            int driveNo = entry.getKey();
                            List<LatLng> path = entry.getValue();

                            // 마커 및 PolylineOptions 객체를 루프 안에서 생성
                            for (color_time = 0; color_time < path.size() - 1; color_time++) {
                                LatLng startLocation = path.get(color_time);
                                LatLng endLocation = path.get(color_time + 1);
                                // PolylineOptions 객체를 가져오거나 생성
                                PolylineOptions polylineOptions = new PolylineOptions().width(15);
                                polylineOptions.color(Color.GREEN);

                                System.out.println(drowsy_start_time);

                                double segmentDistance = calculateDistance(startLocation, endLocation);
                                totalDistance += segmentDistance;
                                totalkm += segmentDistance;

                                // 좌표를 PolylineOptions 객체에 추가
                                polylineOptions.add(startLocation, endLocation);
                                for (int check = 0; check < drowsy_start_time.size(); check++)
                                {
                                    // location_time에 따라 색상 설정
                                    if (isInDrowsyTime(location_time.get(color_time), drowsy_start_time.get(check), drowsy_end_time.get(check))) {
                                        drowsy_km += segmentDistance;
                                        All_drowsy_km += segmentDistance;
                                        String showtdrowsyDistance = decimalFormat.format(drowsy_km);
                                        try {
                                            new Thread(new DrowsyToDatabaseTask(Double.parseDouble(showtdrowsyDistance),drowsy_start_time.get(check), drowsy_end_time.get(check), DriveInfo.this.driveNo, userID)).start();

                                        } catch (ParseException e) {
                                            throw new RuntimeException(e);
                                        }
                                        polylineOptions.color(Color.RED);
                                        // 마커와 함께 PolylineOptions 객체를 사용할 수 있도록 리스트에 추가
                                        mMap.addPolyline(polylineOptions);

                                        break;
                                    } else {
                                        // 여기서 RED 대신에 다른 컬러를 지정할 수 있습니다.
                                        //drowsy_km = 0.0;
                                        polylineOptions.color(Color.GREEN);
                                    }
                                }
                                // 마커와 함께 PolylineOptions 객체를 사용할 수 있도록 리스트에 추가
                                mMap.addPolyline(polylineOptions);
                            }

                            if (color_time  == path.size() - 1) {
                                LatLng startLocation = path.get(color_time - 1);
                                LatLng endLocation = path.get(color_time);

                                double segmentDistance = calculateDistance(startLocation, endLocation);
                                totalDistance += segmentDistance;
                                totalkm += segmentDistance;

                                // PolylineOptions 객체를 가져오거나 생성
                                PolylineOptions polylineOptions = new PolylineOptions().width(15);
                                polylineOptions.color(Color.GREEN);

                                // 좌표를 PolylineOptions 객체에 추가
                                polylineOptions.add(startLocation, endLocation);

                                for (int check = 0; check < drowsy_start_time.size(); check++)
                                {
                                    // location_time에 따라 색상 설정
                                    if (isInDrowsyTime(location_time.get(color_time), drowsy_start_time.get(check), drowsy_end_time.get(check))) {
                                        drowsy_km += segmentDistance;
                                        All_drowsy_km += segmentDistance;
                                        String showtdrowsyDistance = decimalFormat.format(drowsy_km);
                                        try {
                                            new Thread(new DrowsyToDatabaseTask(Double.parseDouble(showtdrowsyDistance),drowsy_start_time.get(check), drowsy_end_time.get(check), DriveInfo.this.driveNo, userID)).start();

                                        } catch (ParseException e) {
                                            throw new RuntimeException(e);
                                        }
                                        polylineOptions.color(Color.RED);
                                        // 마커와 함께 PolylineOptions 객체를 사용할 수 있도록 리스트에 추가
                                        mMap.addPolyline(polylineOptions);
                                        break;
                                    } else {
                                        // 여기서 RED 대신에 다른 컬러를 지정할 수 있습니다.
                                        //drowsy_km = 0.0;
                                        polylineOptions.color(Color.GREEN);
                                        // 마커와 함께 PolylineOptions 객체를 사용할 수 있도록 리스트에 추가
                                    }
                                }
                                // 마커와 함께 PolylineOptions 객체를 사용할 수 있도록 리스트에 추가
                                mMap.addPolyline(polylineOptions);
                            }
                            new Thread(new DrowsyUpdateTask(driveNo)).start();
                            drowsy_km = 0.0;
                            totalkm /= 1000;
                            decimalFormat = new DecimalFormat("#.##");
                            String showtotalkm = decimalFormat.format(totalkm);
                            new Thread(new UpdateToDatabaseTask(Double.parseDouble(showtotalkm), userID, DriveInfo.this.driveNo)).start();
                        }
                        totalDistance /= 1000;
                        decimalFormat = new DecimalFormat("#.##");
                        String showtotalDistance = decimalFormat.format(totalDistance);
                        btn_total.setText(showtotalDistance + "km");
                        All_drowsy_km /= 1000;
                        String showdorwsyDistance = decimalFormat.format(All_drowsy_km);
                        btn_drowsy.setText(showdorwsyDistance + "km");

                        // 모든 drive_no에 대한 경로를 포함하는 LatLngBounds 생성
                        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                        for (List<LatLng> path : drivePaths.values()) {
                            for (LatLng location : path) {
                                boundsBuilder.include(location);
                            }
                        }
                        LatLngBounds bounds = boundsBuilder.build();

                        // 최적의 줌을 설정하여 카메라 이동
                        int padding = 100; // 마커와 화면 경계 간의 여백
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                    });
                } else {
                    Log.e("DriveInfo", "데이터베이스 연결 실패");
                    // 사용자에게 어떤 피드백을 줄지 고려
                }
            } catch (SQLException e) {
                Log.e("DriveInfo", "SQL Exception: " + e.getMessage());
                // 사용자에게 어떤 피드백을 줄지 고려
            } finally {
                // 데이터베이스 연결 닫기
                DatabaseConnector.closeConnection(connection);
            }
        });
        dthread.start();
    }

    private boolean isInDrowsyTime(String gpsTime, String startTime, String endTime) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            Date gpsDateTime = dateFormat.parse(gpsTime);
            Date startTimeDate = dateFormat.parse(startTime);
            Date endTimeDate = dateFormat.parse(endTime);

            System.out.println(gpsTime);
            System.out.println(startTime);
            System.out.println(endTime);

            System.out.println((gpsDateTime.after(startTimeDate) || gpsDateTime.equals(startTimeDate))
                    && (gpsDateTime.before(endTimeDate) || gpsDateTime.equals(endTimeDate)));

            // gps_time이 시작 시간과 종료 시간 사이에 있는지 확인
            // gps_time이 시작 시간과 종료 시간 사이에 있는지 확인
            return (gpsDateTime.after(startTimeDate) || gpsDateTime.equals(startTimeDate))
                    && (gpsDateTime.before(endTimeDate) || gpsDateTime.equals(endTimeDate));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return false;
    }

    private double calculateDistance(LatLng start, LatLng end) {
        float[] results = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
        return results[0];
    }

    private class UpdateToDatabaseTask implements Runnable {
        private Double user_totalkm;
        private String userID;
        private int drive_No;

        public UpdateToDatabaseTask(Double user_totalkm, String userID, int drive_No) {
            this.user_totalkm = user_totalkm;
            this.userID = userID;
            this.drive_No = drive_No;
        }

        @Override
        public void run() {
            Connection connection = null;
            try {
                connection = DatabaseConnector.connect();
                if (connection != null) {
                    // WHERE 절에 drive_no를 추가한 SQL 쿼리
                    String query = "UPDATE drive_info SET total_distance = ? WHERE user_id = ? AND drive_no = ?";

                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        preparedStatement.setDouble(1, user_totalkm);
                        preparedStatement.setString(2, userID);
                        preparedStatement.setInt(3, drive_No);

                        // 업데이트 실행
                        int rowsAffected = preparedStatement.executeUpdate();

                        if (rowsAffected > 0) {
                            System.out.println("총 거리가 성공적으로 업데이트되었습니다.");
                        } else {
                            System.out.println("행이 업데이트되지 않았습니다. 사용자 또는 drive_no를 찾을 수 없을까요?");
                            // 사용자 또는 drive_no를 찾을 수 없는 경우를 처리
                        }
                    }
                } else {
                    System.err.println("데이터베이스에 연결하지 못했습니다.");
                    // 데이터베이스 연결 실패 시 처리
                }
            } catch (SQLException e) {
                System.err.println("SQL 예외: " + e.getMessage());
                // SQL 예외 처리
            } finally {
                // 데이터베이스 연결 닫기
                DatabaseConnector.closeConnection(connection);
            }
        }
    }

    private class DrowsyToDatabaseTask implements Runnable {
        private Double drowsy_km;
        private String drowsy_start_time;
        private String drowsy_end_time;
        private int drive_No;
        private String userID;

        public DrowsyToDatabaseTask(Double drowsy_km, String drowsy_start_time, String drowsy_end_time, int drive_No, String userID) throws ParseException {
            this.drowsy_km = drowsy_km / 1000;
            this.drowsy_start_time = drowsy_start_time;
            this.drowsy_end_time = drowsy_end_time;
            this.drive_No = drive_No;
            this.userID = userID;
        }

        @Override
        public void run() {
            Connection connection = null;
            try {
                connection = DatabaseConnector.connect();

                if (connection != null) {
                    // 졸음운전 거리 업데이트 query
                    String query = "UPDATE drowsy_info SET drowsy_distance = ? WHERE drive_no = ? AND drowsy_start_time = ? AND drowsy_end_time = ? AND user_id = ?";

                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        preparedStatement.setDouble(1, drowsy_km);
                        preparedStatement.setInt(2, drive_No);
                        preparedStatement.setString(3, drowsy_start_time);
                        preparedStatement.setString(4, drowsy_end_time);
                        preparedStatement.setString(5, userID);

                        // 업데이트 실행
                        int rowsAffected = preparedStatement.executeUpdate();

                        if (rowsAffected > 0) {
                            System.out.println("졸음운전 거리가 업데이트 됬습니다.");
                        } else {
                            System.out.println("행이 업데이트되지 않았습니다.");
                            // 사용자 또는 drive_no를 찾을 수 없는 경우를 처리
                        }
                    } catch (SQLException e) {
                        System.err.println("SQL 예외: " + e.getMessage());
                        e.printStackTrace(); // 스택 트레이스 로깅
                        // SQL 예외 처리
                    } finally {
                        DatabaseConnector.closeConnection(connection);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    private class DrowsyUpdateTask implements Runnable {
        private int drive_no;

        public DrowsyUpdateTask(int drive_no) { this.drive_no = drive_no; }

        @Override
        public void run() {
            Connection connection = null;
            try {
                connection = DatabaseConnector.connect();
                if (connection != null) {
                    // 'your_table' 테이블의 'drowsy_distance' 컬럼 값을 가져오는 쿼리
                    String selectQuery = "SELECT drowsy_distance FROM drowsy_info WHERE drive_no = ?";

                    try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
                        // 'drive_no' 매개변수 설정
                        selectStatement.setInt(1, drive_no);

                        // 쿼리 실행
                        ResultSet resultSet = selectStatement.executeQuery();

                        if (resultSet.next()) {
                            // 가져온 'drowsy_distance' 컬럼 값들을 리스트에 저장
                            List<Double> columnValues = new ArrayList<>();

                            // 결과 집합에서 'drowsy_distance' 컬럼의 값을 가져옴
                            do {
                                double value = resultSet.getDouble("drowsy_distance");
                                columnValues.add(value);
                            } while (resultSet.next());

                            // 마지막 값부터 역순으로 뺄셈 및 업데이트 수행
                            for (int i = columnValues.size() - 1; i > 0; i--) {
                                double currentValue = columnValues.get(i);
                                double previousValue = columnValues.get(i - 1);
                                double updatedValue = currentValue - previousValue;
                                System.out.println(columnValues);
                                System.out.println(currentValue);
                                System.out.println(updatedValue);

                                // 해당 컬럼 업데이트 쿼리
                                String updateQuery = "UPDATE drowsy_info SET drowsy_distance = ? WHERE drive_no = ? AND drowsy_distance = ?";

                                try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                                    updateStatement.setDouble(1, updatedValue);
                                    updateStatement.setInt(2, drive_no);
                                    updateStatement.setDouble(3, columnValues.get(i));

                                    // 업데이트 쿼리 실행
                                    updateStatement.executeUpdate();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 데이터베이스와 운전 기록을 검색하는 스레드
    private class ConnectToDatabaseTask implements Runnable {
        private String userDate;

        public ConnectToDatabaseTask(String userDate) {
            this.userDate = userDate;
        }

        @Override
        public void run() {
            List<String> driveRecords = new ArrayList<>();

            try {
                Connection connection = DatabaseConnector.connect();
                if (connection != null) {
                    String query = "SELECT drive_no, start_time, end_time FROM drive_info WHERE user_id = ? AND DATE(start_time) = ?";
                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                    preparedStatement.setString(1, userID);
                    preparedStatement.setString(2, userDate);

                    ResultSet resultSet = preparedStatement.executeQuery();

                    while (resultSet.next()) {
                        int driveNo = resultSet.getInt("drive_no");
                        String startTime = new SimpleDateFormat("HH:mm:ss").format(resultSet.getTimestamp("start_time"));
                        String endTime = new SimpleDateFormat("HH:mm:ss").format(resultSet.getTimestamp("end_time"));

                        // 운전 기록을 목록에 추가
                        driveRecords.add(driveNo + "[" + startTime + "," + endTime + "]");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 결과를 메인 스레드로 전달하여 UI 업데이트
            Message message = databaseHandler.obtainMessage(1, driveRecords);
            databaseHandler.sendMessage(message);
        }
    }

    // 운전 정보를 가져오는 스레드
    private class FetchDriveInfoTask implements Runnable {
        private int driveNo;

        public FetchDriveInfoTask(int driveNo) {
            this.driveNo = driveNo;
        }

        @Override
        public void run() {
            DriveInfoData driveInfo = null;

            try {
                Connection connection = DatabaseConnector.connect();
                if (connection != null) {
                    // drowsy_info 테이블에서 drowsy_distance를 가져오는 쿼리
                    String drowsyQuery = "SELECT drowsy_distance FROM drowsy_info WHERE drive_no = ?";
                    PreparedStatement drowsyStatement = connection.prepareStatement(drowsyQuery);
                    drowsyStatement.setInt(1, driveNo);

                    ResultSet drowsyResultSet = drowsyStatement.executeQuery();

                    if (drowsyResultSet.next()) {
                        int drowsyDistance = drowsyResultSet.getInt("drowsy_distance");

                        // 이제 drive_info에서 다른 정보를 가져오기 위한 쿼리
                        String driveQuery = "SELECT forward_vision, total_distance, window_count FROM drive_info WHERE drive_no = ?";
                        PreparedStatement driveStatement = connection.prepareStatement(driveQuery);
                        driveStatement.setInt(1, driveNo);

                        ResultSet driveResultSet = driveStatement.executeQuery();

                        if (driveResultSet.next()) {
                            int forwardVision = driveResultSet.getInt("forward_vision");
                            int totalDistance = driveResultSet.getInt("total_distance");
                            int windowCount = driveResultSet.getInt("window_count");

                            // 모든 정보를 포함하는 DriveInfoData 객체 생성
                            driveInfo = new DriveInfoData(forwardVision, totalDistance, windowCount, drowsyDistance);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 결과를 메인 스레드로 전달하여 UI 업데이트
            Message message = databaseHandler.obtainMessage(2, driveInfo);
            databaseHandler.sendMessage(message);
        }
    }

    // 운전 정보를 저장하는 DriveInfoData 클래스 정의
    private static class DriveInfoData {
        private final int forwardVision; // 전방 시야 횟수
        private final int totalDistance; // 총 주행 거리
        private final int windowCount; // 창문 조작 횟수
        private final int drowsyDistance; // 졸음 운전 감지 거리

        public DriveInfoData(int forwardVision, int totalDistance, int windowCount, int drowsyDistance) {
            this.forwardVision = forwardVision;
            this.totalDistance = totalDistance;
            this.windowCount = windowCount;
            this.drowsyDistance = drowsyDistance;
        }

        public int getForwardVision() {
            return forwardVision;
        }

        public int getTotalDistance() {
            return totalDistance;
        }

        public int getWindowCount() {
            return windowCount;
        }

        public int getDrowsyDistance() {
            return drowsyDistance;
        }
    }
}
