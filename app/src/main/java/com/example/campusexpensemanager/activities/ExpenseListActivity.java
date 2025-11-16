package com.example.campusexpensemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import com.example.campusexpensemanager.BaseActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.adapters.ExpenseAdapter;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.CurrencyHelper;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpenseListActivity extends BaseActivity implements ExpenseAdapter.OnExpenseClickListener {

    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private EditText etSearch;
    private TextView tvMonthlyTotal, tvExpenseCount, tvEmptyState;
    private FloatingActionButton fabAddExpense;

    // **SPRINT 5: Thêm ChipGroup**
    private ChipGroup chipGroupFilterType;
    private CurrencyHelper currencyHelper;

    private DatabaseHelper dbHelper;

    private List<Expense> expenses; // Đây là danh sách đầy đủ (master list)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // **SPRINT 5: Cập nhật Title**
            getSupportActionBar().setTitle("Transactions");
        }

        // ... (Initialize helpers, Check auth) ...
        dbHelper = DatabaseHelper.getInstance(this);
        currencyHelper = CurrencyHelper.getInstance(this);

        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupSearch();
        setupClickListeners();

        // **SPRINT 5: Setup Filter**
        setupFilterListeners();

        loadExpenses();

        // ... (BackPressedCallback) ...
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle back button in action bar
        onBackPressed();
        return true;
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_expenses);
        etSearch = findViewById(R.id.et_search);
        tvMonthlyTotal = findViewById(R.id.tv_monthly_total);
        tvExpenseCount = findViewById(R.id.tv_expense_count);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        fabAddExpense = findViewById(R.id.fab_add_expense);

        // **SPRINT 5: Find ChipGroup**
        chipGroupFilterType = findViewById(R.id.chip_group_filter_type);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        // Add new expense
        fabAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(ExpenseListActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        // Animate FAB on scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    // Scrolling down - hide FAB
                    fabAddExpense.hide();
                } else if (dy < 0) {
                    // Scrolling up - show FAB
                    fabAddExpense.show();
                }
            }
        });
    }

    // **SPRINT 5: Hàm mới setup Filter**
    private void setupFilterListeners() {
        chipGroupFilterType.setOnCheckedChangeListener((group, checkedId) -> {
            if (adapter == null) return;

            if (checkedId == R.id.chip_all) {
                adapter.filterByType(0); // 0 = All
            } else if (checkedId == R.id.chip_expenses) {
                adapter.filterByType(1); // 1 = Expenses
            } else if (checkedId == R.id.chip_income) {
                adapter.filterByType(2); // 2 = Income
            }
        });
    }

    /**
     * Load expenses from database
     */
    private void loadExpenses() {
        int userId = sessionManager.getUserId();
        // Lấy TẤT CẢ transactions
        expenses = dbHelper.getExpensesByUser(userId);

        if (expenses.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);

            if (adapter == null) {
                adapter = new ExpenseAdapter(this, expenses, this);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateExpenses(expenses);
            }
        }

        // Cập nhật lại filter hiện tại (ví dụ: nếu đang ở chip "Expenses")
        int checkedId = chipGroupFilterType.getCheckedChipId();
        if (adapter != null) {
            if (checkedId == R.id.chip_all) adapter.filterByType(0);
            else if (checkedId == R.id.chip_expenses) adapter.filterByType(1);
            else if (checkedId == R.id.chip_income) adapter.filterByType(2);
        }

        updateSummary();
    }

    /**
     * Cập nhật lại list khi quay lại từ Add/Edit
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại dữ liệu
        loadExpenses();
    }

    /**
     * Update monthly total and expense count
     */
    private void updateSummary() {
        // ... (Lấy ngày đầu tháng, cuối tháng) ...
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        // ... (Giữ nguyên)
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long monthStart = calendar.getTimeInMillis();
        calendar.add(Calendar.MONTH, 1);
        long monthEnd = calendar.getTimeInMillis();

        // **SPRINT 5: Tính toán Income và Expense**
        double monthlyTotalExpense = 0;
        double monthlyTotalIncome = 0;
        int monthlyCountExpense = 0;
        int monthlyCountIncome = 0;
        int totalCount = expenses.size();

        for (Expense expense : expenses) {
            if (expense.getDate() >= monthStart && expense.getDate() < monthEnd) {
                if (expense.isIncome()) {
                    monthlyTotalIncome += expense.getAmount();
                    monthlyCountIncome++;
                } else {
                    monthlyTotalExpense += expense.getAmount();
                    monthlyCountExpense++;
                }
            }
        }

        // Format and display
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedIncome = currencyFormat.format(monthlyTotalIncome) + "đ";
        String formattedExpense = currencyFormat.format(monthlyTotalExpense) + "đ";

        tvMonthlyTotal.setText("Thu: " + formattedIncome + " | Chi: " + formattedExpense);
        tvExpenseCount.setText(monthlyCountIncome + " thu | " +
                monthlyCountExpense + " chi tháng này | " +
                totalCount + " tổng");
    }

    /**
     * Handle expense item click
     */
    @Override
    public void onExpenseClick(Expense expense) {
        // Navigate to edit activity
        Intent intent = new Intent(ExpenseListActivity.this, EditExpenseActivity.class);
        intent.putExtra("expense_id", expense.getId());
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        // Return to MainActivity instead of reloading
        super.onBackPressed();
        finish();
    }
}