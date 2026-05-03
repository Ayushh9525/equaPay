package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class GroupDetailsActivity extends AppCompatActivity {

    private String groupId;
    private String groupName;
    private String groupPurpose;
    private TextView textViewExpensesStatus;
    private TextView textViewBalancesStatus;
    private TextView textViewMembersStatus;
    private TextView textViewBalanceSummary;
    private LinearLayout layoutExpensesContainer;
    private LinearLayout layoutBalancesContainer;
    private LinearLayout layoutMembersContainer;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        firestore = FirebaseFirestore.getInstance();
        ImageView imageViewBack = findViewById(R.id.imageViewBackGroupDetails);
        TextView textViewGroupTitle = findViewById(R.id.textViewGroupTitle);
        TextView textViewGroupPurpose = findViewById(R.id.textViewGroupPurpose);
        TextView textViewGroupId = findViewById(R.id.textViewGroupId);
        Button buttonAddExpense = findViewById(R.id.buttonAddExpenseFromGroup);
        Button buttonAddMembersToGroup = findViewById(R.id.buttonAddMembersToGroup);
        textViewExpensesStatus = findViewById(R.id.textViewExpensesStatus);
        textViewBalancesStatus = findViewById(R.id.textViewBalancesStatus);
        textViewMembersStatus = findViewById(R.id.textViewMembersStatus);
        textViewBalanceSummary = findViewById(R.id.textViewBalanceSummary);
        layoutExpensesContainer = findViewById(R.id.layoutExpensesContainer);
        layoutBalancesContainer = findViewById(R.id.layoutBalancesContainer);
        layoutMembersContainer = findViewById(R.id.layoutMembersContainer);

        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");
        groupPurpose = getIntent().getStringExtra("groupPurpose");

        textViewGroupTitle.setText(groupName != null ? groupName : "Group Details");
        textViewGroupPurpose.setText(groupPurpose != null ? groupPurpose : "No purpose added");
        textViewGroupId.setText(groupId != null ? "Group ID: " + groupId : "Group ID unavailable");

        imageViewBack.setOnClickListener(v -> finish());

        buttonAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddExpenseActivity.class);
            intent.putExtra("groupId", groupId);
            intent.putExtra("groupName", groupName);
            startActivity(intent);
        });

        buttonAddMembersToGroup.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddGroupMembersActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMembers();
        loadExpenses();
    }

    private void loadMembers() {
        textViewMembersStatus.setText("Loading members...");
        layoutMembersContainer.removeAllViews();

        firestore.collection("groups")
                .document(groupId)
                .collection("members")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutMembersContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewMembersStatus.setText("No members added yet.");
                        return;
                    }

                    textViewMembersStatus.setText("Current group members");
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString("name");
                        String email = document.getString("email");
                        addMemberCard(
                                name != null ? name : "Member",
                                email != null ? email : "No email"
                        );
                    }
                })
                .addOnFailureListener(e -> textViewMembersStatus.setText("Failed to load members: " + e.getMessage()));
    }

    private void loadExpenses() {
        if (groupId == null || groupId.isEmpty()) {
            textViewExpensesStatus.setText("Group not found.");
            layoutExpensesContainer.removeAllViews();
            return;
        }

        textViewExpensesStatus.setText("Loading expenses...");
        textViewBalancesStatus.setText("Loading balances...");
        layoutExpensesContainer.removeAllViews();
        layoutBalancesContainer.removeAllViews();

        firestore.collection("groups")
                .document(groupId)
                .collection("expenses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutExpensesContainer.removeAllViews();
                    layoutBalancesContainer.removeAllViews();

                    textViewBalanceSummary.setText("Balance summary will appear here.");

                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewExpensesStatus.setText("No expenses added yet. Use the button below to start tracking shared costs.");
                        textViewBalancesStatus.setText("No balances available yet.");
                        textViewBalanceSummary.setText("No expenses, no balances.");
                        return;
                    }

                    textViewExpensesStatus.setText("Saved expenses");
                    final int totalExpenses = queryDocumentSnapshots.size();
                    final int[] processedExpenses = {0};
                    final boolean[] hasDueEntries = {false};
                    final double[] totalDueAmount = {0};
                    final int[] totalDueEntries = {0};

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String expenseId = document.getId();
                        String expenseTitle = document.getString("expenseTitle");
                        Object amountObject = document.get("amount");
                        String paidBy = document.getString("paidBy");
                        Object splitAmountObject = document.get("splitAmount");
                        Object participantCountObject = document.get("participantCount");

                        String amountText = amountObject != null ? String.valueOf(amountObject) : "0";
                        String splitAmountText = splitAmountObject != null ? String.valueOf(splitAmountObject) : "0";
                        String participantCountText = participantCountObject != null ? String.valueOf(participantCountObject) : "0";
                        addExpenseCard(
                                expenseId,
                                expenseTitle != null ? expenseTitle : "Untitled expense",
                                amountText,
                                paidBy != null ? paidBy : "Unknown payer",
                                splitAmountText,
                                participantCountText
                        );

                        loadBalancesForExpense(
                                expenseId,
                                expenseTitle != null ? expenseTitle : "Untitled expense",
                                paidBy != null ? paidBy : "Unknown payer",
                                processedExpenses,
                                totalExpenses,
                                hasDueEntries,
                                totalDueAmount,
                                totalDueEntries
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    layoutExpensesContainer.removeAllViews();
                    layoutBalancesContainer.removeAllViews();
                    textViewExpensesStatus.setText("Failed to load expenses: " + e.getMessage());
                    textViewBalancesStatus.setText("Failed to load balances: " + e.getMessage());
                });
    }

    private void addExpenseCard(String expenseId, String expenseTitle, String amount, String paidBy, String splitAmount, String participantCount) {
        LinearLayout cardLayout = new LinearLayout(this);
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

        TextView titleTextView = new TextView(this);
        titleTextView.setText(expenseTitle);
        titleTextView.setTextColor(getResources().getColor(R.color.text_primary, null));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleTextView.setTypeface(null, Typeface.BOLD);

        TextView amountTextView = new TextView(this);
        amountTextView.setText("Amount: Rs " + amount);
        amountTextView.setTextColor(getResources().getColor(R.color.accent_green, null));
        amountTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        amountParams.topMargin = dpToPx(8);
        amountTextView.setLayoutParams(amountParams);

        TextView paidByTextView = new TextView(this);
        paidByTextView.setText("Paid by: " + paidBy);
        paidByTextView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        paidByTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        LinearLayout.LayoutParams paidByParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paidByParams.topMargin = dpToPx(6);
        paidByTextView.setLayoutParams(paidByParams);

        TextView splitTextView = new TextView(this);
        splitTextView.setText("Each share: Rs " + splitAmount + " across " + participantCount + " people");
        splitTextView.setTextColor(getResources().getColor(R.color.text_muted, null));
        splitTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        LinearLayout.LayoutParams splitParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        splitParams.topMargin = dpToPx(6);
        splitTextView.setLayoutParams(splitParams);

        Button deleteExpenseButton = new Button(this);
        deleteExpenseButton.setText("Delete Expense");
        deleteExpenseButton.setTextColor(getResources().getColor(R.color.white, null));
        deleteExpenseButton.setBackgroundTintList(getResources().getColorStateList(R.color.accent_red, null));

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        deleteParams.topMargin = dpToPx(12);
        deleteExpenseButton.setLayoutParams(deleteParams);
        deleteExpenseButton.setOnClickListener(v -> deleteExpense(expenseId));

        cardLayout.addView(titleTextView);
        cardLayout.addView(amountTextView);
        cardLayout.addView(paidByTextView);
        cardLayout.addView(splitTextView);
        cardLayout.addView(deleteExpenseButton);
        layoutExpensesContainer.addView(cardLayout);
    }

    private void loadBalancesForExpense(String expenseId, String expenseTitle, String paidBy,
                                        int[] processedExpenses, int totalExpenses, boolean[] hasDueEntries,
                                        double[] totalDueAmount, int[] totalDueEntries) {
        firestore.collection("groups")
                .document(groupId)
                .collection("expenses")
                .document(expenseId)
                .collection("splits")
                .whereEqualTo("status", "due")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        hasDueEntries[0] = true;
                        for (QueryDocumentSnapshot splitDocument : queryDocumentSnapshots) {
                            String participantUserId = splitDocument.getString("participantUserId");
                            String participantLabel = splitDocument.getString("participantLabel");
                            Object shareAmountObject = splitDocument.get("shareAmount");
                            String shareAmount = shareAmountObject != null ? String.valueOf(shareAmountObject) : "0";
                            double shareAmountValue = shareAmountObject instanceof Number
                                    ? ((Number) shareAmountObject).doubleValue()
                                    : 0;
                            totalDueAmount[0] += shareAmountValue;
                            totalDueEntries[0]++;

                            addBalanceCard(
                                    expenseId,
                                    participantUserId != null ? participantUserId : splitDocument.getId(),
                                    participantLabel != null ? participantLabel : "A member",
                                    paidBy,
                                    shareAmount,
                                    expenseTitle
                            );
                        }
                    }

                    processedExpenses[0]++;
                    updateBalanceStatus(processedExpenses[0], totalExpenses, hasDueEntries[0], totalDueAmount[0], totalDueEntries[0]);
                })
                .addOnFailureListener(e -> {
                    processedExpenses[0]++;
                    textViewBalancesStatus.setText("Failed to load balances: " + e.getMessage());
                });
    }

    private void updateBalanceStatus(int processedExpenses, int totalExpenses, boolean hasDueEntries,
                                     double totalDueAmount, int totalDueEntries) {
        if (processedExpenses < totalExpenses) {
            return;
        }

        if (hasDueEntries) {
            textViewBalancesStatus.setText("Who owes whom");
            textViewBalanceSummary.setText("Pending dues: " + totalDueEntries + " | Total outstanding: Rs " + formatAmount(totalDueAmount));
        } else {
            textViewBalancesStatus.setText("No pending balances. Everyone included as payer or no dues yet.");
            textViewBalanceSummary.setText("No pending balances in this group.");
        }
    }

    private void addBalanceCard(String expenseId, String participantUserId, String participantLabel,
                                String paidBy, String shareAmount, String expenseTitle) {
        LinearLayout cardLayout = new LinearLayout(this);
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

        TextView dueTextView = new TextView(this);
        dueTextView.setText(participantLabel + " owes " + paidBy);
        dueTextView.setTextColor(getResources().getColor(R.color.text_primary, null));
        dueTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        dueTextView.setTypeface(null, Typeface.BOLD);

        TextView amountTextView = new TextView(this);
        amountTextView.setText("Amount due: Rs " + shareAmount);
        amountTextView.setTextColor(getResources().getColor(R.color.accent_red, null));
        amountTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        amountParams.topMargin = dpToPx(8);
        amountTextView.setLayoutParams(amountParams);

        TextView sourceTextView = new TextView(this);
        sourceTextView.setText("For expense: " + expenseTitle);
        sourceTextView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        sourceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        sourceParams.topMargin = dpToPx(6);
        sourceTextView.setLayoutParams(sourceParams);

        Button buttonMarkPaid = new Button(this);
        buttonMarkPaid.setText("Mark as paid");
        buttonMarkPaid.setTextColor(getResources().getColor(R.color.button_text_dark, null));
        buttonMarkPaid.setBackgroundTintList(getResources().getColorStateList(R.color.accent_green, null));

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = dpToPx(12);
        buttonMarkPaid.setLayoutParams(buttonParams);
        buttonMarkPaid.setOnClickListener(v ->
                markSplitAsPaid(expenseId, participantUserId, participantLabel, paidBy, shareAmount, expenseTitle)
        );

        cardLayout.addView(dueTextView);
        cardLayout.addView(amountTextView);
        cardLayout.addView(sourceTextView);
        cardLayout.addView(buttonMarkPaid);
        layoutBalancesContainer.addView(cardLayout);
    }

    private void addMemberCard(String name, String email) {
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundResource(R.drawable.bg_message_card);
        int padding = dpToPx(16);
        cardLayout.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(10);
        cardLayout.setLayoutParams(params);

        TextView nameTextView = new TextView(this);
        nameTextView.setText(name);
        nameTextView.setTextColor(getResources().getColor(R.color.text_primary, null));
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        nameTextView.setTypeface(null, Typeface.BOLD);

        TextView emailTextView = new TextView(this);
        emailTextView.setText(email);
        emailTextView.setTextColor(getResources().getColor(R.color.text_secondary, null));
        emailTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams emailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        emailParams.topMargin = dpToPx(6);
        emailTextView.setLayoutParams(emailParams);

        cardLayout.addView(nameTextView);
        cardLayout.addView(emailTextView);
        layoutMembersContainer.addView(cardLayout);
    }

    private String formatAmount(double amount) {
        return String.format(java.util.Locale.US, "%.2f", amount);
    }

    private void deleteExpense(String expenseId) {
        firestore.collection("groups")
                .document(groupId)
                .collection("expenses")
                .document(expenseId)
                .collection("splits")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    com.google.firebase.firestore.WriteBatch batch = firestore.batch();
                    for (QueryDocumentSnapshot splitDocument : queryDocumentSnapshots) {
                        batch.delete(splitDocument.getReference());
                    }
                    batch.delete(
                            firestore.collection("groups")
                                    .document(groupId)
                                    .collection("expenses")
                                    .document(expenseId)
                    );
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
                                loadExpenses();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete expense: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load expense splits: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void markSplitAsPaid(String expenseId, String participantUserId, String participantLabel,
                                 String paidBy, String shareAmount, String expenseTitle) {
        firestore.collection("groups")
                .document(groupId)
                .collection("expenses")
                .document(expenseId)
                .collection("splits")
                .document(participantUserId)
                .update(
                        "status", "paid",
                        "settledAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {
                    createSettledNotification(participantUserId, participantLabel, paidBy, shareAmount, expenseTitle);
                    Toast.makeText(this, "Marked as paid", Toast.LENGTH_SHORT).show();
                    loadExpenses();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to mark as paid: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void createSettledNotification(String participantUserId, String participantLabel,
                                           String paidBy, String shareAmount, String expenseTitle) {
        java.util.Map<String, Object> notificationMap = new java.util.HashMap<>();
        notificationMap.put("title", "Contribution settled");
        notificationMap.put("message", participantLabel + " settled Rs " + shareAmount + " for \"" + expenseTitle + "\".");
        notificationMap.put("type", "expense_settled");
        notificationMap.put("groupId", groupId);
        notificationMap.put("createdAt", System.currentTimeMillis());
        notificationMap.put("fromUserId", participantUserId);

        firestore.collection("users")
                .whereEqualTo("email", paidBy)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot payerDocument : queryDocumentSnapshots) {
                        firestore.collection("users")
                                .document(payerDocument.getId())
                                .collection("notifications")
                                .document()
                                .set(notificationMap);
                    }
                });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
