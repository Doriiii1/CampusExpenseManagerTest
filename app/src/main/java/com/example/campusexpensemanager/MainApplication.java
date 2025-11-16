package com.example.campusexpensemanager;

import android.app.Application;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.campusexpensemanager.workers.RecurringExpenseWorker;

import java.util.concurrent.TimeUnit;

/**
 * Lớp Application tùy chỉnh để khởi tạo các tác vụ nền
 * như RecurringExpenseWorker.
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Lên lịch cho tác vụ kiểm tra chi phí lặp lại
        setupRecurringWork();
    }

    private void setupRecurringWork() {
        // Tạo một yêu cầu chạy định kỳ mỗi 1 ngày
        PeriodicWorkRequest recurringWorkRequest =
                new PeriodicWorkRequest.Builder(RecurringExpenseWorker.class, 1, TimeUnit.DAYS)
                        // Có thể thêm Constraints (ví dụ: chỉ chạy khi có mạng)
                        // .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build();

        // Đăng ký tác vụ với WorkManager, đảm bảo chỉ có 1 tác vụ duy nhất
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "RecurringExpenseJob", // Tên duy nhất cho tác vụ
                ExistingPeriodicWorkPolicy.KEEP, // Giữ lại tác vụ cũ nếu đã tồn tại
                recurringWorkRequest
        );
    }
}