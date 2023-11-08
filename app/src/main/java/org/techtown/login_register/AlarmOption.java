package org.techtown.login_register;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlarmOption extends AppCompatActivity {

    private Button btn_back, btn_explore, btn_check;
    private TextView tv_alarm;
    private Spinner sp_alarm;
    private String userID, alarmSelect;
    private MediaPlayer mediaPlayer;
    private static final String PREFS_NAME = "AlarmPrefs"; // SharedPreferences 이름

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_option);

        Intent intent = getIntent();
        if (intent.hasExtra("userID")) {
            userID = intent.getStringExtra("userID");

        }
        btn_back = findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AlarmOption.this, MainMenu.class);
                intent.putExtra("userID", userID);
                startActivity(intent);
                finish();
            }
        });

        btn_explore = findViewById(R.id.btn_explore);
        btn_explore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
            private void openFilePicker() {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                startActivityForResult(intent, 1);
            }
        });

        tv_alarm = findViewById(R.id.tv_alarm);
        sp_alarm = findViewById(R.id.sp_alarm);

        // SharedPreferences 또는 기본 값에서 배열을 로드
        ArrayAdapter<String> alarmAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getSavedAlarmList());
        alarmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_alarm.setAdapter(alarmAdapter);

        // 현재 선택된 알람을 SharedPreferences에서 로드
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String selectedAlarm = preferences.getString("selectedAudio", "기본 알람음 (전자 알람)");
        int position = alarmAdapter.getPosition(selectedAlarm);
        sp_alarm.setSelection(position);
        tv_alarm.setText(selectedAlarm);

        sp_alarm.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedAlarm = sp_alarm.getSelectedItem().toString();
                tv_alarm.setText(selectedAlarm);
                alarmSelect = tv_alarm.getText().toString();
                // 선택한 알람음을 SharedPreferences에 저장
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("selectedAudio", selectedAlarm);
                editor.apply();

                // Toast 메시지 표시
                Toast.makeText(getApplicationContext(), (position + 1) + "번째 알람이 선택되었습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        btn_check = findViewById(R.id.btn_check);
        btn_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer == null||!mediaPlayer.isPlaying()) {
                    if (alarmSelect != null) {
                        if (alarmSelect.equals("기본 알람음 (기상 나팔)")) {
                            mediaPlayer = MediaPlayer.create(AlarmOption.this, R.raw.basic1);
                        } else if (alarmSelect.equals("기본 알람음 (전자 알람)")) {
                            mediaPlayer = MediaPlayer.create(AlarmOption.this, R.raw.basic2);
                        } else if (alarmSelect.equals("기본 알람음 (아날로그 알람)")) {
                            mediaPlayer = MediaPlayer.create(AlarmOption.this, R.raw.basic3);
                        } else {
                            // 선택한 알람음이 사용자가 추가한 경우, 해당 파일을 재생합니다.
                            Uri selectedFileUri = Uri.fromFile(new File(getFilesDir(), alarmSelect));
                            mediaPlayer = MediaPlayer.create(AlarmOption.this, selectedFileUri);
                        }
                    } else {
                        mediaPlayer = MediaPlayer.create(AlarmOption.this, R.raw.basic1);
                    }
                    mediaPlayer.setLooping(true);
                    if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                    }
                    btn_check.setText("알람음 멈춤");
                }else{
                    mediaPlayer.pause();
                    btn_check.setText("알람음 확인");
                }

            }
        });
    }

    private List<String> getSavedAlarmList() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> alarmSet = preferences.getStringSet("alarmList", null);

        // 만약 저장된 목록이 없으면 초기값을 설정합니다.
        if (alarmSet == null) {
            alarmSet = new HashSet<>(Arrays.asList("기본 알람음 (기상 나팔)", "기본 알람음 (전자 알람)", "기본 알람음 (아날로그 알람)"));
        }

        List<String> alarmList = new ArrayList<>(alarmSet);
        return alarmList;
    }


    private void saveAlarmListInSharedPreferences(List<String> alarmList) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Set<String> alarmSet = new HashSet<>(alarmList);
        editor.putStringSet("alarmList", alarmSet);
        editor.apply();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();

            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);

                if (inputStream != null) {
                    // 원본 파일 이름 가져오기
                    String selectedFileName = getFileNameFromUri(selectedFileUri);

                    // 로컬 디렉토리에 파일을 저장
                    File localFile = new File(getFilesDir(), selectedFileName);
                    FileOutputStream outputStream = new FileOutputStream(localFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    inputStream.close();
                    outputStream.close();

                    // 기존 알람 목록 가져오기
                    List<String> alarmList = getSavedAlarmList();

                    // 알람 목록 업데이트
                    alarmList.add(selectedFileName);

                    // 업데이트된 목록을 SharedPreferences에 저장합니다.
                    saveAlarmListInSharedPreferences(alarmList);

                    ArrayAdapter<String> alarmAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, alarmList);
                    alarmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    sp_alarm.setAdapter(alarmAdapter);

                    // 새로 추가한 알람을 선택합니다.
                    sp_alarm.setSelection(alarmList.indexOf(selectedFileName));

                    SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("selectedAudio", selectedFileName);
                    editor.apply();

                    Toast.makeText(this, "파일이 선택되었고 저장되었습니다.", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Uri에서 파일 이름 가져오기
    private String getFileNameFromUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            String fileName = cursor.getString(nameIndex);
            cursor.close();
            return fileName;
        }
        return null;
    }

}