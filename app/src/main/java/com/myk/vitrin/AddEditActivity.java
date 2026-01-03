package com.myk.vitrin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.List;

public class AddEditActivity extends AppCompatActivity {
    TextInputEditText etTitle, etPrice, etDesc;
    Spinner spinnerCat, spinnerSubCat;
    ImageView imgPreview, btnRemoveImage;
    ImageView btnManageCat, btnAddCat, btnManageSubCat, btnAddSubCat;
    LinearLayout layoutSubCategoryRow;

    DatabaseHelper db;
    int id = -1;
    String selectedImageUri = "";
    String oldImageUri = "";
    String preSelectedCat = "";
    String preSelectedSubCat = "";

    private static final String HINT_CAT = "انتخاب دسته‌بندی...";
    private static final String EMPTY_CAT = "دسته‌بندی ندارید";
    private static final String HINT_SUBCAT = "انتخاب زیر دسته...";
    private static final String NO_SUBCAT = "زیر دسته‌ای ندارید";

    ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri originalUri = result.getData().getData();

                    if (!selectedImageUri.isEmpty() && selectedImageUri.contains(getPackageName())) {
                        deleteImageFile(selectedImageUri);
                    }

                    String savedPath = saveImageToInternalStorage(originalUri);

                    if (savedPath != null) {
                        selectedImageUri = savedPath;
                        imgPreview.setImageURI(Uri.parse(savedPath));
                        imgPreview.setPadding(0, 0, 0, 0);
                        btnRemoveImage.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "خطا در ذخیره عکس", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        initViews();
        db = new DatabaseHelper(this);
        etPrice.addTextChangedListener(new PersianNumberTextWatcher(etPrice));

        setupListeners();

        handleIntent();

        loadCategories();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etPrice = findViewById(R.id.etPrice);
        etDesc = findViewById(R.id.etDesc);
        spinnerCat = findViewById(R.id.spinnerCat);
        spinnerSubCat = findViewById(R.id.spinnerSubCat);
        layoutSubCategoryRow = findViewById(R.id.layoutSubCategoryRow);

        imgPreview = findViewById(R.id.imgPreview);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);

        btnAddCat = findViewById(R.id.btnAddCat);
        btnManageCat = findViewById(R.id.btnManageCat);

        btnAddSubCat = findViewById(R.id.btnAddSubCat);
        btnManageSubCat = findViewById(R.id.btnManageSubCat);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveAd());
    }

    private void setupListeners() {
        imgPreview.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        btnRemoveImage.setOnClickListener(v -> {
            if (!selectedImageUri.isEmpty() && selectedImageUri.contains(getPackageName())) {
                deleteImageFile(selectedImageUri);
            }
            selectedImageUri = "";
            imgPreview.setImageResource(R.drawable.ic_placeholder);
            imgPreview.setPadding(dpToPx(50), dpToPx(50), dpToPx(50), dpToPx(50));
            btnRemoveImage.setVisibility(View.GONE);
        });

        spinnerCat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();

                if (selected.equals(HINT_CAT) || selected.equals(EMPTY_CAT)) {
                    btnManageCat.setVisibility(View.GONE);
                    layoutSubCategoryRow.setVisibility(View.GONE);
                } else {
                    btnManageCat.setVisibility(View.VISIBLE);
                    layoutSubCategoryRow.setVisibility(View.VISIBLE);
                    loadSubCategories(selected);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSubCat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if(selected.equals(HINT_SUBCAT) || selected.equals(NO_SUBCAT)) {
                    btnManageSubCat.setVisibility(View.GONE);
                } else {
                    btnManageSubCat.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAddCat.setOnClickListener(v -> showAddCategoryDialog());
        btnManageCat.setOnClickListener(v -> showManageCategoryDialog());

        btnAddSubCat.setOnClickListener(v -> showAddSubCategoryDialog());
        btnManageSubCat.setOnClickListener(v -> showManageSubCategoryDialog());
    }

    private void handleIntent() {
        Intent i = getIntent();
        if (i.hasExtra("ID")) {
            id = i.getIntExtra("ID", -1);
            etTitle.setText(i.getStringExtra("TITLE"));
            etPrice.setText(i.getStringExtra("PRICE"));
            etDesc.setText(i.getStringExtra("DESC"));
            selectedImageUri = i.getStringExtra("IMG");
            oldImageUri = selectedImageUri;

            preSelectedCat = i.getStringExtra("CAT");
            preSelectedSubCat = i.getStringExtra("SUBCAT");

            if (selectedImageUri != null && !selectedImageUri.isEmpty()) {
                try {
                    imgPreview.setImageURI(Uri.parse(selectedImageUri));
                    imgPreview.setPadding(0, 0, 0, 0);
                    btnRemoveImage.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    imgPreview.setImageResource(R.drawable.ic_placeholder);
                    btnRemoveImage.setVisibility(View.GONE);
                }
            }

            ((Button)findViewById(R.id.btnSave)).setText("بروزرسانی محصول");
            ((TextView)findViewById(R.id.tvHeaderTitle)).setText("ویرایش محصول");
        }
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            File directory = getDir("product_images", MODE_PRIVATE);
            File mypath = new File(directory, "img_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(mypath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            inputStream.close();
            return mypath.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void deleteImageFile(String path) {
        try {
            if (path == null || path.isEmpty()) return;
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                if (text.equals(HINT_CAT) || text.equals(HINT_SUBCAT) ||
                        text.equals(EMPTY_CAT) || text.equals(NO_SUBCAT)) {
                    view.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
                } else {
                    view.setTextColor(android.graphics.Color.parseColor("#444444"));
                }
            }
        };
    }

    private void loadCategories() {
        List<String> cats = db.getCategoryNames();

        if (cats.isEmpty()) {
            cats.add(EMPTY_CAT);
            spinnerCat.setEnabled(false);
        } else {
            cats.add(0, HINT_CAT);
            spinnerCat.setEnabled(true);
        }

        spinnerCat.setAdapter(createCustomAdapter(cats));

        if (!preSelectedCat.isEmpty()) {
            for (int i=0; i<cats.size(); i++) {
                if (cats.get(i).equals(preSelectedCat)) {
                    spinnerCat.setSelection(i);
                    break;
                }
            }
        } else {
            spinnerCat.setSelection(0);
        }
    }

    private void loadSubCategories(String parentCat) {
        List<String> subCats = db.getSubCategoryNames(parentCat);

        if(subCats.isEmpty()) {
            subCats.add(NO_SUBCAT);
            spinnerSubCat.setEnabled(false);
            btnManageSubCat.setVisibility(View.GONE);
        } else {
            subCats.add(0, HINT_SUBCAT);
            spinnerSubCat.setEnabled(true);
        }

        spinnerSubCat.setAdapter(createCustomAdapter(subCats));

        if (!preSelectedSubCat.isEmpty()) {
            for (int i=0; i<subCats.size(); i++) {
                if (subCats.get(i).equals(preSelectedSubCat)) {
                    spinnerSubCat.setSelection(i);
                    break;
                }
            }
            preSelectedSubCat = "";
        } else {
            if (spinnerSubCat.isEnabled()) spinnerSubCat.setSelection(0);
        }
    }

    private FrameLayout createDialogInput(EditText editText) {
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dpToPx(24), dpToPx(8), dpToPx(24), 0);
        editText.setLayoutParams(params);
        container.addView(editText);
        return container;
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("نام دسته‌بندی جدید");

        new MaterialAlertDialogBuilder(this)
                .setTitle("دسته‌بندی جدید")
                .setView(createDialogInput(input))
                .setPositiveButton("افزودن", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        db.addCategory(name);
                        preSelectedCat = name;
                        loadCategories();
                    }
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private void showManageCategoryDialog() {
        String currentCat = spinnerCat.getSelectedItem().toString();
        String[] options = {"ویرایش نام دسته", "حذف دسته"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dialog_option, options);

        new MaterialAlertDialogBuilder(this)
                .setTitle("مدیریت: " + currentCat)
                .setAdapter(adapter, (dialog, which) -> {
                    if (which == 0) showEditCategoryDialog(currentCat);
                    else showDeleteCategoryDialog(currentCat);
                })
                .show();
    }

    private void showEditCategoryDialog(String oldName) {
        EditText input = new EditText(this);
        input.setText(oldName);
        new MaterialAlertDialogBuilder(this)
                .setTitle("ویرایش نام دسته")
                .setView(createDialogInput(input))
                .setPositiveButton("ذخیره", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        db.updateCategory(oldName, newName);
                        preSelectedCat = newName;
                        loadCategories();
                        Toast.makeText(this, "دسته ویرایش شد", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private void showDeleteCategoryDialog(String name) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("حذف دسته")
                .setMessage("آیا مطمئن هستید؟ تمام محصولات و زیر دسته‌های این دسته حذف خواهند شد!")
                .setPositiveButton("بله، حذف کن", (dialog, which) -> {
                    db.deleteCategory(name);
                    preSelectedCat = "";
                    loadCategories();
                    Toast.makeText(this, "دسته حذف شد", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("خیر", null)
                .show();
    }

    private void showAddSubCategoryDialog() {
        if(spinnerCat.getSelectedItem() == null || spinnerCat.getSelectedItem().toString().equals(HINT_CAT) || spinnerCat.getSelectedItem().toString().equals(EMPTY_CAT)) return;
        String parent = spinnerCat.getSelectedItem().toString();

        EditText input = new EditText(this);
        input.setHint("نام زیر دسته جدید");
        new MaterialAlertDialogBuilder(this)
                .setTitle("زیر دسته برای: " + parent)
                .setView(createDialogInput(input))
                .setPositiveButton("افزودن", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        db.addSubCategory(name, parent);
                        preSelectedSubCat = name;
                        loadSubCategories(parent);
                    }
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private void showManageSubCategoryDialog() {
        if(spinnerSubCat.getSelectedItem() == null) return;
        String currentSub = spinnerSubCat.getSelectedItem().toString();
        String currentParent = spinnerCat.getSelectedItem().toString();

        String[] options = {"ویرایش نام زیر دسته", "حذف زیر دسته"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dialog_option, options);

        new MaterialAlertDialogBuilder(this)
                .setTitle("مدیریت: " + currentSub)
                .setAdapter(adapter, (dialog, which) -> {
                    if (which == 0) showEditSubCategoryDialog(currentSub, currentParent);
                    else showDeleteSubCategoryDialog(currentSub, currentParent);
                })
                .show();
    }

    private void showEditSubCategoryDialog(String oldName, String parent) {
        EditText input = new EditText(this);
        input.setText(oldName);
        new MaterialAlertDialogBuilder(this)
                .setTitle("ویرایش نام زیر دسته")
                .setView(createDialogInput(input))
                .setPositiveButton("ذخیره", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        db.updateSubCategory(oldName, newName, parent);
                        preSelectedSubCat = newName;
                        loadSubCategories(parent);
                        Toast.makeText(this, "زیر دسته ویرایش شد", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private void showDeleteSubCategoryDialog(String name, String parent) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("حذف زیر دسته")
                .setMessage("محصولات این زیر دسته هم حذف می‌شوند. ادامه می‌دهید؟")
                .setPositiveButton("بله", (dialog, which) -> {
                    db.deleteSubCategory(name, parent);
                    preSelectedSubCat = "";
                    loadSubCategories(parent);
                    Toast.makeText(this, "زیر دسته حذف شد", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("خیر", null)
                .show();
    }

    private void saveAd() {
        String t = etTitle.getText().toString();
        String rawPrice = etPrice.getText().toString();
        String p = toEnglishDigits(rawPrice).replace(",", "");
        String d = etDesc.getText().toString();

        String cat = "";
        if (spinnerCat.getSelectedItem() != null) cat = spinnerCat.getSelectedItem().toString();

        String subcat = "";
        if (layoutSubCategoryRow.getVisibility() == View.VISIBLE && spinnerSubCat.getSelectedItem() != null && spinnerSubCat.isEnabled()) {
            String selected = spinnerSubCat.getSelectedItem().toString();
            if (!selected.equals(HINT_SUBCAT) && !selected.equals(NO_SUBCAT)) {
                subcat = selected;
            }
        }

        if (t.isEmpty() || p.isEmpty()) {
            Toast.makeText(this, "عنوان و قیمت الزامی است", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cat.equals(HINT_CAT) || cat.equals(EMPTY_CAT) || cat.isEmpty()) {
            Toast.makeText(this, "لطفا یک دسته‌بندی انتخاب کنید (یا بسازید)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (id != -1 && !selectedImageUri.equals(oldImageUri) && oldImageUri.contains(getPackageName())) {
            deleteImageFile(oldImageUri);
        }

        if (id == -1) {
            db.addAd(new Ad(t, p, d, cat, subcat, selectedImageUri));
            Toast.makeText(this, "محصول جدید اضافه شد", Toast.LENGTH_SHORT).show();
        } else {
            db.updateAd(new Ad(id, t, p, d, cat, subcat, selectedImageUri));
            Toast.makeText(this, "محصول بروزرسانی شد", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    public static String toPersianDigits(String str) {
        return str.replace("0", "۰").replace("1", "۱").replace("2", "۲")
                .replace("3", "۳").replace("4", "۴").replace("5", "۵")
                .replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹");
    }

    public static String toEnglishDigits(String str) {
        return str.replace("۰", "0").replace("۱", "1").replace("۲", "2")
                .replace("۳", "3").replace("۴", "4").replace("۵", "5")
                .replace("۶", "6").replace("۷", "7").replace("۸", "8").replace("۹", "9");
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