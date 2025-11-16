package com.example.campusexpensemanager.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.campusexpensemanager.BaseActivity;
import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Currency;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.CurrencyHelper;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddExpenseActivity extends BaseActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private TextInputLayout tilAmount, tilDescription;
    private TextInputEditText etAmount, etDescription;
    private Spinner spinnerCategory;
    private Button btnSelectDate, btnSelectTime, btnCaptureReceipt;
    private ImageView ivReceiptPreview;
    private FloatingActionButton fabSave;

    // **SPRINT 5: Thêm Views**
    private RadioGroup rgTransactionType;
    private CheckBox cbRecurring;
    private Spinner spinnerRecurrencePeriod;
    private TextView tvAddTitle;

    // **SPRINT 6: Thêm Currency Spinner**
    private Spinner spinnerCurrency;
    private ArrayAdapter<Currency> currencyAdapter;
    private List<Currency> currencyList;
    private CurrencyHelper currencyHelper;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private List<Category> categories;
    private Calendar selectedDateTime;
    private String receiptPhotoPath;

    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // ... (Initialize helpers, Check auth, Initialize date/time)
        dbHelper = DatabaseHelper.getInstance(this);
        currencyHelper = CurrencyHelper.getInstance(this);

        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        selectedDateTime = Calendar.getInstance();

        initializeViews();
        loadCategories();

        loadCurrencies();

        setupCameraLauncher();
        setupClickListeners();

        // **SPRINT 5: Setup cho Recurring
        setupRecurringSpinner();
        setupRecurringListener();

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
        btnCaptureReceipt = findViewById(R.id.btn_capture_receipt);
        ivReceiptPreview = findViewById(R.id.iv_receipt_preview);
        fabSave = findViewById(R.id.fab_save);

        // **SPRINT 5: Initialize views mới**
        tvAddTitle = findViewById(R.id.tv_add_title); // Đảm bảo bạn có ID này trên TextView tiêu đề
        rgTransactionType = findViewById(R.id.rg_transaction_type);
        cbRecurring = findViewById(R.id.cb_recurring);
        spinnerRecurrencePeriod = findViewById(R.id.spinner_recurrence_period);

        // **SPRINT 6: Find Currency Spinner**
        spinnerCurrency = findViewById(R.id.spinner_currency);
    }

    // ... (loadCategories, setupCameraLauncher, showDatePicker, showTimePicker, updateDateTimeButtons, captureReceipt, launchCamera, onRequestPermissionsResult, createImageFile giữ nguyên) ...
    private void loadCategories() {
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

    // **SPRINT 6: Hàm mới load Currencies**
    private void loadCurrencies() {
        currencyList = currencyHelper.getAllCurrencies();
        currencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencyList);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(currencyAdapter);

        // Set lựa chọn mặc định từ SessionManager
        int defaultCurrencyId = sessionManager.getDefaultCurrencyId();
        for (int i = 0; i < currencyList.size(); i++) {
            if (currencyList.get(i).getId() == defaultCurrencyId) {
                spinnerCurrency.setSelection(i);
                break;
            }
        }
    }

    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        ivReceiptPreview.setVisibility(View.VISIBLE);
                        ivReceiptPreview.setImageURI(Uri.fromFile(new File(receiptPhotoPath)));
                        Toast.makeText(this, "Receipt captured", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        btnCaptureReceipt.setOnClickListener(v -> captureReceipt());
        fabSave.setOnClickListener(v -> saveExpense());
    }

    // **SPRINT 5: Hàm mới để setup spinner lặp lại**
    private void setupRecurringSpinner() {
        String[] periods = {"Weekly", "Monthly", "Yearly"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                periods
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecurrencePeriod.setAdapter(adapter);
    }

    // **SPRINT 5: Hàm mới để lắng nghe checkbox lặp lại**
    private void setupRecurringListener() {
        cbRecurring.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                spinnerRecurrencePeriod.setVisibility(View.VISIBLE);
            } else {
                spinnerRecurrencePeriod.setVisibility(View.GONE);
            }
        });
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

    private void captureReceipt() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
            return;
        }
        launchCamera();
    }

    private void launchCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
                return;
            }
            File photoFile = createImageFile();
            if (photoFile != null) {
                receiptPhotoPath = photoFile.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(
                        this,
                        "com.example.campusexpensemanager.fileprovider",
                        photoFile
                );
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            } else {
                Toast.makeText(this, "Failed to create photo file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(System.currentTimeMillis());
            String imageFileName = "RECEIPT_" + timeStamp + "_";
            File storageDir = getExternalFilesDir("receipts");
            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs();
            }
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Validate and save expense to database
     */
    private void saveExpense() {
        // ... (Validate amount)
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

        // ... (Validate category)
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

        // **SPRINT 5: Lấy Transaction Type**
        int transactionType = rgTransactionType.getCheckedRadioButtonId() == R.id.rb_income ?
                Expense.TYPE_INCOME : Expense.TYPE_EXPENSE;

        // **SPRINT 6: Lấy Currency ID**
        Currency selectedCurrency = (Currency) spinnerCurrency.getSelectedItem();
        int currencyId = (selectedCurrency != null) ? selectedCurrency.getId() : 1; // Mặc định là 1 (VND)

        // Create expense object
        Expense expense = new Expense(userId, categoryId, amount, dateTime, description, transactionType);
        expense.setCurrencyId(currencyId);
        expense.setReceiptPath(receiptPhotoPath);

        // **SPRINT 5: Xử lý Recurring**
        if (cbRecurring.isChecked()) {
            String period = spinnerRecurrencePeriod.getSelectedItem().toString();
            Calendar nextOccurrenceCal = (Calendar) selectedDateTime.clone();

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

            expense.setRecurring(true);
            expense.setRecurrencePeriod(period);
            expense.setNextOccurrenceDate(nextOccurrenceCal.getTimeInMillis());
        }

        // Insert into database
        long expenseId = dbHelper.insertExpense(expense);

        if (expenseId != -1) {
            NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
            String formattedAmount = currencyHelper.formatAmount(amount, currencyId);

            // Cập nhật Toast
            String message = (transactionType == Expense.TYPE_INCOME ? "Income" : "Expense") + " added: " + formattedAmount;
            if (expense.isRecurring()) {
                message += " (Recurring " + expense.getRecurrencePeriod() + ")";
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            finish();
        } else {
            Toast.makeText(this, "Failed to add transaction", Toast.LENGTH_SHORT).show();
        }
    }
}