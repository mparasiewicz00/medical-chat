package com.stmd.medical_chat;

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

public class UserListActivity extends AppCompatActivity {

    private static final String TAG = "UserListActivity";

    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView userRecyclerView;
    private UserAdapter userAdapter;
    private List<String> userList;
    private DatabaseReference usersRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        userRecyclerView = findViewById(R.id.userRecyclerView);

        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList, this);  // Przekazanie kontekstu
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userRecyclerView.setAdapter(userAdapter);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        loadChatHistory();

        searchButton.setOnClickListener(v -> searchUser());
    }

    private void loadChatHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userChatsRef = FirebaseDatabase.getInstance()
                    .getReference("messages")
                    .child(formatEmailForDatabase(currentUser.getEmail()));

            userChatsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    userList.clear();
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        userList.add(formatEmailFromDatabase(userSnapshot.getKey()));
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
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        userList.clear();
                        userList.add(email);
                        userAdapter.notifyDataSetChanged();
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

    private String formatEmailForDatabase(String email) {
        return email.replace(".", "_").replace("@", "at");
    }

    private String formatEmailFromDatabase(String formattedEmail) {
        return formattedEmail.replace("at", "@").replace("_", ".");
    }
}
