package com.stmd.medical_chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText, peselEditText, pwzEditText;
    private CheckBox roleCheckBox;
    private Spinner specializationSpinner;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        peselEditText = findViewById(R.id.peselEditText);
        pwzEditText = findViewById(R.id.pwzEditText);
        roleCheckBox = findViewById(R.id.roleCheckBox);
        specializationSpinner = findViewById(R.id.specializationSpinner);
        registerButton = findViewById(R.id.registerButton);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        roleCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                pwzEditText.setVisibility(View.VISIBLE);
                specializationSpinner.setVisibility(View.VISIBLE);
            } else {
                pwzEditText.setVisibility(View.GONE);
                specializationSpinner.setVisibility(View.GONE);
            }
        });

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String pesel = peselEditText.getText().toString().trim();
        String pwz = pwzEditText.getText().toString().trim();
        String specialization = specializationSpinner.getSelectedItem().toString().trim();
        boolean isDoctor = roleCheckBox.isChecked();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(pesel)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isDoctor && (TextUtils.isEmpty(pwz) || specialization.equals("Select Specialization"))) {
            Toast.makeText(this, "Please fill in all fields for doctors", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            DatabaseReference currentUserRef = usersRef.child(formatEmailForDatabase(user.getEmail()));
                            currentUserRef.child("email").setValue(email);
                            currentUserRef.child("pesel").setValue(pesel);
                            currentUserRef.child("role").setValue(isDoctor ? "doctor" : "patient");
                            if (isDoctor) {
                                currentUserRef.child("pwz").setValue(pwz);
                                currentUserRef.child("specialization").setValue(specialization);
                            }
                            Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String formatEmailForDatabase(String email) {
        return email.replace(".", "_").replace("@", "_at_");
    }
}
