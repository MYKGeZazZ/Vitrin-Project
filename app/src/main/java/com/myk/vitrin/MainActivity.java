package com.myk.vitrin;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.DecimalFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    RecyclerView recycler;
    DatabaseHelper db;
    AdsAdapter adapter;
    EditText etSearch;
    ImageView btnFilter;
    TextView tvEmpty;
    View mainRootLayout;

    String currentSort = "newest";
    String currentCategory = "همه";
    String currentSubCategory = "همه";
    long minPrice = 0;
    long maxPrice = 999999999;

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable = new Runnable() {
        @Override
        public void run() {
            loadAds();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainRootLayout = findViewById(R.id.mainRootLayout);
        etSearch = findViewById(R.id.etSearch);
        btnFilter = findViewById(R.id.btnFilter);
        recycler = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);

        recycler.setLayoutManager(new GridLayoutManager(this, 2));
        db = new DatabaseHelper(this);

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(this, AddEditActivity.class))
        );

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("خروج")
                        .setMessage("آیا مطمئن هستید که می‌خواهید از برنامه خارج شوید؟")
                        .setPositiveButton("بله", (dialog, which) -> finish())
                        .setNegativeButton("خیر", null)
                        .show();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, 500);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnFilter.setOnClickListener(v -> {
            hideKeyboardAndFocus();
            showFilterDialog();
        });

        setupTouchListeners();
        loadAds();
    }

    private void setupTouchListeners() {
        View.OnTouchListener touchListener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboardAndFocus();
            }
            return false;
        };
        mainRootLayout.setOnTouchListener(touchListener);
        findViewById(R.id.tvHeader).setOnTouchListener(touchListener);
        recycler.setOnTouchListener(touchListener);
    }

    private void hideKeyboardAndFocus() {
        etSearch.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mainRootLayout.getWindowToken(), 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAds();
        if (etSearch != null) etSearch.clearFocus();
    }

    private void loadAds() {
        String query = etSearch.getText().toString();
        List<Ad> ads = db.getFilteredAds(query, currentCategory, currentSubCategory, minPrice, maxPrice, currentSort);

        if (ads.isEmpty()) {
            recycler.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);

            if (!query.isEmpty() || !currentCategory.equals("همه") || minPrice > 0 || maxPrice < 999999999) {
                tvEmpty.setText("محصولی یافت نشد");
            } else {
                tvEmpty.setText("محصولی نداری");
            }
        } else {
            recycler.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }

        if (adapter == null) {
            adapter = new AdsAdapter(this, ads, db, () -> loadAds());
            recycler.setAdapter(adapter);
        } else {
            adapter.updateList(ads);
        }
    }

    private ArrayAdapter<String> createCustomAdapter(List<String> items) {
        return new ArrayAdapter<String>(this, R.layout.item_filter_spinner, items) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                setColorBasedOnExactMatch(view, getItem(position));
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                setColorBasedOnExactMatch(view, getItem(position));
                return view;
            }

            private void setColorBasedOnExactMatch(TextView view, String text) {
                if (text.equals("زیر دسته‌ای ندارد")) {
                    view.setTextColor(Color.parseColor("#9E9E9E"));
                } else {
                    view.setTextColor(Color.parseColor("#444444"));
                }
            }
        };
    }

    private void showFilterDialog() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.FinalBottomSheetDialogTheme);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_filters, null);
        sheet.setContentView(view);

        RadioGroup rgSort = view.findViewById(R.id.rgSort);
        Spinner spinnerCat = view.findViewById(R.id.spinnerFilterCat);
        Spinner spinnerSub = view.findViewById(R.id.spinnerFilterSubCat);
        EditText etMin = view.findViewById(R.id.etMinPrice);
        EditText etMax = view.findViewById(R.id.etMaxPrice);
        Button btnApply = view.findViewById(R.id.btnApply);
        Button btnReset = view.findViewById(R.id.btnReset);

        etMin.addTextChangedListener(new PersianNumberTextWatcher(etMin));
        etMax.addTextChangedListener(new PersianNumberTextWatcher(etMax));

        List<String> cats = db.getCategoryNames();
        cats.add(0, "همه");

        spinnerCat.setAdapter(createCustomAdapter(cats));

        int catPos = -1;
        for(int i=0; i<cats.size(); i++) {
            if(cats.get(i).equals(currentCategory)) {
                catPos = i;
                break;
            }
        }
        if(catPos >= 0) spinnerCat.setSelection(catPos);

        spinnerCat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCat = parent.getItemAtPosition(position).toString();

                if(selectedCat.equals("همه")) {
                    spinnerSub.setVisibility(View.GONE);
                    spinnerSub.setAdapter(null);
                    currentSubCategory = "همه";
                } else {
                    List<String> subCats = db.getSubCategoryNames(selectedCat);
                    spinnerSub.setVisibility(View.VISIBLE);

                    if(subCats.isEmpty()) {
                        spinnerSub.setEnabled(false);
                        List<String> empty = List.of("زیر دسته‌ای ندارد");
                        spinnerSub.setAdapter(createCustomAdapter(empty));
                    } else {
                        spinnerSub.setEnabled(true);
                        subCats.add(0, "همه");
                        spinnerSub.setAdapter(createCustomAdapter(subCats));

                        int subPos = -1;
                        for(int i=0; i<subCats.size(); i++) {
                            if(subCats.get(i).equals(currentSubCategory)) {
                                subPos = i;
                                break;
                            }
                        }
                        if(subPos >= 0) spinnerSub.setSelection(subPos);
                    }
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        if(minPrice > 0) etMin.setText(String.valueOf(minPrice));
        if(maxPrice < 999999999) etMax.setText(String.valueOf(maxPrice));

        if(currentSort.equals("newest")) rgSort.check(R.id.rbNewest);
        else if(currentSort.equals("oldest")) rgSort.check(R.id.rbOldest);
        else if(currentSort.equals("expensive")) rgSort.check(R.id.rbExpensive);
        else if(currentSort.equals("cheap")) rgSort.check(R.id.rbCheap);

        btnApply.setOnClickListener(v -> {
            if(spinnerCat.getSelectedItem() != null)
                currentCategory = spinnerCat.getSelectedItem().toString();

            if(spinnerSub.getVisibility() == View.VISIBLE && spinnerSub.isEnabled() && spinnerSub.getSelectedItem() != null) {
                String selectedSub = spinnerSub.getSelectedItem().toString();
                if(selectedSub.equals("زیر دسته‌ای ندارد")) {
                    currentSubCategory = "همه";
                } else {
                    currentSubCategory = selectedSub;
                }
            } else {
                currentSubCategory = "همه";
            }

            String minStr = toEnglishDigits(etMin.getText().toString()).replace(",", "");
            String maxStr = toEnglishDigits(etMax.getText().toString()).replace(",", "");

            minPrice = minStr.isEmpty() ? 0 : Long.parseLong(minStr);
            maxPrice = maxStr.isEmpty() ? 999999999 : Long.parseLong(maxStr);

            int selectedId = rgSort.getCheckedRadioButtonId();
            if(selectedId == R.id.rbExpensive) currentSort = "expensive";
            else if(selectedId == R.id.rbCheap) currentSort = "cheap";
            else if(selectedId == R.id.rbOldest) currentSort = "oldest";
            else currentSort = "newest";

            loadAds();
            Toast.makeText(this, "فیلتر اعمال شد", Toast.LENGTH_SHORT).show();
            sheet.dismiss();
        });

        btnReset.setOnClickListener(v -> {
            currentCategory = "همه";
            currentSubCategory = "همه";
            minPrice = 0;
            maxPrice = 999999999;
            currentSort = "newest";
            loadAds();
            Toast.makeText(this, "فیلترها پاک شدند", Toast.LENGTH_SHORT).show();
            sheet.dismiss();
        });

        sheet.show();
    }

    public static String toPersianDigits(String str) {
        return str.replace("0", "۰").replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴").replace("5", "۵").replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹");
    }
    public static String toEnglishDigits(String str) {
        return str.replace("۰", "0").replace("۱", "1").replace("۲", "2").replace("۳", "3").replace("۴", "4").replace("۵", "5").replace("۶", "6").replace("۷", "7").replace("۸", "8").replace("۹", "9");
    }
    public class PersianNumberTextWatcher implements TextWatcher {
        private final EditText editText;
        private final DecimalFormat df;
        private boolean isEditing;
        public PersianNumberTextWatcher(EditText editText) {
            this.editText = editText;
            df = new DecimalFormat("#,###");
            isEditing = false;
        }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            if (isEditing) return;
            isEditing = true;
            try {
                String originalString = s.toString();
                String cleanString = toEnglishDigits(originalString).replace(",", "");
                if (!cleanString.isEmpty()) {
                    long parsed = Long.parseLong(cleanString);
                    String formatted = df.format(parsed);
                    String finalString = toPersianDigits(formatted);
                    editText.setText(finalString);
                    editText.setSelection(finalString.length());
                }
            } catch (NumberFormatException e) {}
            isEditing = false;
        }
    }
}