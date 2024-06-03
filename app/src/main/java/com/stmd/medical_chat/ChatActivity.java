package com.stmd.medical_chat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView recyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton chooseImageButton;
    private ImageButton backButton;
    private Button logoutButton;
    private FirebaseAuth mAuth;
    private DatabaseReference messagesRef;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private String recipientEmail;
    private String currentUserEmailFormatted;
    private String recipientEmailFormatted;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri filePath;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recipientEmail = getIntent().getStringExtra("recipientEmail");
        if (recipientEmail == null) {
            Toast.makeText(this, "Recipient not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeUI();
        loadMessages();
    }

    @SuppressLint("WrongViewCast")
    private void initializeUI() {
        recyclerView = findViewById(R.id.recyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        chooseImageButton = findViewById(R.id.chooseImageButton);
        backButton = findViewById(R.id.backButton);
        logoutButton = findViewById(R.id.logoutButton); // Dodaj przycisk "Logout"

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);

        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        currentUserEmailFormatted = formatEmailForDatabase(mAuth.getCurrentUser().getEmail());
        recipientEmailFormatted = formatEmailForDatabase(recipientEmail);
        messagesRef = database.getReference("messages")
                .child(currentUserEmailFormatted)
                .child(recipientEmailFormatted);
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        sendButton.setOnClickListener(v -> sendMessage());
        chooseImageButton.setOnClickListener(v -> chooseImage());
        backButton.setOnClickListener(v -> onBackPressed());
        logoutButton.setOnClickListener(v -> logout()); // Dodaj obsługę przycisku "Logout"
    }

    private void sendMessage() {
        String message = messageEditText.getText().toString();
        if (!message.isEmpty()) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Message msg = new Message(user.getEmail(), message);
                String senderPath = formatEmailForDatabase(Objects.requireNonNull(user.getEmail()));
                String recipientPath = formatEmailForDatabase(recipientEmail);

                messagesRef.push().setValue(msg).addOnSuccessListener(aVoid -> {
                    // Also save the message to the recipient's node
                    FirebaseDatabase.getInstance().getReference("messages")
                            .child(recipientPath)
                            .child(senderPath)
                            .push().setValue(msg);
                    messageEditText.setText("");
                    Toast.makeText(ChatActivity.this, "Message sent", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Error sending message", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error sending message", e);
                });
            }
        }
    }

    private void loadMessages() {
        messagesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messageList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    messageList.add(message);
                }
                messageAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading messages", databaseError.toException());
            }
        });
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            uploadImage();
        }
    }

    private void uploadImage() {
        if (filePath != null) {
            StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());
            ref.putFile(filePath)
                    .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        sendMessageWithImage(imageUrl);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(ChatActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void sendMessageWithImage(String imageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Message msg = new Message(user.getEmail(), imageUrl);
            String senderPath = formatEmailForDatabase(Objects.requireNonNull(user.getEmail()));
            String recipientPath = formatEmailForDatabase(recipientEmail);

            messagesRef.push().setValue(msg).addOnSuccessListener(aVoid -> {
                // Also save the message to the recipient's node
                FirebaseDatabase.getInstance().getReference("messages")
                        .child(recipientPath)
                        .child(senderPath)
                        .push().setValue(msg);
                Toast.makeText(ChatActivity.this, "Image sent", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(ChatActivity.this, "Error sending image", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error sending image", e);
            });
        }
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(ChatActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private String formatEmailForDatabase(String email) {
        return email.replace(".", "_").replace("@", "_at_");
    }
}
