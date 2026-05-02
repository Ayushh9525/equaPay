package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class GroupDetailsActivity extends AppCompatActivity {

    private String groupId;
    private String groupName;
    private String groupPurpose;
    private TextView textViewExpensesStatus;
    private LinearLayout layoutExpensesContainer;
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
        textViewExpensesStatus = findViewById(R.id.textViewExpensesStatus);
        layoutExpensesContainer = findViewById(R.id.layoutExpensesContainer);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExpenses();
    }

    private void loadExpenses() {
        if (groupId == null || groupId.isEmpty()) {
            textViewExpensesStatus.setText("Group not found.");
            layoutExpensesContainer.removeAllViews();
            return;
        }

        textViewExpensesStatus.setText("Loading expenses...");
        layoutExpensesContainer.removeAllViews();

        firestore.collection("groups")
                .document(groupId)
                .collection("expenses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutExpensesContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewExpensesStatus.setText("No expenses added yet. Use the button below to start tracking shared costs.");
                        return;
                    }

                    textViewExpensesStatus.setText("Saved expenses");

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String expenseTitle = document.getString("expenseTitle");
                        Object amountObject = document.get("amount");
                        String paidBy = document.getString("paidBy");

                        String amountText = amountObject != null ? String.valueOf(amountObject) : "0";
                        addExpenseCard(
                                expenseTitle != null ? expenseTitle : "Untitled expense",
                                amountText,
                                paidBy != null ? paidBy : "Unknown payer"
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    layoutExpensesContainer.removeAllViews();
                    textViewExpensesStatus.setText("Failed to load expenses: " + e.getMessage());
                });
    }

    private void addExpenseCard(String expenseTitle, String amount, String paidBy) {
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

        cardLayout.addView(titleTextView);
        cardLayout.addView(amountTextView);
        cardLayout.addView(paidByTextView);
        layoutExpensesContainer.addView(cardLayout);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
