package com.stmd.medical_chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<String> chatList;
    private OnChatClickListener onChatClickListener;

    public ChatAdapter(List<String> chatList, OnChatClickListener onChatClickListener) {
        this.chatList = chatList;
        this.onChatClickListener = onChatClickListener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view, onChatClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        String userEmail = chatList.get(position);
        holder.chatTextView.setText(userEmail);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView chatTextView;
        OnChatClickListener onChatClickListener;

        public ChatViewHolder(View itemView, OnChatClickListener onChatClickListener) {
            super(itemView);
            chatTextView = itemView.findViewById(R.id.chatTextView);
            this.onChatClickListener = onChatClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onChatClickListener.onChatClick(getAdapterPosition());
        }
    }

    public interface OnChatClickListener {
        void onChatClick(int position);
    }
}
