package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GroupDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        ImageView imageViewBack = findViewById(R.id.imageViewBackGroupDetails);
        TextView textViewGroupTitle = findViewById(R.id.textViewGroupTitle);
        TextView textViewGroupPurpose = findViewById(R.id.textViewGroupPurpose);
        TextView textViewGroupId = findViewById(R.id.textViewGroupId);
        Button buttonAddExpense = findViewById(R.id.buttonAddExpenseFromGroup);

        String groupId = getIntent().getStringExtra("groupId");
        String groupName = getIntent().getStringExtra("groupName");
        String groupPurpose = getIntent().getStringExtra("groupPurpose");

        textViewGroupTitle.setText(groupName != null ? groupName : "Group Details");
        textViewGroupPurpose.setText(groupPurpose != null ? groupPurpose : "No purpose added");
        textViewGroupId.setText(groupId != null ? "Group ID: " + groupId : "Group ID unavailable");

        imageViewBack.setOnClickListener(v -> finish());

        buttonAddExpense.setOnClickListener(v -> Toast.makeText(
                this,
                "Add Expense screen is the next feature we will connect here.",
                Toast.LENGTH_SHORT
        ).show());
    }
}
