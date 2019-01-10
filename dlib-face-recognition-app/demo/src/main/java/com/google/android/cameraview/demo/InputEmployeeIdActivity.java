package com.google.android.cameraview.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class InputEmployeeIdActivity extends AppCompatActivity {

    Button btnNext;
    EditText etId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_employee_id);
        btnNext = findViewById(R.id.btn_next);
        etId = findViewById(R.id.et_id);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EmployeeData employeeData = EmployeeData.get(getApplicationContext());
                String employeeId = getEmployeeId();
                if (employeeId.length() > 0 && employeeData.hasDetails(employeeId)) {
                    Intent intent = new Intent(getApplicationContext(), CheckEmployeeActivity.class);
                    intent.putExtra("id", employeeId);
                    startActivity(intent);
                    finish();
                } else if (employeeId.length() == 0) {
                    etId.setError("Enter employee id");
                } else if (!employeeData.hasDetails(employeeId)) {
                    etId.setError("Employee id not found");
                }
            }
        });

    }

    private String getEmployeeId() {
        return etId.getText().toString().trim();
    }
}
