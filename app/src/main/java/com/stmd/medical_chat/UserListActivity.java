package com.stmd.medical_chat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserListActivity extends AppCompatActivity {

    private static final String TAG = "UserListActivity";

    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView userRecyclerView;
    private UserAdapter userAdapter;
    private List<String> userList;
    private DatabaseReference usersRef;
    private FirebaseAuth mAuth;
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        userRecyclerView = findViewById(R.id.userRecyclerView);
        Button logoutButton = findViewById(R.id.logoutButton);

        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList, this);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userRecyclerView.setAdapter(userAdapter);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        loadUserRole(currentUser.getEmail());
        searchButton.setOnClickListener(v -> searchUser());

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(UserListActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserRole(String email) {
        String emailFormatted = formatEmailForDatabase(email);
        usersRef.child(emailFormatted).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUserRole = snapshot.getValue(String.class);
                loadChatHistory();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading user role", error.toException());
            }
        });
    }

    private void loadChatHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserEmailFormatted = formatEmailForDatabase(Objects.requireNonNull(currentUser.getEmail()));
            DatabaseReference userChatsRef = FirebaseDatabase.getInstance()
                    .getReference("messages")
                    .child(currentUserEmailFormatted);

            userChatsRef.addValueEventListener(new ValueEventListener() {
                @SuppressLint("NotifyDataSetChanged")
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    userList.clear();
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String chatPartner = formatEmailFromDatabase(Objects.requireNonNull(userSnapshot.getKey()));
                        String userRole = snapshot.child("role").getValue(String.class);
                        if (isCommunicationAllowed(userRole)) {
                            userList.add(chatPartner);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading chat history", error.toException());
                }
            });
        }
    }

    private void searchUser() {
        String email = searchEditText.getText().toString().trim();
        if (!email.isEmpty()) {
            String emailFormatted = formatEmailForDatabase(email);
            usersRef.child(emailFormatted).addListenerForSingleValueEvent(new ValueEventListener() {
                @SuppressLint("NotifyDataSetChanged")
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String userRole = snapshot.child("role").getValue(String.class);
                        if (isCommunicationAllowed(userRole)) {
                            userList.clear();
                            userList.add(email);
                            userAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(UserListActivity.this, "You cannot communicate with this user", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(UserListActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(UserListActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean isCommunicationAllowed(String userRole) {
        return userRole != null && ((currentUserRole.equals("doctor") && userRole.equals("patient")) || (currentUserRole.equals("patient") && userRole.equals("doctor")));
    }

    private String formatEmailForDatabase(String email) {
        return email.replace(".", "_").replace("@", "_at_");
    }

    private String formatEmailFromDatabase(String formattedEmail) {
        return formattedEmail.replace("_at_", "@").replace("_", ".");
    }
}
