package com.example.campusexpensemanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusexpensemanager.utils.SessionManager;

import java.util.Locale;

/**
 * BaseActivity xử lý việc chuyển đổi ngôn ngữ tự động.
 * Tất cả các Activity khác phải kế thừa từ class này.
 */
public class BaseActivity extends AppCompatActivity {

    protected SessionManager sessionManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Lấy ngôn ngữ đã lưu từ SessionManager
        sessionManager = new SessionManager(newBase);
        String langCode = sessionManager.getLanguage();
        super.attachBaseContext(updateLocale(newBase, langCode));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo sessionManager một lần nữa cho context của Activity
        sessionManager = new SessionManager(this);
    }

    /**
     * Cập nhật Locale cho Context
     */
    public static Context updateLocale(Context context, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        context = context.createConfigurationContext(config);
        return context;
    }

    /**
     * Tải lại Activity với ngôn ngữ mới
     */
    protected void recreateActivity() {
        recreate();
    }
}