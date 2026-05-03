package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ProfileFragment extends Fragment {

    private TextView textViewProfileName;
    private TextView textViewProfileEmail;
    private TextView textViewProfileUsername;
    private TextView textViewSummaryOwe;
    private TextView textViewSummaryReceive;
    private TextView textViewSummaryNet;
    private TextView textViewSummaryStatus;
    private FirebaseFirestore firestore;

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        firestore = FirebaseFirestore.getInstance();
        textViewProfileName = view.findViewById(R.id.textViewProfileName);
        textViewProfileEmail = view.findViewById(R.id.textViewProfileEmail);
        textViewProfileUsername = view.findViewById(R.id.textViewProfileUsername);
        textViewSummaryOwe = view.findViewById(R.id.textViewSummaryOwe);
        textViewSummaryReceive = view.findViewById(R.id.textViewSummaryReceive);
        textViewSummaryNet = view.findViewById(R.id.textViewSummaryNet);
        textViewSummaryStatus = view.findViewById(R.id.textViewSummaryStatus);
        TextView textViewEditProfile = view.findViewById(R.id.textViewEditProfile);
        TextView textViewEmailSettings = view.findViewById(R.id.textViewEmailSettings);
        TextView textViewNotificationSettings = view.findViewById(R.id.textViewNotificationSettings);
        TextView textViewSecuritySettings = view.findViewById(R.id.textViewSecuritySettings);
        TextView textViewLogout = view.findViewById(R.id.textViewLogout);
        textViewEditProfile.setOnClickListener(v -> openEditProfile());
        textViewEmailSettings.setOnClickListener(v -> openEmailSettings());
        textViewNotificationSettings.setOnClickListener(v -> openNotificationCenter());
        textViewSecuritySettings.setOnClickListener(v -> openSecuritySettings());
        textViewLogout.setOnClickListener(v -> logoutUser());

        loadProfile();
        loadBalanceSummary();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        loadBalanceSummary();
    }

    private void loadProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || getActivity() == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = documentSnapshot.getString("name");
                    String email = documentSnapshot.getString("email");
                    String username = documentSnapshot.getString("username");

                    textViewProfileName.setText(name != null ? name : "User");
                    textViewProfileEmail.setText(email != null ? email : "No email");
                    textViewProfileUsername.setText("Username: " + (username != null ? username : "not set"));
                })
                .addOnFailureListener(e -> {
                    textViewProfileUsername.setText("Username: unavailable");
                    Toast.makeText(getActivity(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadBalanceSummary() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || getActivity() == null) {
            return;
        }

        textViewSummaryStatus.setText("Loading balance summary...");

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("notifications")
                .whereEqualTo("type", "expense_due")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalOwe = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String message = document.getString("message");
                        totalOwe += extractAmountFromMessage(message);
                    }

                    calculatePayerReceivables(currentUser.getUid(), totalOwe);
                })
                .addOnFailureListener(e -> {
                    textViewSummaryStatus.setText("Failed to load due summary.");
                    Log.e("ProfileFragment", "Error loading notifications", e);
                });
    }

    private void calculatePayerReceivables(String currentUserId, double totalOwe) {
        firestore.collectionGroup("expenses")
                .whereEqualTo("paidByUserId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        updateSummaryViews(totalOwe, 0);
                        return;
                    }

                    final double[] totalReceive = {0};
                    final int totalExpenses = queryDocumentSnapshots.size();
                    final int[] processedExpenses = {0};

                    for (QueryDocumentSnapshot expenseDocument : queryDocumentSnapshots) {
                        expenseDocument.getReference()
                                .collection("splits")
                                .whereEqualTo("status", "due")
                                .get()
                                .addOnSuccessListener(splitSnapshots -> {
                                    for (QueryDocumentSnapshot splitDocument : splitSnapshots) {
                                        Object shareAmountObject = splitDocument.get("shareAmount");
                                        if (shareAmountObject instanceof Number) {
                                            totalReceive[0] += ((Number) shareAmountObject).doubleValue();
                                        }
                                    }

                                    processedExpenses[0]++;
                                    if (processedExpenses[0] == totalExpenses) {
                                        updateSummaryViews(totalOwe, totalReceive[0]);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    processedExpenses[0]++;
                                    if (processedExpenses[0] == totalExpenses) {
                                        updateSummaryViews(totalOwe, totalReceive[0]);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    textViewSummaryStatus.setText("Failed to load receive summary.");
                    Log.e("ProfileFragment", "Error loading paid expenses", e);
                });
    }

    private void updateSummaryViews(double totalOwe, double totalReceive) {
        double netBalance = totalReceive - totalOwe;

        textViewSummaryOwe.setText("You owe: Rs " + formatAmount(totalOwe));
        textViewSummaryReceive.setText("You should receive: Rs " + formatAmount(totalReceive));
        textViewSummaryNet.setText("Net balance: Rs " + formatAmount(netBalance));

        if (netBalance > 0) {
            textViewSummaryStatus.setText("You are net positive overall.");
        } else if (netBalance < 0) {
            textViewSummaryStatus.setText("You currently owe more than you should receive.");
        } else {
            textViewSummaryStatus.setText("Your balances are settled evenly.");
        }
    }

    private double extractAmountFromMessage(String message) {
        if (message == null) {
            return 0;
        }

        try {
            int rsIndex = message.indexOf("Rs ");
            if (rsIndex == -1) {
                return 0;
            }

            int startIndex = rsIndex + 3;
            int endIndex = message.indexOf(" ", startIndex);
            if (endIndex == -1) {
                endIndex = message.length();
            }

            return Double.parseDouble(message.substring(startIndex, endIndex));
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatAmount(double amount) {
        return String.format(java.util.Locale.US, "%.2f", amount);
    }

    private void openEditProfile() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        }
    }

    private void openEmailSettings() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), EmailSettingsActivity.class);
            startActivity(intent);
        }
    }

    private void openNotificationCenter() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), NotificationCenterActivity.class);
            startActivity(intent);
        }
    }

    private void openSecuritySettings() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), SecurityActivity.class);
            startActivity(intent);
        }
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        if (getActivity() != null) {
            Toast.makeText(getActivity(), "Logged out successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}
