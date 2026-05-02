package com.example.myapplication;

import android.os.Bundle;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ActivityFragment extends Fragment {

    private TextView textViewActivityStatus;
    private LinearLayout layoutActivityContainer;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    public ActivityFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        textViewActivityStatus = view.findViewById(R.id.textViewActivityStatus);
        layoutActivityContainer = view.findViewById(R.id.layoutActivityContainer);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            textViewActivityStatus.setText("Please log in again to view activity.");
            layoutActivityContainer.removeAllViews();
            return;
        }

        textViewActivityStatus.setText("Loading activity...");
        layoutActivityContainer.removeAllViews();

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("notifications")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutActivityContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewActivityStatus.setText("No recent activity. Friend requests and due reminders will appear here.");
                        return;
                    }

                    textViewActivityStatus.setText("Recent activity");

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String title = document.getString("title");
                        String message = document.getString("message");
                        addActivityCard(
                                title != null ? title : "Notification",
                                message != null ? message : "No message available"
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    layoutActivityContainer.removeAllViews();
                    textViewActivityStatus.setText("Failed to load activity: " + e.getMessage());
                });
    }

    private void addActivityCard(String title, String message) {
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

        TextView titleTextView = new TextView(getContext());
        titleTextView.setText(title);
        titleTextView.setTextColor(getResources().getColor(R.color.text_primary, null));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        titleTextView.setTypeface(null, Typeface.BOLD);

        TextView messageTextView = new TextView(getContext());
        messageTextView.setText(message);
        messageTextView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.topMargin = dpToPx(8);
        messageTextView.setLayoutParams(messageParams);

        cardLayout.addView(titleTextView);
        cardLayout.addView(messageTextView);
        layoutActivityContainer.addView(cardLayout);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
