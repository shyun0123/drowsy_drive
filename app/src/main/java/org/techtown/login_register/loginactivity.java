package org.techtown.login_register;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class loginactivity extends AppCompatActivity {
    private EditText et_id, et_pass; // 사용자 아이디와 비밀번호를 입력하는 EditText 위젯
    private Button btn_login, btn_register; // 로그인과 회원가입을 위한 버튼 위젯
    private Handler databaseHandler; // 데이터베이스 작업을 위한 핸들러d

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginactivity);

        et_id = findViewById(R.id.et_id);
        et_pass = findViewById(R.id.et_pass);

        btn_login = findViewById(R.id.btn_login);
        btn_register = findViewById(R.id.btn_register);

        // 회원가입 버튼 클릭 시 회원가입 화면으로 이동
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(loginactivity.this, register_activity.class);
                startActivity(intent);
            }
        });

        // 로그인 버튼 클릭 시 로그인 정보 확인을 위한 데이터베이스 연결 스레드 시작
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userID = et_id.getText().toString();
                String userPass = et_pass.getText().toString();

                // 데이터베이스에 연결하고 로그인 정보 확인을 수행하는 새 스레드 생성
                new Thread(new ConnectToDatabaseTask(userID, userPass)).start();
            }
        });

        // 데이터베이스 작업을 위한 핸들러 초기화
        databaseHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(android.os.Message msg) {
                if (msg.what == 1) {
                    // 데이터베이스 작업이 완료되면 UI 업데이트
                    boolean loginResult = (boolean) msg.obj;

                    if (loginResult) {
                        // 로그인 성공
                        Toast.makeText(getApplicationContext(), "로그인 성공", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(loginactivity.this, MainMenu.class);
                        intent.putExtra("userID", et_id.getText().toString());
                        intent.putExtra("userPass", et_pass.getText().toString());
                        startActivity(intent);
                        finish();
                    } else {
                        // 로그인 실패
                        Toast.makeText(getApplicationContext(), "로그인 실패", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
    }

    // 데이터베이스에 연결하고 로그인 정보를 확인하는 스레드
    private class ConnectToDatabaseTask implements Runnable {
        private String userID;
        private String userPass;

        public ConnectToDatabaseTask(String userID, String userPass) {
            this.userID = userID;
            this.userPass = userPass;
        }

        @Override
        public void run() {
            boolean loginResult = verifyLogin(userID, userPass);

            // 결과를 UI 업데이트를 위한 메인 스레드로 전송
            android.os.Message message = databaseHandler.obtainMessage(1, loginResult);
            databaseHandler.sendMessage(message);
        }
    }

    // 로그인 정보 확인 메서드
    private boolean verifyLogin(String userID, String userPass) {
        try {
            Connection connection = DatabaseConnector.connect();
            if (connection != null) {
                // 데이터베이스 쿼리
                String query = "SELECT * FROM user_info WHERE user_id = ? AND user_pw = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, userID);
                preparedStatement.setString(2, userPass);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    // 로그인 성공
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // 로그인 실패
        return false;
    }
}
