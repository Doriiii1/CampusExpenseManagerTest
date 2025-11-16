package com.example.campusexpensemanager.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.campusexpensemanager.BaseActivity;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.CurrencyHelper;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * AddIncomeActivity handles adding new income transactions
 * Similar to AddExpenseActivity but for income tracking
 */
public class AddIncomeActivity extends BaseActivity {

    private TextInputLayout tilAmount, tilDescription;
    private TextInputEditText etAmount, etDescription;
    private Spinner spinnerCategory;
    private Button btnSelectDate, btnSelectTime;
    private FloatingActionButton fabSave;

    private DatabaseHelper dbHelper;
    private CurrencyHelper currencyHelper;

    private List<Category> categories;
    private Calendar selectedDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_income);

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        currencyHelper = CurrencyHelper.getInstance(this);

        // Check authentication
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        // Initialize date/time to current
        selectedDateTime = Calendar.getInstance();

        // Initialize views
        initializeViews();

        // Load income categories
        loadCategories();

        // Setup click listeners
        setupClickListeners();

        // Set initial date/time text
        updateDateTimeButtons();
    }

    private void initializeViews() {
        tilAmount = findViewById(R.id.til_amount);
        tilDescription = findViewById(R.id.til_description);
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSelectTime = findViewById(R.id.btn_select_time);
        fabSave = findViewById(R.id.fab_save);
    }

    /**
     * Load income categories from database
     * Uses same Category table but filter by income types
     */
    private void loadCategories() {
        // For MVP: Use existing categories or create income-specific ones
        categories = dbHelper.getAllCategories();

        if (categories.isEmpty()) {
            Toast.makeText(this, "No categories available", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        fabSave.setOnClickListener(v -> saveIncome());
    }

    private void showDatePicker() {
        int year = selectedDateTime.get(Calendar.YEAR);
        int month = selectedDateTime.get(Calendar.MONTH);
        int day = selectedDateTime.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDateTime.set(Calendar.YEAR, selectedYear);
                    selectedDateTime.set(Calendar.MONTH, selectedMonth);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, selectedDay);
                    updateDateTimeButtons();
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void showTimePicker() {
        int hour = selectedDateTime.get(Calendar.HOUR_OF_DAY);
        int minute = selectedDateTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                    selectedDateTime.set(Calendar.MINUTE, selectedMinute);
                    updateDateTimeButtons();
                },
                hour, minute, true
        );

        timePickerDialog.show();
    }

    private void updateDateTimeButtons() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        btnSelectDate.setText(dateFormat.format(selectedDateTime.getTime()));
        btnSelectTime.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    /**
     * Validate and save income to database
     */
    private void saveIncome() {
        // Validate amount
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            tilAmount.setError(getString(R.string.error_empty_field));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            tilAmount.setError("Invalid amount");
            return;
        }

        if (amount <= 0) {
            tilAmount.setError("Amount must be greater than 0");
            return;
        }

        tilAmount.setError(null);

        // Validate category
        if (spinnerCategory.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get form data
        int userId = sessionManager.getUserId();
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        int categoryId = selectedCategory.getId();
        long dateTime = selectedDateTime.getTimeInMillis();
        String description = etDescription.getText().toString().trim();

        // Create income object (using Expense model with type=INCOME)
        Expense income = new Expense(userId, categoryId, amount, dateTime, description, Expense.TYPE_INCOME);
        income.setCurrencyId(1); // Default VND

        // Insert into database
        long incomeId = dbHelper.insertExpense(income);

        if (incomeId != -1) {
            NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
            String formattedAmount = currencyFormat.format(amount) + "đ";

            Toast.makeText(this,
                    "Income added: " + formattedAmount,
                    Toast.LENGTH_SHORT).show();

            finish();
        } else {
            Toast.makeText(this, "Failed to add income", Toast.LENGTH_SHORT).show();
        }
    }
}