package org.techtown.login_register;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class register_activity extends AppCompatActivity {
    private EditText et_id, et_pass, et_pass2, et_name, et_phone;
    private Button btn_register, btn_back, btn_ok;
    private TextView textView;
    private String checkID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        et_id = findViewById(R.id.et_id);
        et_pass = findViewById(R.id.et_pass);
        et_pass2 = findViewById(R.id.et_pass2);
        et_name = findViewById(R.id.et_name);
        et_phone = findViewById(R.id.et_phone);

        btn_register = findViewById(R.id.btn_register);
        btn_back = findViewById(R.id.btn_back);
        btn_ok = findViewById(R.id.btn_ok);

        textView = findViewById(R.id.textView2);
        textView.setVisibility(View.GONE);

        // 이전으로 돌아가는 버튼
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(register_activity.this, loginactivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 아이디 중복 확인 버튼
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // EditText에 입력된 아이디 가져오기
                String userID = et_id.getText().toString();
                // 백그라운드 스레드에서 데이터베이스 연결 시도
                new CheckDuplicateIDThread(userID).start();
            }
        });

        // 회원가입 버튼
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // EditText에 입력된 값 가져오기
                String userID = et_id.getText().toString();
                String userPass = et_pass.getText().toString();
                String userPass2 = et_pass2.getText().toString();
                String userName = et_name.getText().toString();
                String userPhone = et_phone.getText().toString();


                if (userID.isEmpty() || userPass.isEmpty() || userPass2.isEmpty() || userName.isEmpty() || userPhone.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "빈칸을 모두 채워주세요.", Toast.LENGTH_SHORT).show();
                    return;
                } else if (!userPass.equals(userPass2)) {
                    Toast.makeText(getApplicationContext(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                    return;
                } else if (userID.length()<5||userID.length()>12){
                    Toast.makeText(getApplicationContext(), "아이디를 5자 이상, 12자 이하로 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                } else if (userPass.length()<5||userPass.length()>12){
                    Toast.makeText(getApplicationContext(), "비밀번호를 5자 이상, 12자 이하로 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                } else if (userName.length()<2||userName.length()>15){
                    Toast.makeText(getApplicationContext(), "이름을 2자 이상, 15자 이하로 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                } else if (userPhone.length()<8||userPhone.length()>20){
                    Toast.makeText(getApplicationContext(), "전화번호를 8자 이상, 20자 이하로 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!checkID.equals(userID)){
                    Log.w("불일치",userID);
                    Toast.makeText(getApplicationContext(), "아이디의 중복체크를 다시 해주세요.", Toast.LENGTH_SHORT).show();
                    btn_register.setEnabled(false);
                    return;
                }
                // 백그라운드 스레드에서 회원가입 시도
                new RegisterUserThread(userID, userPass, userName, userPhone).start();
            }
        });
    }

    // 아이디 중복 확인 스레드
    private class CheckDuplicateIDThread extends Thread {
        private String userID;

        public CheckDuplicateIDThread(String userID) {
            this.userID = userID;
        }

        @Override
        public void run() {
            boolean isDuplicate = checkDuplicateID(userID);
            checkID = userID;
            // 결과를 메인 스레드로 전달하여 UI 업데이트
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (isDuplicate) {
                        // 중복된 아이디
                        textView.setVisibility(View.VISIBLE);
                        textView.setTextColor(ContextCompat.getColor(register_activity.this, R.color.red));
                        textView.setText("이미 사용 중인 아이디입니다.");
                        //Toast.makeText(getApplicationContext(), "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show();
                        btn_register.setEnabled(false); // 회원가입 버튼 비활성화
                    } else {
                        // 중복되지 않는 아이디
                        textView.setVisibility(View.VISIBLE);
                        textView.setTextColor(ContextCompat.getColor(register_activity.this, R.color.blue));
                        textView.setText("사용 가능한 아이디입니다.");
                        //Toast.makeText(getApplicationContext(), "사용 가능한 아이디입니다.", Toast.LENGTH_SHORT).show();
                        btn_register.setEnabled(true); // 회원가입 버튼 활성화
                    }
                }
            });
        }
    }

    // 회원가입 스레드
    private class RegisterUserThread extends Thread {
        private String userID;
        private String userPass;
        private String userName;
        private String userPhone;

        public RegisterUserThread(String userID, String userPass, String userName, String userPhone) {
            this.userID = userID;
            this.userPass = userPass;
            this.userName = userName;
            this.userPhone = userPhone;
        }

        @Override
        public void run() {
            boolean isRegistered = registerUser(userID, userPass, userName, userPhone);

            // 결과를 메인 스레드로 전달하여 UI 업데이트
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (isRegistered) {
                        // 회원가입 성공
                        Toast.makeText(getApplicationContext(), "회원가입 성공", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(register_activity.this, loginactivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // 회원가입 실패
                        Toast.makeText(getApplicationContext(), "회원가입 실패", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // 아이디 중복 확인 메서드
    private boolean checkDuplicateID(String userID) {
        try {
            Connection connection = DatabaseConnector.connect();
            if (connection != null) {
                String query = "SELECT * FROM user_info WHERE user_id = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, userID);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    // 중복된 아이디
                    return true;
                } else {
                    // 중복되지 않는 아이디
                    return false;
                }
            } else {
                // 데이터베이스 연결 실패
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 회원가입 메서드
    private boolean registerUser(String userID, String userPass, String userName, String userPhone) {
        try {
            Connection connection = DatabaseConnector.connect();
            if (connection != null) {
                String query = "INSERT INTO user_info (user_id, user_pw, user_name, user_pn) VALUES (?, ?, ?, ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, userID);
                preparedStatement.setString(2, userPass);
                preparedStatement.setString(3, userName);
                preparedStatement.setString(4, userPhone);
                int result = preparedStatement.executeUpdate();

                if (result > 0) {
                    // 회원가입 성공
                    return true;
                } else {
                    // 회원가입 실패
                    return false;
                }
            } else {
                // 데이터베이스 연결 실패
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
