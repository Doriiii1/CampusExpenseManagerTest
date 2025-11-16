package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campusexpensemanager.BaseActivity;
import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Budget;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.CurrencyHelper;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.NumberFormat; // **MỚI**
import java.util.Calendar;
import java.util.List;
import java.util.Locale; // **MỚI**

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private BottomNavigationView bottomNavigation;
    private LinearLayout layoutDashboard;

    // **SPRINT 5: Thêm TextViews**
    private TextView tvGreeting, tvMonthlyTotal, tvBudgetRemaining, tvTopCategory;
    private TextView tvMonthlyIncome, tvMonthlyBalance; // **MỚI**

    private Button btnAddExpense, btnViewBudget, btnGenerateReport;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private CurrencyHelper currencyHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ... (onCreate giữ nguyên) ...
        super.onCreate(savedInstanceState);

        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }
        setContentView(R.layout.activity_main);
        dbHelper = DatabaseHelper.getInstance(this);
        currencyHelper = CurrencyHelper.getInstance(this);
        initializeViews();
        setupBottomNavigation();
        showDashboard();
    }

    // ... (navigateToLogin giữ nguyên) ...
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        layoutDashboard = findViewById(R.id.layout_dashboard);
        tvGreeting = findViewById(R.id.tv_greeting);

        // **SPRINT 5: Find views mới**
        tvMonthlyIncome = findViewById(R.id.tv_monthly_income); // **MỚI**
        tvMonthlyTotal = findViewById(R.id.tv_monthly_total);
        tvMonthlyBalance = findViewById(R.id.tv_monthly_balance); // **MỚI**

        tvBudgetRemaining = findViewById(R.id.tv_budget_remaining);
        tvTopCategory = findViewById(R.id.tv_top_category);
        btnAddExpense = findViewById(R.id.btn_add_expense);
        btnViewBudget = findViewById(R.id.btn_view_budget);
        btnGenerateReport = findViewById(R.id.btn_generate_report);

        // ... (Button clicks giữ nguyên) ...
        btnAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });
        btnViewBudget.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BudgetDashboardActivity.class);
            startActivity(intent);
        });
        btnGenerateReport.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                showDashboard();
                return true;
                // **SPRINT 5: Đổi tên nav_expenses thành nav_transactions**
            } else if (itemId == R.id.nav_transactions) {
                Intent intent = new Intent(MainActivity.this, ExpenseListActivity.class);
                startActivity(intent);
                return false;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return false;
            }

            return false;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
    }

    // ... (showDashboard) ...
    private void showDashboard() {
        layoutDashboard.setVisibility(View.VISIBLE);
        loadDashboardData();
    }

    /**
     * Load dashboard summary data
     */
    private void loadDashboardData() {
        try {
            int userId = sessionManager.getUserId();

            // ... (Greeting) ...
            String userName = sessionManager.getUserName();
            if (userName == null || userName.isEmpty()) { userName = "User"; }
            tvGreeting.setText("Hello, " + userName + "! 👋");

            // Lấy TẤT CẢ transactions
            List<Expense> expenses = dbHelper.getExpensesByUser(userId);

            // ... (Tính ngày đầu tháng) ...
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            long monthStart = calendar.getTimeInMillis();

            // **SPRINT 5: Tính toán Income, Expense, Balance**
            double monthlyTotalExpense = 0;
            double monthlyTotalIncome = 0;
            java.util.Map<Integer, Double> categoryTotals = new java.util.HashMap<>();

            for (Expense expense : expenses) {
                if (expense.getDate() >= monthStart) {

                    // **SPRINT 6: Chuyển đổi sang VND trước khi tính toán**
                    double amountInVND = currencyHelper.convertToVND(
                            expense.getAmount(),
                            expense.getCurrencyId()
                    );

                    if (expense.isIncome()) {
                        monthlyTotalIncome += amountInVND;
                    } else {
                        monthlyTotalExpense += amountInVND;
                        int categoryId = expense.getCategoryId();
                        categoryTotals.put(categoryId,
                                categoryTotals.getOrDefault(categoryId, 0.0) + amountInVND);
                    }
                }
            }

            double monthlyBalance = monthlyTotalIncome - monthlyTotalExpense;

            // ... (Tính Budget) ...
            List<Budget> budgets = dbHelper.getBudgetsByUser(userId);
            double totalBudget = 0;
            for (Budget budget : budgets) {
                if (budget.getPeriodEnd() >= System.currentTimeMillis()) {
                    totalBudget += budget.getAmount();
                }
            }
            // Chỉ tính budget remaining dựa trên expense
            double budgetRemaining = totalBudget - monthlyTotalExpense;

            // ... (Tìm Top Category) ...
            String topCategory = "None";
            double topAmount = 0;
            for (java.util.Map.Entry<Integer, Double> entry : categoryTotals.entrySet()) {
                if (entry.getValue() > topAmount) {
                    topAmount = entry.getValue();
                    Category cat = dbHelper.getCategoryById(entry.getKey());
                    if (cat != null) {
                        topCategory = cat.getName();
                    }
                }
            }

            // **SPRINT 6: Dùng CurrencyHelper để format (chỉ VND)**
            NumberFormat vndFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            String formattedIncome = vndFormat.format(monthlyTotalIncome);
            String formattedExpense = vndFormat.format(monthlyTotalExpense);
            String formattedBalance = vndFormat.format(monthlyBalance);
            String formattedRemaining = vndFormat.format(Math.max(0, budgetRemaining));
            String formattedTopAmount = vndFormat.format(topAmount);

            // Update UI
            tvMonthlyIncome.setText("Total Income: " + formattedIncome);
            tvMonthlyTotal.setText("Total Spent: " + formattedExpense);
            tvMonthlyBalance.setText("Balance: " + formattedBalance);
            tvBudgetRemaining.setText("Budget Remaining: " + formattedRemaining);
            tvTopCategory.setText("Top Category: " + topCategory + " (" + formattedTopAmount + ")");

            // Đặt màu cho Balance
            if (monthlyBalance < 0) {
                tvMonthlyBalance.setTextColor(ContextCompat.getColor(this, R.color.error));
            } else {
                tvMonthlyBalance.setTextColor(ContextCompat.getColor(this, R.color.success));
            }

        } catch (Exception e) {
            e.printStackTrace();
            // ... (Xử lý lỗi) ...
            tvGreeting.setText("Hello, User! 👋");
            tvMonthlyIncome.setText("Total Income: 0đ");
            tvMonthlyTotal.setText("Total Spent: 0đ");
            tvMonthlyBalance.setText("Balance: 0đ");
            tvBudgetRemaining.setText("Budget Remaining: 0đ");
            tvTopCategory.setText("Top Category: None");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (layoutDashboard != null && layoutDashboard.getVisibility() == View.VISIBLE) {
            loadDashboardData();
        }
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        }
    }
}