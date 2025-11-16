package com.example.campusexpensemanager.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.CurrencyHelper;
import com.example.campusexpensemanager.utils.DatabaseHelper;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ExpenseAdapter for RecyclerView displaying expense list
 * Features: Category icon, formatted amount, date, truncated description
 */
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private Context context;
    private List<Expense> expenses;
    private List<Expense> expensesFiltered; // For search functionality
    private DatabaseHelper dbHelper;
    private OnExpenseClickListener listener;

    // Currency formatter
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    // **SPRINT 6: Thêm CurrencyHelper**
    private CurrencyHelper currencyHelper;

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public ExpenseAdapter(Context context, List<Expense> expenses, OnExpenseClickListener listener) {
        this.context = context;
        this.expenses = expenses;
        this.expensesFiltered = new ArrayList<>(expenses);
        this.listener = listener;
        this.dbHelper = DatabaseHelper.getInstance(context);

        // **SPRINT 6: Khởi tạo CurrencyHelper**
        this.currencyHelper = CurrencyHelper.getInstance(context);

        // Initialize formatters
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expensesFiltered.get(position);

        // Get category info
        Category category = dbHelper.getCategoryById(expense.getCategoryId());

        if (category != null) {
            holder.tvCategoryName.setText(category.getName());
            // TODO: Set category icon from resources (ic_food, ic_transport, etc.)
            // For now, use default icon
        }

        // **SPRINT 6: Cập nhật Amount formatting**
        String formattedAmount = currencyHelper.formatAmountWithVND(
                expense.getAmount(),
                expense.getCurrencyId()
        );

        // **SPRINT 5: Cập nhật màu sắc và tiền tố cho Amount**
        if (expense.isIncome()) {
            holder.tvAmount.setText("+" + formattedAmount);
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.success)); // Màu xanh
        } else {
            holder.tvAmount.setText("-" + formattedAmount);
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.error)); // Màu đỏ
        }

        // ... (Date, Description) ...
        String formattedDate = dateFormat.format(new Date(expense.getDate()));
        holder.tvDate.setText(formattedDate);
        String description = expense.getDescription();
        if (description != null && !description.isEmpty()) {
            if (description.length() > 50) {
                description = description.substring(0, 47) + "...";
            }
            holder.tvDescription.setText(description);
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // ... (Receipt indicator) ...
        if (expense.getReceiptPath() != null && !expense.getReceiptPath().isEmpty()) {
            holder.ivReceiptIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.ivReceiptIndicator.setVisibility(View.GONE);
        }

        // **SPRINT 5: Cập nhật icon Recurring**
        if (expense.isRecurring()) {
            holder.ivRecurringIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.ivRecurringIndicator.setVisibility(View.GONE);
        }

        // Click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExpenseClick(expense);
            }
        });

        // Add scale animation on click
        holder.cardView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return expensesFiltered.size();
    }

    /**
     * Update expense list and refresh adapter
     */
    public void updateExpenses(List<Expense> newExpenses) {
        this.expenses = newExpenses;
        this.expensesFiltered = new ArrayList<>(newExpenses);
        notifyDataSetChanged();
    }

    /**
     * Filter expenses by description
     */
    public void filter(String query) {
        expensesFiltered.clear();

        if (query == null || query.isEmpty()) {
            expensesFiltered.addAll(expenses);
        } else {
            String lowerCaseQuery = query.toLowerCase();

            for (Expense expense : expenses) {
                // Filter by description or category name
                String description = expense.getDescription();
                Category category = dbHelper.getCategoryById(expense.getCategoryId());

                boolean matchDescription = description != null &&
                        description.toLowerCase().contains(lowerCaseQuery);
                boolean matchCategory = category != null &&
                        category.getName().toLowerCase().contains(lowerCaseQuery);

                if (matchDescription || matchCategory) {
                    expensesFiltered.add(expense);
                }
            }
        }

        notifyDataSetChanged();
    }

    // **SPRINT 5: Hàm mới để lọc theo Type**
    /**
     * Lọc danh sách theo loại transaction
     * @param type 0 = All, 1 = Expenses, 2 = Income
     */
    public void filterByType(int type) {
        expensesFiltered.clear();

        if (type == 0) {
            // Show all
            expensesFiltered.addAll(expenses);
        } else {
            int transactionType = (type == 1) ? Expense.TYPE_EXPENSE : Expense.TYPE_INCOME;
            for (Expense expense : expenses) {
                if (expense.getType() == transactionType) {
                    expensesFiltered.add(expense);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Filter expenses by category
     */
    public void filterByCategory(int categoryId) {
        expensesFiltered.clear();

        if (categoryId == 0) {
            // Show all
            expensesFiltered.addAll(expenses);
        } else {
            for (Expense expense : expenses) {
                if (expense.getCategoryId() == categoryId) {
                    expensesFiltered.add(expense);
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Filter expenses by date range
     */
    public void filterByDateRange(long startDate, long endDate) {
        expensesFiltered.clear();

        for (Expense expense : expenses) {
            long expenseDate = expense.getDate();
            if (expenseDate >= startDate && expenseDate <= endDate) {
                expensesFiltered.add(expense);
            }
        }

        notifyDataSetChanged();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivCategoryIcon;
        TextView tvCategoryName;
        TextView tvAmount;
        TextView tvDate;
        TextView tvDescription;
        ImageView ivReceiptIndicator;
        ImageView ivRecurringIndicator; // **SPRINT 5: Thêm view**

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.card_expense_item);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDescription = itemView.findViewById(R.id.tv_description);
            ivReceiptIndicator = itemView.findViewById(R.id.iv_receipt_indicator);
            ivRecurringIndicator = itemView.findViewById(R.id.iv_recurring_indicator); // **SPRINT 5: Find view**
        }
    }
}