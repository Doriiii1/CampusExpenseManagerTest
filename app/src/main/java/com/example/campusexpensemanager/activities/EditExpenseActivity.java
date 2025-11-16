package com.example.campusexpensemanager.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusexpensemanager.BaseActivity;
import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Currency;
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
 * EditExpenseActivity handles editing and deleting expenses
 * Features: Pre-filled form, Update button, Delete with undo
 */
public class EditExpenseActivity extends BaseActivity {

    private TextInputLayout tilAmount, tilDescription;
    private TextInputEditText etAmount, etDescription;
    private Spinner spinnerCategory;
    private Button btnSelectDate, btnSelectTime, btnDelete;
    private ImageView ivReceiptPreview;
    private FloatingActionButton fabUpdate;

    // **SPRINT 5: Thêm Views**
    private RadioGroup rgTransactionType;
    private CheckBox cbRecurring;
    private Spinner spinnerRecurrencePeriod;
    private TextView tvEditTitle;
    private ArrayAdapter<String> recurrenceAdapter;

    // **SPRINT 6: Thêm Currency Spinner**
    private Spinner spinnerCurrency;
    private ArrayAdapter<Currency> currencyAdapter;
    private List<Currency> currencyList;
    private CurrencyHelper currencyHelper;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private Expense currentExpense;
    private List<Category> categories;
    private Calendar selectedDateTime;

    private Expense deletedExpense; // For undo functionality

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_expense);

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        currencyHelper = CurrencyHelper.getInstance(this);

        // Check authentication
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        // Get expense ID from intent
        int expenseId = getIntent().getIntExtra("expense_id", -1);
        if (expenseId == -1) {
            Toast.makeText(this, "Invalid expense", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load expense from database
        loadExpense(expenseId);

        // Initialize views
        initializeViews();

        // Load categories
        loadCategories();

        // **SPRINT 5: Setup
        setupRecurringSpinner();
        loadCurrencies();
        setupRecurringListener();


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
        ivReceiptPreview = findViewById(R.id.iv_receipt_preview);
        fabUpdate = findViewById(R.id.fab_update);

        // **SPRINT 5: Initialize views mới**
        tvEditTitle = findViewById(R.id.tv_edit_title);
        rgTransactionType = findViewById(R.id.rg_transaction_type);
        cbRecurring = findViewById(R.id.cb_recurring);
        spinnerRecurrencePeriod = findViewById(R.id.spinner_recurrence_period);

        spinnerCurrency = findViewById(R.id.spinner_currency);
    }

    /**
     * Load expense from database
     */
    private void loadExpense(int expenseId) {
        // Query database for expense
        List<Expense> allExpenses = dbHelper.getExpensesByUser(sessionManager.getUserId());

        for (Expense expense : allExpenses) {
            if (expense.getId() == expenseId) {
                currentExpense = expense;
                break;
            }
        }

        if (currentExpense == null) {
            Toast.makeText(this, "Expense not found", Toast.LENGTH_SHORT).show();
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

    // **SPRINT 5: Thêm hàm setup recurring
    private void setupRecurringSpinner() {
        String[] periods = {"Weekly", "Monthly", "Yearly"};
        recurrenceAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                periods
        );
        recurrenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecurrencePeriod.setAdapter(recurrenceAdapter);
    }

    private void setupRecurringListener() {
        cbRecurring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                spinnerRecurrencePeriod.setVisibility(View.VISIBLE);
            } else {
                spinnerRecurrencePeriod.setVisibility(View.GONE);
            }
        });
    }

    // **SPRINT 6: Hàm mới load Currencies**
    private void loadCurrencies() {
        currencyList = currencyHelper.getAllCurrencies();
        currencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencyList);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(currencyAdapter);
    }

    /**
     * Pre-fill form with current expense data
     */
    private void prefillForm() {
        // Amount
        etAmount.setText(String.valueOf(currentExpense.getAmount()));

        // Category
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == currentExpense.getCategoryId()) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        // Date and time
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.setTimeInMillis(currentExpense.getDate());
        updateDateTimeButtons();

        // Description
        if (currentExpense.getDescription() != null) {
            etDescription.setText(currentExpense.getDescription());
        }

        // Receipt preview (if available)
        if (currentExpense.getReceiptPath() != null && !currentExpense.getReceiptPath().isEmpty()) {
            ivReceiptPreview.setVisibility(View.VISIBLE);
            // TODO: Load image from path
        }

        // **SPRINT 5: Pre-fill Type và Recurring**
        if (currentExpense.isIncome()) {
            rgTransactionType.check(R.id.rb_income);
        } else {
            rgTransactionType.check(R.id.rb_expense);
        }

        cbRecurring.setChecked(currentExpense.isRecurring());
        if (currentExpense.isRecurring()) {
            spinnerRecurrencePeriod.setVisibility(View.VISIBLE);
            int periodPosition = recurrenceAdapter.getPosition(currentExpense.getRecurrencePeriod());
            spinnerRecurrencePeriod.setSelection(periodPosition);
        } else {
            spinnerRecurrencePeriod.setVisibility(View.GONE);
        }

        // **SPRINT 6: Pre-fill Currency Spinner**
        int currencyId = currentExpense.getCurrencyId();
        for (int i = 0; i < currencyList.size(); i++) {
            if (currencyList.get(i).getId() == currencyId) {
                spinnerCurrency.setSelection(i);
                break;
            }
        }
    }

    private void setupClickListeners() {
        // Date picker
        btnSelectDate.setOnClickListener(v -> showDatePicker());

        // Time picker
        btnSelectTime.setOnClickListener(v -> showTimePicker());

        // Update expense
        fabUpdate.setOnClickListener(v -> updateExpense());

        // Delete expense
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    /**
     * Show date picker dialog
     */
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

    /**
     * Show time picker dialog
     */
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

    /**
     * Update date and time button text
     */
    private void updateDateTimeButtons() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        btnSelectDate.setText(dateFormat.format(selectedDateTime.getTime()));
        btnSelectTime.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    /**
     * Validate and update expense
     */
    private void updateExpense() {
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

        Currency selectedCurrency = (Currency) spinnerCurrency.getSelectedItem();
        int currencyId = (selectedCurrency != null) ? selectedCurrency.getId() : 1;

        // Update expense object
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        currentExpense.setAmount(amount);
        currentExpense.setCategoryId(selectedCategory.getId());
        currentExpense.setDate(selectedDateTime.getTimeInMillis());
        currentExpense.setDescription(etDescription.getText().toString().trim());
        currentExpense.setCurrencyId(currencyId);

        // **SPRINT 5: Cập nhật Type và Recurring**
        int transactionType = rgTransactionType.getCheckedRadioButtonId() == R.id.rb_income ?
                Expense.TYPE_INCOME : Expense.TYPE_EXPENSE;
        currentExpense.setType(transactionType);

        if (cbRecurring.isChecked()) {
            String period = spinnerRecurrencePeriod.getSelectedItem().toString();
            Calendar nextOccurrenceCal = (Calendar) selectedDateTime.clone();

            // Tính toán ngày tiếp theo dựa trên ngày ĐÃ CHỌN (không phải ngày hiện tại)
            switch (period) {
                case "Weekly":
                    nextOccurrenceCal.add(Calendar.DAY_OF_YEAR, 7);
                    break;
                case "Monthly":
                    nextOccurrenceCal.add(Calendar.MONTH, 1);
                    break;
                case "Yearly":
                    nextOccurrenceCal.add(Calendar.YEAR, 1);
                    break;
            }

            currentExpense.setRecurring(true);
            currentExpense.setRecurrencePeriod(period);
            // Chỉ cập nhật ngày tiếp theo nếu nó khác
            // (logic phức tạp "update one vs all" có thể cần ở đây, hiện tại đang update rule)
            currentExpense.setNextOccurrenceDate(nextOccurrenceCal.getTimeInMillis());

        } else {
            // Tắt lặp lại
            currentExpense.setRecurring(false);
            currentExpense.setRecurrencePeriod(null);
            currentExpense.setNextOccurrenceDate(0);
        }

        // Update in database
        int rowsAffected = dbHelper.updateExpense(currentExpense);

        if (rowsAffected > 0) {
            Toast.makeText(this, "Transaction updated successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to update transaction", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show delete confirmation dialog
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.expense_delete))
                .setMessage(getString(R.string.expense_delete_confirm))
                .setPositiveButton(getString(R.string.action_yes), (dialog, which) -> deleteExpense())
                .setNegativeButton(getString(R.string.action_no), null)
                .show();
    }

    /**
     * Delete expense with undo option
     */
    private void deleteExpense() {
        // ... (Logic undo giữ nguyên, nhưng cần dùng constructor mới)
        deletedExpense = new Expense(
                currentExpense.getId(),
                currentExpense.getUserId(),
                currentExpense.getCategoryId(),
                currentExpense.getCurrencyId(),
                currentExpense.getAmount(),
                currentExpense.getDate(),
                currentExpense.getDescription(),
                currentExpense.getReceiptPath(),
                currentExpense.getCreatedAt(),
                currentExpense.getType(),
                currentExpense.isRecurring(),
                currentExpense.getRecurrencePeriod(),
                currentExpense.getNextOccurrenceDate()
        );

        int rowsDeleted = dbHelper.deleteExpense(currentExpense.getId());

        if (rowsDeleted > 0) {
            Snackbar snackbar = Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.expense_deleted),
                    Snackbar.LENGTH_LONG
            );
            snackbar.setAction(getString(R.string.expense_undo), v -> undoDelete());
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
            Toast.makeText(this, "Failed to delete expense", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Undo delete operation
     */
    private void undoDelete() {
        if (deletedExpense != null) {
            long newId = dbHelper.insertExpense(deletedExpense);

            if (newId != -1) {
                deletedExpense.setId((int) newId);
                currentExpense = deletedExpense;

                Toast.makeText(this, "Expense restored", Toast.LENGTH_SHORT).show();

                // Refresh form
                prefillForm();
            } else {
                Toast.makeText(this, "Failed to restore expense", Toast.LENGTH_SHORT).show();
            }
        }
    }
}