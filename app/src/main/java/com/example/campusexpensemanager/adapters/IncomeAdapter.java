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
import com.example.campusexpensemanager.utils.DatabaseHelper;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * IncomeAdapter for RecyclerView displaying income list
 * Similar to ExpenseAdapter but with green color scheme
 */
public class IncomeAdapter extends RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder> {

    private Context context;
    private List<Expense> incomes;
    private List<Expense> incomesFiltered;
    private DatabaseHelper dbHelper;
    private OnIncomeClickListener listener;

    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    public interface OnIncomeClickListener {
        void onIncomeClick(Expense income);
    }

    public IncomeAdapter(Context context, List<Expense> incomes, OnIncomeClickListener listener) {
        this.context = context;
        this.incomes = incomes;
        this.incomesFiltered = new ArrayList<>(incomes);
        this.listener = listener;
        this.dbHelper = DatabaseHelper.getInstance(context);

        this.currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public IncomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_income, parent, false);
        return new IncomeViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull IncomeViewHolder holder, int position) {
        Expense income = incomesFiltered.get(position);

        // Get category info
        Category category = dbHelper.getCategoryById(income.getCategoryId());

        if (category != null) {
            holder.tvCategoryName.setText(category.getName());
        }

        // Format amount with GREEN color for income
        String formattedAmount = currencyFormat.format(income.getAmount()) + "đ";
        holder.tvAmount.setText(formattedAmount);
        holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.success)); // Green

        // Format date
        String formattedDate = dateFormat.format(new Date(income.getDate()));
        holder.tvDate.setText(formattedDate);

        // Truncate description (max 50 chars)
        String description = income.getDescription();
        if (description != null && !description.isEmpty()) {
            if (description.length() > 50) {
                description = description.substring(0, 47) + "...";
            }
            holder.tvDescription.setText(description);
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // Click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIncomeClick(income);
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
        return incomesFiltered.size();
    }

    /**
     * Filter incomes by description
     */
    public void filter(String query) {
        incomesFiltered.clear();

        if (query == null || query.isEmpty()) {
            incomesFiltered.addAll(incomes);
        } else {
            String lowerCaseQuery = query.toLowerCase();

            for (Expense income : incomes) {
                String description = income.getDescription();
                Category category = dbHelper.getCategoryById(income.getCategoryId());

                boolean matchDescription = description != null &&
                        description.toLowerCase().contains(lowerCaseQuery);
                boolean matchCategory = category != null &&
                        category.getName().toLowerCase().contains(lowerCaseQuery);

                if (matchDescription || matchCategory) {
                    incomesFiltered.add(income);
                }
            }
        }

        notifyDataSetChanged();
    }

    static class IncomeViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivCategoryIcon;
        TextView tvCategoryName;
        TextView tvAmount;
        TextView tvDate;
        TextView tvDescription;

        public IncomeViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.card_income_item);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDescription = itemView.findViewById(R.id.tv_description);
        }
    }
}