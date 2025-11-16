package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView; // **MỚI**
import android.widget.ArrayAdapter; // **MỚI**
import android.widget.Button;
import android.widget.Spinner; // **MỚI**
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.campusexpensemanager.BaseActivity; // **MỚI**
import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Currency; // **MỚI**
import com.example.campusexpensemanager.models.User;
import com.example.campusexpensemanager.utils.CurrencyHelper;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List; // **MỚI**

/**
 * ProfileActivity manages user profile viewing and editing
 * Features: Edit profile, Change password, Dark mode toggle, Logout
 */
public class ProfileActivity extends BaseActivity {

    private TextInputLayout tilName, tilEmail, tilAddress, tilPhone;
    private TextInputLayout tilOldPassword, tilNewPassword, tilConfirmNewPassword;
    private TextInputEditText etName, etEmail, etAddress, etPhone;
    private TextInputEditText etOldPassword, etNewPassword, etConfirmNewPassword;
    private SwitchMaterial switchDarkMode;
    private Button btnEditMode, btnSaveProfile, btnChangePassword, btnLogout;

    // **SPRINT 6: Thêm Spinners**
    private Spinner spinnerDefaultCurrency, spinnerLanguage;
    private ArrayAdapter<Currency> currencyAdapter;
    private ArrayAdapter<CharSequence> languageAdapter;
    private List<Currency> currencyList;
    private boolean isLanguageSpinnerInitialized = false; // Cờ để tránh gọi recreate() khi khởi tạo
    private boolean isCurrencySpinnerInitialized = false;

    private DatabaseHelper dbHelper;
    private CurrencyHelper currencyHelper;
    private User currentUser;

    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check authentication BEFORE setContentView
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        // Set content view
        setContentView(R.layout.activity_profile);
        dbHelper = DatabaseHelper.getInstance(this);
        currencyHelper = CurrencyHelper.getInstance(this);

        // Initialize views
        initializeViews();

        // Load current user (with error handling)
        try {
            loadCurrentUser();

            // Only proceed if user loaded successfully
            if (currentUser != null) {
                // Display user info FIRST (without calling setEditMode)
                populateFields();

                // Setup dark mode
                setupDarkMode();

                // **SPRINT 6: Setup Spinners MỚI**
                setupCurrencySpinner();
                setupLanguageSpinner();

                // Setup click listeners
                setupClickListeners();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading profile: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        // Setup back pressed callback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isEditMode) {
                    // Cancel edit mode instead of going back
                    cancelEdit();
                } else {
                    // Allow default back behavior
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void initializeViews() {
        // Profile fields
        tilName = findViewById(R.id.til_profile_name);
        tilEmail = findViewById(R.id.til_profile_email);
        tilAddress = findViewById(R.id.til_profile_address);
        tilPhone = findViewById(R.id.til_profile_phone);

        etName = findViewById(R.id.et_profile_name);
        etEmail = findViewById(R.id.et_profile_email);
        etAddress = findViewById(R.id.et_profile_address);
        etPhone = findViewById(R.id.et_profile_phone);

        // Password change fields
        tilOldPassword = findViewById(R.id.til_old_password);
        tilNewPassword = findViewById(R.id.til_new_password);
        tilConfirmNewPassword = findViewById(R.id.til_confirm_new_password);

        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmNewPassword = findViewById(R.id.et_confirm_new_password);

        // Controls
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        btnEditMode = findViewById(R.id.btn_edit_mode);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnLogout = findViewById(R.id.btn_logout);

        // **SPRINT 6: Find Spinners**
        spinnerDefaultCurrency = findViewById(R.id.spinner_default_currency);
        spinnerLanguage = findViewById(R.id.spinner_language);
    }

    private void loadCurrentUser() {
        try {
            int userId = sessionManager.getUserId();
            currentUser = dbHelper.getUserById(userId);

            if (currentUser == null) {
                Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }
    }

    /**
     * Populate fields WITHOUT calling setEditMode (avoid recursion)
     */
    private void populateFields() {
        if (currentUser == null) {
            return;
        }

        // Set values
        etName.setText(currentUser.getName() != null ? currentUser.getName() : "");
        etEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        etAddress.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "");
        etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");

        // Email is always disabled
        etEmail.setEnabled(false);

        // Set initial state to VIEW mode (all disabled except email)
        etName.setEnabled(false);
        etAddress.setEnabled(false);
        etPhone.setEnabled(false);

        btnEditMode.setText(getString(R.string.profile_edit));
        btnSaveProfile.setVisibility(View.GONE);
    }

    private void setupDarkMode() {
        // Set switch state from session
        boolean isDarkMode = sessionManager.isDarkModeEnabled();
        switchDarkMode.setChecked(isDarkMode);

        // Apply dark mode
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    // **SPRINT 6: Hàm mới cho Currency Spinner**
    private void setupCurrencySpinner() {
        currencyList = dbHelper.getAllCurrencies();
        currencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencyList);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDefaultCurrency.setAdapter(currencyAdapter);

        // Set lựa chọn mặc định
        int defaultCurrencyId = sessionManager.getDefaultCurrencyId();
        for (int i = 0; i < currencyList.size(); i++) {
            if (currencyList.get(i).getId() == defaultCurrencyId) {
                spinnerDefaultCurrency.setSelection(i);
                break;
            }
        }

        // Thêm listener
        spinnerDefaultCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isCurrencySpinnerInitialized) {
                    isCurrencySpinnerInitialized = true;
                    return; // Bỏ qua lần chạy đầu tiên
                }
                Currency selectedCurrency = (Currency) parent.getItemAtPosition(position);
                updateDefaultCurrency(selectedCurrency.getId());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // **SPRINT 6: Hàm mới cho Language Spinner**
    private void setupLanguageSpinner() {
        languageAdapter = ArrayAdapter.createFromResource(this,
                R.array.languages, android.R.layout.simple_spinner_item); // (Cần tạo R.array.languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(languageAdapter);

        // Set lựa chọn mặc định
        String currentLang = sessionManager.getLanguage();
        if (currentLang.equals("vi")) {
            spinnerLanguage.setSelection(1);
        } else {
            spinnerLanguage.setSelection(0); // en
        }

        // Thêm listener
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isLanguageSpinnerInitialized) {
                    isLanguageSpinnerInitialized = true;
                    return; // Bỏ qua lần chạy đầu tiên
                }
                String langCode = (position == 1) ? "vi" : "en";
                updateLanguage(langCode);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        // Edit mode toggle
        btnEditMode.setOnClickListener(v -> {
            if (isEditMode) {
                cancelEdit();
            } else {
                enterEditMode();
            }
        });

        // Save profile
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // Change password
        btnChangePassword.setOnClickListener(v -> changePassword());

        // Dark mode toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // Only respond to user interaction
                toggleDarkMode(isChecked);
            }
        });

        // Logout
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    /**
     * Enter edit mode - enable fields
     */
    private void enterEditMode() {
        isEditMode = true;

        etName.setEnabled(true);
        etAddress.setEnabled(true);
        etPhone.setEnabled(true);

        btnEditMode.setText("Cancel");
        btnSaveProfile.setVisibility(View.VISIBLE);
    }

    /**
     * Cancel edit mode - restore original values
     */
    private void cancelEdit() {
        isEditMode = false;

        // Restore original values
        if (currentUser != null) {
            etName.setText(currentUser.getName() != null ? currentUser.getName() : "");
            etAddress.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "");
            etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
        }

        // Disable fields
        etName.setEnabled(false);
        etAddress.setEnabled(false);
        etPhone.setEnabled(false);

        btnEditMode.setText(getString(R.string.profile_edit));
        btnSaveProfile.setVisibility(View.GONE);
    }

    /**
     * Save profile changes
     */
    private void saveProfile() {
        // Validate inputs
        String name = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty()) {
            tilName.setError(getString(R.string.error_empty_field));
            return;
        }

        if (address.isEmpty()) {
            tilAddress.setError(getString(R.string.error_empty_field));
            return;
        }

        if (phone.isEmpty()) {
            tilPhone.setError(getString(R.string.error_empty_field));
            return;
        }

        // Validate phone format
        if (!phone.matches("^0\\d{9}$")) {
            tilPhone.setError(getString(R.string.error_invalid_phone));
            return;
        }

        // Clear errors
        tilName.setError(null);
        tilAddress.setError(null);
        tilPhone.setError(null);

        // Update user object
        currentUser.setName(name);
        currentUser.setAddress(address);
        currentUser.setPhone(phone);

        // Save to database
        int rowsAffected = dbHelper.updateUser(currentUser);

        if (rowsAffected > 0) {
            // Update session
            sessionManager.updateUserName(name);

            Toast.makeText(this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();

            // Exit edit mode
            cancelEdit();
        } else {
            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Change password with validation
     */
    private void changePassword() {
        String oldPassword = etOldPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmNewPassword = etConfirmNewPassword.getText().toString();

        // Validate old password
        if (oldPassword.isEmpty()) {
            tilOldPassword.setError(getString(R.string.error_empty_field));
            return;
        }

        String oldPasswordHash = SessionManager.hashPassword(oldPassword);
        if (!oldPasswordHash.equals(currentUser.getPasswordHash())) {
            tilOldPassword.setError("Incorrect password");
            return;
        }

        // Validate new password
        if (newPassword.isEmpty()) {
            tilNewPassword.setError(getString(R.string.error_empty_field));
            return;
        }

        if (newPassword.length() < 8) {
            tilNewPassword.setError(getString(R.string.error_password_short));
            return;
        }

        // Validate confirm password
        if (!confirmNewPassword.equals(newPassword)) {
            tilConfirmNewPassword.setError(getString(R.string.error_password_mismatch));
            return;
        }

        // Clear errors
        tilOldPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmNewPassword.setError(null);

        // Update password
        String newPasswordHash = SessionManager.hashPassword(newPassword);
        currentUser.setPasswordHash(newPasswordHash);

        int rowsAffected = dbHelper.updateUser(currentUser);

        if (rowsAffected > 0) {
            Toast.makeText(this, getString(R.string.profile_password_changed), Toast.LENGTH_SHORT).show();

            // Clear password fields
            etOldPassword.setText("");
            etNewPassword.setText("");
            etConfirmNewPassword.setText("");
        } else {
            Toast.makeText(this, "Failed to change password", Toast.LENGTH_SHORT).show();
        }
    }

    // **SPRINT 6: Hàm mới update Currency**
    private void updateDefaultCurrency(int currencyId) {
        // Cập nhật Session
        sessionManager.setDefaultCurrencyId(currencyId);

        // Cập nhật DB
        if (currentUser != null) {
            currentUser.setDefaultCurrencyId(currencyId);
            dbHelper.updateUser(currentUser);
        }
        Toast.makeText(this, "Default currency updated", Toast.LENGTH_SHORT).show();
    }

    // **SPRINT 6: Hàm mới update Language**
    private void updateLanguage(String langCode) {
        if (!sessionManager.getLanguage().equals(langCode)) {
            sessionManager.setLanguage(langCode);
            // Tải lại Activity để áp dụng ngôn ngữ
            recreateActivity();
        }
    }

    /**
     * Toggle dark mode with smooth transition
     */
    private void toggleDarkMode(boolean enabled) {
        try {
            // Save preference
            sessionManager.setDarkMode(enabled);

            // Update database
            if (currentUser != null) {
                currentUser.setDarkModeEnabled(enabled);
                dbHelper.updateUser(currentUser);
            }

            // Apply dark mode with animation
            if (enabled) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            // Recreate activity with fade animation
            recreateActivity();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error toggling dark mode: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show logout confirmation dialog
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_logout_title))
                .setMessage(getString(R.string.confirm_logout_message))
                .setPositiveButton(getString(R.string.action_yes), (dialog, which) -> logout())
                .setNegativeButton(getString(R.string.action_no), null)
                .show();
    }

    /**
     * Logout user and navigate to login
     */
    private void logout() {
        sessionManager.logout();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    /**
     * Navigate to LoginActivity
     */
    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}