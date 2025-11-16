package com.example.campusexpensemanager.utils;

import android.content.Context;

import com.example.campusexpensemanager.models.Currency;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Singleton helper class để quản lý, chuyển đổi, và định dạng tiền tệ.
 */
public class CurrencyHelper {

    private static CurrencyHelper instance;
    private Map<Integer, Currency> currencyMap;
    private DatabaseHelper dbHelper;
    private NumberFormat vndFormat;
    private NumberFormat usdFormat;

    private CurrencyHelper(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
        vndFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        usdFormat = NumberFormat.getCurrencyInstance(Locale.US);
        loadCurrencies();
    }

    public static synchronized CurrencyHelper getInstance(Context context) {
        if (instance == null) {
            instance = new CurrencyHelper(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Tải tất cả tiền tệ từ DB vào một Map để truy cập nhanh
     */
    private void loadCurrencies() {
        currencyMap = new HashMap<>();
        List<Currency> currencies = dbHelper.getAllCurrencies();
        for (Currency currency : currencies) {
            currencyMap.put(currency.getId(), currency);
        }
    }

    /**
     * Lấy một đối tượng Currency bằng ID
     */
    public Currency getCurrencyById(int id) {
        return currencyMap.getOrDefault(id, currencyMap.get(1)); // Mặc định là VND
    }

    /**
     * Chuyển đổi một số tiền từ một loại tiền tệ bất kỳ sang VND
     */
    public double convertToVND(double amount, int currencyId) {
        Currency currency = getCurrencyById(currencyId);
        if (currency == null) {
            return amount; // Không tìm thấy, trả về giá trị gốc
        }
        return amount * currency.getRateToVND();
    }

    /**
     * Định dạng số tiền theo đúng loại tiền tệ của nó (VD: $10.00 hoặc 250.000đ)
     */
    public String formatAmount(double amount, int currencyId) {
        Currency currency = getCurrencyById(currencyId);
        if (currency.getCode().equals("USD")) {
            return usdFormat.format(amount);
        }
        return vndFormat.format(amount);
    }

    /**
     * Định dạng số tiền, hiển thị cả giá trị gốc và giá trị VND (nếu khác VND)
     * VD: $10.00 (250.000đ)
     * HOẶC: 50.000đ
     */
    public String formatAmountWithVND(double originalAmount, int originalCurrencyId) {
        // Nếu là VND, chỉ hiển thị VND
        if (originalCurrencyId == 1) { // 1 là ID của VND
            return vndFormat.format(originalAmount);
        }

        // Nếu là ngoại tệ (USD)
        String originalFormatted = formatAmount(originalAmount, originalCurrencyId);
        String vndFormatted = vndFormat.format(convertToVND(originalAmount, originalCurrencyId));

        return originalFormatted + " (" + vndFormatted + ")";
    }

    /**
     * Lấy danh sách tất cả tiền tệ (dùng cho Spinner)
     */
    public List<Currency> getAllCurrencies() {
        return dbHelper.getAllCurrencies();
    }
}