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
import com.example.campusexpensemanager.adapters.IncomeAdapter;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.CurrencyHelper;
import com.example.campusexpensemanager.utils.DatabaseHelper;
import com.example.campusexpensemanager.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * IncomeListActivity displays all user incomes in RecyclerView
 * Features: Search, filter, monthly summary, add new income
 */
public class IncomeListActivity extends BaseActivity implements IncomeAdapter.OnIncomeClickListener {

    private RecyclerView recyclerView;
    private IncomeAdapter adapter;
    private EditText etSearch;
    private TextView tvMonthlyTotal, tvIncomeCount, tvEmptyState;
    private FloatingActionButton fabAddIncome;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private CurrencyHelper currencyHelper;

    private List<Expense> incomes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_list);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Income");
        }

        // Initialize helpers
        dbHelper = DatabaseHelper.getInstance(this);
        currencyHelper = CurrencyHelper.getInstance(this);

        // Check authentication
        if (!sessionManager.isLoggedIn()) {
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup search
        setupSearch();

        // Setup click listeners
        setupClickListeners();

        // Load incomes
        loadIncomes();

        // Setup back pressed callback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_incomes);
        etSearch = findViewById(R.id.et_search);
        tvMonthlyTotal = findViewById(R.id.tv_monthly_total);
        tvIncomeCount = findViewById(R.id.tv_income_count);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        fabAddIncome = findViewById(R.id.fab_add_income);
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
        // Add new income
        fabAddIncome.setOnClickListener(v -> {
            Intent intent = new Intent(IncomeListActivity.this, AddIncomeActivity.class);
            startActivity(intent);
        });

        // Animate FAB on scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    fabAddIncome.hide();
                } else if (dy < 0) {
                    fabAddIncome.show();
                }
            }
        });
    }

    /**
     * Load incomes from database (type = INCOME only)
     */
    private void loadIncomes() {
        int userId = sessionManager.getUserId();

        // Get all expenses and filter by type = INCOME
        List<Expense> allExpenses = dbHelper.getExpensesByUser(userId);
        incomes = new ArrayList<>();

        for (Expense expense : allExpenses) {
            if (expense.getType() == Expense.TYPE_INCOME) {
                incomes.add(expense);
            }
        }

        if (incomes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);

            // Create adapter
            adapter = new IncomeAdapter(this, incomes, this);
            recyclerView.setAdapter(adapter);
        }

        // Calculate and display summary
        updateSummary();
    }

    /**
     * Update monthly total and income count
     */
    private void updateSummary() {
        // Get current month's incomes
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long monthStart = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        long monthEnd = calendar.getTimeInMillis();

        // Calculate total for this month
        double monthlyTotal = 0;
        int monthlyCount = 0;

        for (Expense income : incomes) {
            if (income.getDate() >= monthStart && income.getDate() < monthEnd) {
                monthlyTotal += income.getAmount();
                monthlyCount++;
            }
        }

        // Format and display
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedTotal = currencyFormat.format(monthlyTotal) + "đ";

        tvMonthlyTotal.setText("Total This Month: " + formattedTotal);
        tvIncomeCount.setText(monthlyCount + " incomes this month | " +
                incomes.size() + " total");
    }

    /**
     * Handle income item click
     */
    @Override
    public void onIncomeClick(Expense income) {
        // Navigate to edit activity
        Intent intent = new Intent(IncomeListActivity.this, EditIncomeActivity.class);
        intent.putExtra("income_id", income.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadIncomes();
    }
}