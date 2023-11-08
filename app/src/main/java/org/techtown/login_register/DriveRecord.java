package org.techtown.login_register;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.text.DecimalFormat;


public class DriveRecord extends AppCompatActivity {
    private TextView tv_score;
    private Button btn_back, btn_forward, btn_distance;
    private ImageButton ibtn_date;
    private EditText et_date;
    private Spinner sp_no;
    private String userID;
    private double distanceSum=0;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive_record);

        // 화면의 UI 요소 초기화
        tv_score = findViewById(R.id.tv_score);
        btn_back = findViewById(R.id.btn_back);
        btn_distance = findViewById(R.id.btn_drowsy);
        btn_forward = findViewById(R.id.btn_forward);
        ibtn_date = findViewById(R.id.ibtn_date);
        et_date = findViewById(R.id.et_date);
        sp_no = findViewById(R.id.sp_no);

        // Intent로부터 userID를 받아옴
        Intent intent = getIntent();
        if (intent.hasExtra("userID")) {
            userID = intent.getStringExtra("userID");
            // TextView에 userID 설정
        }

        et_date = findViewById(R.id.et_date);
        et_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        // "뒤로 가기" 버튼 클릭 이벤트 처리
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DriveRecord.this, MainMenu.class);
                intent.putExtra("userID", userID);
                startActivity(intent);
                finish();
            }
        });

        // "날짜 조회" 버튼 클릭 이벤트 처리
        ibtn_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userDate = et_date.getText().toString();
                new ConnectToDatabaseThread(userDate).start();
            }
        });

        // 스피너 선택 이벤트 처리
        sp_no.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedItem = sp_no.getSelectedItem().toString();

                // 선택된 항목에서 운전 번호 추출
                int driveNo = Integer.parseInt(selectedItem.split("\\[")[0]);

                // 선택된 운전 레코드의 전방 시야 정보와 총 거리를 가져오는 백그라운드 작업 시작
                new FetchDriveInfoThread(driveNo).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 아무것도 선택되지 않았을 때 아무 동작도 하지 않음
            }
        });
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
                        et_date.setText(dateFormat.format(calendar.getTime()));
                    }
                }, year, month, day);

        // DatePickerDialog를 표시
        datePickerDialog.show();
    }

    // 데이터베이스 연결 및 운전 레코드 조회 쓰레드
    private class ConnectToDatabaseThread extends Thread {
        private String userDate;

        public ConnectToDatabaseThread(String userDate) {
            this.userDate = userDate;
        }

        @Override
        public void run() {
            List<String> driveRecords = new ArrayList<>();
            int sumDroDis = 0;
            try {
                Connection connection = DatabaseConnector.connect();
                if (connection != null) {
                    // 데이터베이스 쿼리
                    String query = "SELECT drive_no, start_time, end_time FROM drive_info WHERE user_id = ? AND DATE(start_time) = ?";
                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                    preparedStatement.setString(1, userID);
                    preparedStatement.setString(2, userDate);

                    ResultSet resultSet = preparedStatement.executeQuery();

                    while (resultSet.next()) {
                        int driveNo = resultSet.getInt("drive_no");
                        String startTime = new SimpleDateFormat("HH:mm:ss").format(resultSet.getTimestamp("start_time"));
                        String endTime = new SimpleDateFormat("HH:mm:ss").format(resultSet.getTimestamp("end_time"));
                        Log.w("서버",""+driveNo);

                        // 운전 레코드를 리스트에 추가
                        driveRecords.add(driveNo + "[" + startTime + "," + endTime + "]");
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 결과를 메인 스레드로 전달
            Message message = handler.obtainMessage(1, driveRecords);
            message.sendToTarget();

            message = handler.obtainMessage(3, distanceSum);
            message.sendToTarget();
        }
    }

    // 운전 정보를 가져오는 쓰레드
    private class FetchDriveInfoThread extends Thread {
        private int driveNo;

        public FetchDriveInfoThread(int driveNo) {
            this.driveNo = driveNo;
        }

        @Override
        public void run() {
            DriveRecordInfo driveInfo = null;

            try {
                Connection connection = DatabaseConnector.connect();
                double sumDroDis =0;
                if (connection != null) {
                    // "drowsy_info" 테이블에서 "drowsy_distance"를 가져오는 쿼리
                    String query2 = "SELECT drowsy_distance FROM drowsy_info WHERE drive_no = ?";
                    PreparedStatement preparedStatement2 = connection.prepareStatement(query2);
                    preparedStatement2.setInt(1, driveNo);

                    ResultSet drowsyResultSet = preparedStatement2.executeQuery();

                    while (drowsyResultSet.next()) {
                        sumDroDis += drowsyResultSet.getDouble("drowsy_distance");
                    }

                    distanceSum = sumDroDis;

                    Log.w("서버 반응 driveNo FetchDrive", driveNo + "");



                    // 이제 "drive_info"에서 기타 정보를 가져오기 위한 쿼리
                    String driveQuery = "SELECT forward_vision FROM drive_info WHERE drive_no = ?";
                    PreparedStatement driveStatement = connection.prepareStatement(driveQuery);
                    driveStatement.setInt(1, driveNo);
                    Log.w("서버 반응 driveNo FetchDrive 2", driveNo + "");
                    ResultSet driveResultSet = driveStatement.executeQuery();

                    if (driveResultSet.next()) {
                        int forwardVision = driveResultSet.getInt("forward_vision");

                        // 모든 정보를 포함하는 DriveRecordInfo 객체 생성
                        driveInfo = new DriveRecordInfo(forwardVision, distanceSum);
                        Log.w("서버 반응 forward FetchDrive", forwardVision + "");

                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 결과를 메인 스레드로 전달
            Message message = handler.obtainMessage(2, driveInfo);
            message.sendToTarget();
        }
    }

    // 핸들러를 사용하여 메인 스레드와 통신
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                // 데이터베이스 결과를 처리하고 스피너 업데이트
                List<String> driveRecords = (List<String>) msg.obj;
                handleDatabaseResult(driveRecords);
            } else if (msg.what == 2) {
                // 운전 정보를 처리하고 UI 업데이트
                DriveRecordInfo driveInfo = (DriveRecordInfo) msg.obj;
                handleDriveInfoResult(driveInfo);
            }
        }
    };

    // 데이터베이스 결과를 처리하고 스피너 업데이트
    private void handleDatabaseResult(List<String> driveRecords) {
        if (!driveRecords.isEmpty()) {
            // 스피너에 운전 레코드를 표시
            ArrayAdapter<String> adapter = new ArrayAdapter<>(DriveRecord.this, android.R.layout.simple_spinner_dropdown_item, driveRecords);
            sp_no.setAdapter(adapter);
        } else {
            // 데이터베이스에서 해당 사용자와 날짜에 맞는 레코드가 없을 경우 메시지 표시
            Toast.makeText(getApplicationContext(), "운전 기록이 없습니다.", Toast.LENGTH_SHORT).show();
            sp_no.setAdapter(null); // 스피너를 비움
        }
    }

    // 운전 정보를 처리하고 UI 업데이트
    private void handleDriveInfoResult(DriveRecordInfo driveInfo) {
        if (driveInfo != null) {
            // btn_forward와 btn_distance 업데이트
            String forwardVisionText = driveInfo.getForwardVision() + "회";
            DecimalFormat df = new DecimalFormat("0.00");
            String drowsyDistanceText = df.format(driveInfo.getTotalDistance()) + "km";

            btn_forward.setText(forwardVisionText);

            btn_distance.setText(drowsyDistanceText);

            // 점수 계산
            int forwardVision = driveInfo.getForwardVision();
            double drowsyDistance = driveInfo.getTotalDistance();
            int score = 100 - 2 * forwardVision - (int)(50 * drowsyDistance);

            // tv_score 업데이트
            tv_score.setText(score +"");
        }
    }

    // 운전 정보를 저장하는 클래스 이름 변경
    private static class DriveRecordInfo {
        private final int forwardVision;
        private final double drowsyDistance;

        public DriveRecordInfo(int forwardVision, double totalDistance) {
            this.forwardVision = forwardVision;
            this.drowsyDistance = totalDistance;
        }

        public int getForwardVision() {
            return forwardVision;
        }

        public double getTotalDistance() {
            return drowsyDistance;
        }
    }
}

