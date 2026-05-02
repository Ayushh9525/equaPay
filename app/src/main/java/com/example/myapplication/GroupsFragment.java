package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class GroupsFragment extends Fragment {

    private LinearLayout layoutGroupsContainer;
    private TextView textViewGroupsStatus;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    public GroupsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groups, container, false);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        layoutGroupsContainer = view.findViewById(R.id.layoutGroupsContainer);
        textViewGroupsStatus = view.findViewById(R.id.textViewGroupsStatus);
        Button buttonStartGroup = view.findViewById(R.id.buttonStartGroup);

        buttonStartGroup.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), CreateGroupActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadGroups();
    }

    private void loadGroups() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null || getContext() == null) {
            textViewGroupsStatus.setText("Login required to view groups.");
            layoutGroupsContainer.removeAllViews();
            return;
        }

        textViewGroupsStatus.setText("Loading groups...");
        layoutGroupsContainer.removeAllViews();

        firestore.collection("groups")
                .whereEqualTo("createdBy", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutGroupsContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewGroupsStatus.setText("No groups added yet. Create your first group above.");
                        return;
                    }

                    textViewGroupsStatus.setText("Your groups");

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String groupName = document.getString("groupName");
                        String groupPurpose = document.getString("groupPurpose");
                        addGroupCard(
                                document.getId(),
                                groupName != null ? groupName : "Unnamed Group",
                                groupPurpose != null ? groupPurpose : "No purpose added"
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    layoutGroupsContainer.removeAllViews();
                    textViewGroupsStatus.setText("Failed to load groups: " + e.getMessage());
                });
    }

    private void addGroupCard(String groupId, String groupName, String groupPurpose) {
        if (getContext() == null) {
            return;
        }

        LinearLayout cardLayout = new LinearLayout(getContext());
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundResource(R.drawable.bg_message_card);
        cardLayout.setClickable(true);
        cardLayout.setFocusable(true);
        int padding = dpToPx(18);
        cardLayout.setPadding(padding, padding, padding, padding);
        cardLayout.setOnClickListener(v -> openGroupDetails(groupId, groupName, groupPurpose));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dpToPx(12);
        cardLayout.setLayoutParams(cardParams);

        TextView nameTextView = new TextView(getContext());
        nameTextView.setText(groupName);
        nameTextView.setTextColor(getResources().getColor(R.color.text_primary, null));
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        nameTextView.setTypeface(null, Typeface.BOLD);

        TextView purposeTextView = new TextView(getContext());
        purposeTextView.setText(groupPurpose);
        purposeTextView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        purposeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        LinearLayout.LayoutParams purposeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        purposeParams.topMargin = dpToPx(8);
        purposeTextView.setLayoutParams(purposeParams);

        TextView deleteTextView = new TextView(getContext());
        deleteTextView.setText("Delete group");
        deleteTextView.setTextColor(getResources().getColor(R.color.accent_red, null));
        deleteTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        deleteTextView.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        deleteParams.topMargin = dpToPx(14);
        deleteTextView.setLayoutParams(deleteParams);
        deleteTextView.setOnClickListener(v -> showDeleteConfirmation(groupId, groupName));

        cardLayout.addView(nameTextView);
        cardLayout.addView(purposeTextView);
        cardLayout.addView(deleteTextView);
        layoutGroupsContainer.addView(cardLayout);
    }

    private void openGroupDetails(String groupId, String groupName, String groupPurpose) {
        if (getActivity() == null) {
            return;
        }

        Intent intent = new Intent(getActivity(), GroupDetailsActivity.class);
        intent.putExtra("groupId", groupId);
        intent.putExtra("groupName", groupName);
        intent.putExtra("groupPurpose", groupPurpose);
        startActivity(intent);
    }

    private void showDeleteConfirmation(String groupId, String groupName) {
        if (getContext() == null) {
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Delete group")
                .setMessage("Are you sure you want to delete \"" + groupName + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteGroup(groupId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteGroup(String groupId) {
        firestore.collection("groups")
                .document(groupId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Group deleted", Toast.LENGTH_SHORT).show();
                    }
                    loadGroups();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to delete group: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
