package com.example.campusexpensemanager.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.activities.MainActivity;
import com.example.campusexpensemanager.models.Category;
import com.example.campusexpensemanager.models.Expense;
import com.example.campusexpensemanager.utils.DatabaseHelper;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecurringExpenseWorker extends Worker {

    private static final String TAG = "RecurringExpenseWorker";
    private static final String CHANNEL_ID = "RecurringExpenseChannel";
    private final DatabaseHelper dbHelper;
    private final Context context;

    public RecurringExpenseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "RecurringExpenseWorker: Tác vụ bắt đầu...");
        try {
            // 1. Lấy danh sách chi phí lặp lại đã đến hạn
            List<Expense> dueExpenses = dbHelper.getDueRecurringExpenses();
            Log.d(TAG, "Tìm thấy " + dueExpenses.size() + " chi phí lặp lại đến hạn.");

            for (Expense originalExpense : dueExpenses) {
                // 2. Clone chi phí gốc
                Expense newExpense = new Expense(
                        originalExpense.getUserId(),
                        originalExpense.getCategoryId(),
                        originalExpense.getCurrencyId(),
                        originalExpense.getAmount(),
                        System.currentTimeMillis(), // Ngày hiện tại
                        originalExpense.getDescription(),
                        null, // Không sao chép biên lai
                        System.currentTimeMillis(),
                        originalExpense.getType()
                );
                // Đảm bảo chi phí mới không phải là chi phí lặp lại
                newExpense.setRecurring(false);

                // 3. Thêm chi phí mới vào DB
                long newExpenseId = dbHelper.insertExpense(newExpense);

                if (newExpenseId != -1) {
                    Log.d(TAG, "Đã thêm chi phí mới: " + newExpenseId);

                    // 4. Cập nhật ngày lặp lại tiếp theo cho chi phí gốc
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(originalExpense.getNextOccurrenceDate());

                    switch (originalExpense.getRecurrencePeriod()) {
                        case "Weekly":
                            c.add(Calendar.DAY_OF_YEAR, 7);
                            break;
                        case "Monthly":
                            c.add(Calendar.MONTH, 1);
                            break;
                        case "Yearly":
                            c.add(Calendar.YEAR, 1);
                            break;
                    }
                    originalExpense.setNextOccurrenceDate(c.getTimeInMillis());
                    dbHelper.updateExpense(originalExpense);

                    // 5. Gửi thông báo
                    sendNotification(newExpense);
                }
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi trong RecurringExpenseWorker", e);
            return Result.failure();
        }
    }

    private void sendNotification(Expense expense) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Tạo Channel cho Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Recurring Expenses";
            String description = "Thông báo khi chi phí lặp lại được tự động thêm";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }

        // Lấy tên Category
        Category category = dbHelper.getCategoryById(expense.getCategoryId());
        String categoryName = (category != null) ? category.getName() : "Unknown";

        // Định dạng tiền tệ
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        String amount = currencyFormat.format(expense.getAmount()) + "đ";

        // Intent để mở app
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_wallet) // Bạn đã có icon này
                .setContentTitle("Chi phí lặp lại đã được thêm")
                .setContentText(categoryName + ": " + amount)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify((int) expense.getId(), builder.build());
    }
}