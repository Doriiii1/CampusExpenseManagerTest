package com.example.campusexpensemanager.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import com.example.campusexpensemanager.BaseActivity;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.CurrencyHelper;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * EditIncomeActivity handles editing and deleting income transactions
 */
public class EditIncomeActivity extends BaseActivity {

    private TextInputLayout tilAmount, tilDescription;
    private TextInputEditText etAmount, etDescription;
    private Spinner spinnerCategory;
    private Button btnSelectDate, btnSelectTime, btnDelete;
    private FloatingActionButton fabUpdate;

    private DatabaseHelper dbHelper;
    private CurrencyHelper currencyHelper;

    private Expense currentIncome;
    private List<Category> categories;
    private Calendar selectedDateTime;

    private Expense deletedIncome; // For undo functionality

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_income);

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        currencyHelper = CurrencyHelper.getInstance(this);

        // Check authentication
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        // Get income ID from intent
        int incomeId = getIntent().getIntExtra("income_id", -1);
        if (incomeId == -1) {
            Toast.makeText(this, "Invalid income", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load income from database
        loadIncome(incomeId);

        // Initialize views
        initializeViews();

        // Load categories
        loadCategories();

        // Pre-fill form
        prefillForm();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        tilAmount = findViewById(R.id.til_amount);
        tilDescription = findViewById(R.id.til_description);
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSelectTime = findViewById(R.id.btn_select_time);
        btnDelete = findViewById(R.id.btn_delete);
        fabUpdate = findViewById(R.id.fab_update);
    }

    /**
     * Load income from database
     */
    private void loadIncome(int incomeId) {
        // Query database for income (expense with type=INCOME)
        List<Expense> allExpenses = dbHelper.getExpensesByUser(sessionManager.getUserId());

        for (Expense expense : allExpenses) {
            if (expense.getId() == incomeId && expense.getType() == Expense.TYPE_INCOME) {
                currentIncome = expense;
                break;
            }
        }

        if (currentIncome == null) {
            Toast.makeText(this, "Income not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Load categories from database
     */
    private void loadCategories() {
        categories = dbHelper.getAllCategories();

        ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    /**
     * Pre-fill form with current income data
     */
    private void prefillForm() {
        // Amount
        etAmount.setText(String.valueOf(currentIncome.getAmount()));

        // Category
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == currentIncome.getCategoryId()) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        // Date and time
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.setTimeInMillis(currentIncome.getDate());
        updateDateTimeButtons();

        // Description
        if (currentIncome.getDescription() != null) {
            etDescription.setText(currentIncome.getDescription());
        }
    }

    private void setupClickListeners() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        fabUpdate.setOnClickListener(v -> updateIncome());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
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
     * Validate and update income
     */
    private void updateIncome() {
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

        // Update income object
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        currentIncome.setAmount(amount);
        currentIncome.setCategoryId(selectedCategory.getId());
        currentIncome.setDate(selectedDateTime.getTimeInMillis());
        currentIncome.setDescription(etDescription.getText().toString().trim());

        // Update in database
        int rowsAffected = dbHelper.updateExpense(currentIncome);

        if (rowsAffected > 0) {
            NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
            String formattedAmount = currencyFormat.format(amount) + "đ";

            Toast.makeText(this,
                    "Income updated: " + formattedAmount,
                    Toast.LENGTH_SHORT).show();

            finish();
        } else {
            Toast.makeText(this, "Failed to update income", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Income")
                .setMessage("Are you sure you want to delete this income?")
                .setPositiveButton(getString(R.string.action_yes), (dialog, which) -> deleteIncome())
                .setNegativeButton(getString(R.string.action_no), null)
                .show();
    }

    /**
     * Delete income with undo option
     */
    private void deleteIncome() {
        // Store for undo
        deletedIncome = new Expense(
                currentIncome.getId(),
                currentIncome.getUserId(),
                currentIncome.getCategoryId(),
                currentIncome.getCurrencyId(),
                currentIncome.getAmount(),
                currentIncome.getDate(),
                currentIncome.getDescription(),
                currentIncome.getReceiptPath(),
                currentIncome.getCreatedAt(),
                currentIncome.getType()
        );

        // Delete from database
        int rowsDeleted = dbHelper.deleteExpense(currentIncome.getId());

        if (rowsDeleted > 0) {
            Snackbar snackbar = Snackbar.make(
                    findViewById(android.R.id.content),
                    "Income deleted",
                    Snackbar.LENGTH_LONG
            );

            snackbar.setAction("UNDO", v -> undoDelete());

            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    if (event != DISMISS_EVENT_ACTION) {
                        finish();
                    }
                }
            });

            snackbar.show();
        } else {
            Toast.makeText(this, "Failed to delete income", Toast.LENGTH_SHORT).show();
        }
    }

    private void undoDelete() {
        if (deletedIncome != null) {
            long newId = dbHelper.insertExpense(deletedIncome);

            if (newId != -1) {
                deletedIncome.setId((int) newId);
                currentIncome = deletedIncome;
                Toast.makeText(this, "Income restored", Toast.LENGTH_SHORT).show();
                prefillForm();
            } else {
                Toast.makeText(this, "Failed to restore income", Toast.LENGTH_SHORT).show();
            }
        }
    }
}