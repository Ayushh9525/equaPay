package com.example.myapplication;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class FriendsFragment extends Fragment {

    private EditText editTextFriendSearch;
    private TextView textViewFriendStatus;
    private LinearLayout layoutFriendSearchResult;
    private LinearLayout layoutFriendsContainer;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    public FriendsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        editTextFriendSearch = view.findViewById(R.id.editTextFriendSearch);
        textViewFriendStatus = view.findViewById(R.id.textViewFriendStatus);
        layoutFriendSearchResult = view.findViewById(R.id.layoutFriendSearchResult);
        layoutFriendsContainer = view.findViewById(R.id.layoutFriendsContainer);
        Button buttonSearchFriend = view.findViewById(R.id.buttonSearchFriend);

        buttonSearchFriend.setOnClickListener(v -> searchFriend());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFriends();
    }

    private void searchFriend() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String query = editTextFriendSearch.getText().toString().trim();

        layoutFriendSearchResult.removeAllViews();

        if (currentUser == null) {
            textViewFriendStatus.setText("Please log in again to search friends.");
            return;
        }

        if (TextUtils.isEmpty(query)) {
            editTextFriendSearch.setError("Enter username, email, name, or user id");
            editTextFriendSearch.requestFocus();
            return;
        }

        textViewFriendStatus.setText("Searching...");

        if (query.contains("@")) {
            firestore.collection("users")
                    .whereEqualTo("email", query)
                    .get()
                    .addOnSuccessListener(this::handleSearchResults)
                    .addOnFailureListener(e -> textViewFriendStatus.setText("Search failed: " + e.getMessage()));
        } else {
            searchFriendByUsername(query.toLowerCase());
        }
    }

    private void searchFriendByUsername(String query) {
        firestore.collection("users")
                .whereEqualTo("username", query)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        handleSearchResults(queryDocumentSnapshots);
                    } else {
                        searchFriendById(query);
                    }
                })
                .addOnFailureListener(e -> textViewFriendStatus.setText("Search failed: " + e.getMessage()));
    }

    private void searchFriendById(String query) {
        firestore.collection("users")
                .document(query)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        layoutFriendSearchResult.removeAllViews();
                        textViewFriendStatus.setText("Search result");
                        showSearchResultCard(documentSnapshot);
                    } else {
                        searchFriendByName(query);
                    }
                })
                .addOnFailureListener(e -> textViewFriendStatus.setText("Search failed: " + e.getMessage()));
    }

    private void searchFriendByName(String query) {
        firestore.collection("users")
                .whereEqualTo("name", query)
                .get()
                .addOnSuccessListener(this::handleSearchResults)
                .addOnFailureListener(e -> textViewFriendStatus.setText("Search failed: " + e.getMessage()));
    }

    private void handleSearchResults(QuerySnapshot queryDocumentSnapshots) {
        layoutFriendSearchResult.removeAllViews();

        if (queryDocumentSnapshots.isEmpty()) {
            textViewFriendStatus.setText("No user found with that username, email, name, or id.");
            return;
        }

        textViewFriendStatus.setText("Search result");

        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            showSearchResultCard(document);
        }
    }

    private void showSearchResultCard(DocumentSnapshot document) {
        if (getContext() == null) {
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String friendId = document.getId();
        String friendName = document.getString("name") != null ? document.getString("name") : "Unknown";
        String friendEmail = document.getString("email") != null ? document.getString("email") : "No email";
        String friendUsername = document.getString("username") != null ? document.getString("username") : "No username";

        if (currentUser != null && friendId.equals(currentUser.getUid())) {
            textViewFriendStatus.setText("You cannot add yourself as a friend.");
            return;
        }

        LinearLayout cardLayout = new LinearLayout(getContext());
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundResource(R.drawable.bg_message_card);
        int padding = dpToPx(18);
        cardLayout.setPadding(padding, padding, padding, padding);

        TextView nameTextView = new TextView(getContext());
        nameTextView.setText(friendName);
        nameTextView.setTextColor(getResources().getColor(R.color.text_primary, null));
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        nameTextView.setTypeface(null, Typeface.BOLD);

        TextView emailTextView = new TextView(getContext());
        emailTextView.setText(friendEmail);
        emailTextView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        LinearLayout.LayoutParams emailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        emailParams.topMargin = dpToPx(8);
        emailTextView.setLayoutParams(emailParams);

        TextView usernameTextView = new TextView(getContext());
        usernameTextView.setText("Username: " + friendUsername);
        usernameTextView.setTextColor(getResources().getColor(R.color.accent_green, null));
        usernameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams usernameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        usernameParams.topMargin = dpToPx(6);
        usernameTextView.setLayoutParams(usernameParams);

        TextView userIdTextView = new TextView(getContext());
        userIdTextView.setText("User ID: " + friendId);
        userIdTextView.setTextColor(getResources().getColor(R.color.text_muted, null));
        userIdTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams idParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        idParams.topMargin = dpToPx(6);
        userIdTextView.setLayoutParams(idParams);

        Button addFriendButton = new Button(getContext());
        addFriendButton.setText("Add Friend");
        addFriendButton.setBackgroundTintList(getResources().getColorStateList(R.color.accent_green, null));
        addFriendButton.setTextColor(getResources().getColor(R.color.button_text_dark, null));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = dpToPx(14);
        addFriendButton.setLayoutParams(buttonParams);
        addFriendButton.setOnClickListener(v -> addFriend(friendId, friendName, friendEmail, addFriendButton));

        cardLayout.addView(nameTextView);
        cardLayout.addView(emailTextView);
        cardLayout.addView(usernameTextView);
        cardLayout.addView(userIdTextView);
        cardLayout.addView(addFriendButton);
        layoutFriendSearchResult.addView(cardLayout);
    }

    private void addFriend(String friendId, String friendName, String friendEmail, Button addFriendButton) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            textViewFriendStatus.setText("Please log in again to add friends.");
            return;
        }

        addFriendButton.setEnabled(false);
        addFriendButton.setText("Adding...");

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("friends")
                .document(friendId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        addFriendButton.setEnabled(true);
                        addFriendButton.setText("Add Friend");
                        textViewFriendStatus.setText("This user is already in your friends list.");
                        return;
                    }

                    saveFriendForBothUsers(currentUser, friendId, friendName, friendEmail, addFriendButton);
                })
                .addOnFailureListener(e -> {
                    addFriendButton.setEnabled(true);
                    addFriendButton.setText("Add Friend");
                    textViewFriendStatus.setText("Failed to add friend: " + e.getMessage());
                });
    }

    private void saveFriendForBothUsers(FirebaseUser currentUser, String friendId, String friendName,
                                        String friendEmail, Button addFriendButton) {
        String currentName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User";
        String currentEmail = currentUser.getEmail() != null ? currentUser.getEmail() : "No email";

        Map<String, Object> currentToFriendMap = new HashMap<>();
        currentToFriendMap.put("friendId", friendId);
        currentToFriendMap.put("friendName", friendName);
        currentToFriendMap.put("friendEmail", friendEmail);
        currentToFriendMap.put("addedAt", System.currentTimeMillis());

        Map<String, Object> friendToCurrentMap = new HashMap<>();
        friendToCurrentMap.put("friendId", currentUser.getUid());
        friendToCurrentMap.put("friendName", currentName);
        friendToCurrentMap.put("friendEmail", currentEmail);
        friendToCurrentMap.put("addedAt", System.currentTimeMillis());

        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("title", "New friend added");
        notificationMap.put("message", currentEmail + " added you as a friend.");
        notificationMap.put("type", "friend_added");
        notificationMap.put("createdAt", System.currentTimeMillis());
        notificationMap.put("fromUserId", currentUser.getUid());

        WriteBatch batch = firestore.batch();
        batch.set(
                firestore.collection("users").document(currentUser.getUid())
                        .collection("friends").document(friendId),
                currentToFriendMap
        );
        batch.set(
                firestore.collection("users").document(friendId)
                        .collection("friends").document(currentUser.getUid()),
                friendToCurrentMap
        );
        batch.set(
                firestore.collection("users").document(friendId)
                        .collection("notifications").document(),
                notificationMap
        );

        batch.commit()
                .addOnSuccessListener(unused -> {
                    addFriendButton.setEnabled(true);
                    addFriendButton.setText("Added");
                    textViewFriendStatus.setText("Friend added successfully.");
                    Toast.makeText(getContext(), "Friend added successfully", Toast.LENGTH_SHORT).show();
                    loadFriends();
                })
                .addOnFailureListener(e -> {
                    addFriendButton.setEnabled(true);
                    addFriendButton.setText("Add Friend");
                    textViewFriendStatus.setText("Failed to add friend: " + e.getMessage());
                });
    }

    private void loadFriends() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            textViewFriendStatus.setText("Please log in again to view friends.");
            layoutFriendsContainer.removeAllViews();
            return;
        }

        layoutFriendsContainer.removeAllViews();

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("friends")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutFriendsContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        addEmptyFriendsText();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String friendName = document.getString("friendName");
                        String friendEmail = document.getString("friendEmail");
                        String friendId = document.getString("friendId");
                        addFriendCard(
                                friendId != null ? friendId : document.getId(),
                                friendName != null ? friendName : "Unknown friend",
                                friendEmail != null ? friendEmail : "No email"
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    layoutFriendsContainer.removeAllViews();
                    textViewFriendStatus.setText("Failed to load friends: " + e.getMessage());
                });
    }

    private void addEmptyFriendsText() {
        if (getContext() == null) {
            return;
        }

        TextView emptyTextView = new TextView(getContext());
        emptyTextView.setText("No friends added yet.");
        emptyTextView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        emptyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        layoutFriendsContainer.addView(emptyTextView);
    }

    private void addFriendCard(String friendId, String friendName, String friendEmail) {
        if (getContext() == null) {
            return;
        }

        LinearLayout cardLayout = new LinearLayout(getContext());
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundResource(R.drawable.bg_message_card);
        int padding = dpToPx(18);
        cardLayout.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dpToPx(12);
        cardLayout.setLayoutParams(cardParams);

        TextView nameTextView = new TextView(getContext());
        nameTextView.setText(friendName);
        nameTextView.setTextColor(getResources().getColor(R.color.text_primary, null));
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        nameTextView.setTypeface(null, Typeface.BOLD);

        TextView emailTextView = new TextView(getContext());
        emailTextView.setText(friendEmail);
        emailTextView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        LinearLayout.LayoutParams emailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        emailParams.topMargin = dpToPx(8);
        emailTextView.setLayoutParams(emailParams);

        Button removeFriendButton = new Button(getContext());
        removeFriendButton.setText("Remove Friend");
        removeFriendButton.setBackgroundTintList(getResources().getColorStateList(R.color.accent_red, null));
        removeFriendButton.setTextColor(getResources().getColor(R.color.white, null));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = dpToPx(12);
        removeFriendButton.setLayoutParams(buttonParams);
        removeFriendButton.setOnClickListener(v -> removeFriend(friendId, removeFriendButton));

        cardLayout.addView(nameTextView);
        cardLayout.addView(emailTextView);
        cardLayout.addView(removeFriendButton);
        layoutFriendsContainer.addView(cardLayout);
    }

    private void removeFriend(String friendId, Button removeFriendButton) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            textViewFriendStatus.setText("Please log in again to remove friends.");
            return;
        }

        removeFriendButton.setEnabled(false);
        removeFriendButton.setText("Removing...");

        WriteBatch batch = firestore.batch();
        batch.delete(
                firestore.collection("users")
                        .document(currentUser.getUid())
                        .collection("friends")
                        .document(friendId)
        );
        batch.delete(
                firestore.collection("users")
                        .document(friendId)
                        .collection("friends")
                        .document(currentUser.getUid())
        );

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Friend removed", Toast.LENGTH_SHORT).show();
                    loadFriends();
                })
                .addOnFailureListener(e -> {
                    removeFriendButton.setEnabled(true);
                    removeFriendButton.setText("Remove Friend");
                    textViewFriendStatus.setText("Failed to remove friend: " + e.getMessage());
                });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
