package com.myk.vitrin;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

public class AdsAdapter extends RecyclerView.Adapter<AdsAdapter.AdViewHolder> {

    Context context;
    List<Ad> list;
    DatabaseHelper db;
    Runnable onDeleteListener;

    public AdsAdapter(Context context, List<Ad> list, DatabaseHelper db, Runnable onDeleteListener) {
        this.context = context;
        this.list = list;
        this.db = db;
        this.onDeleteListener = onDeleteListener;
    }

    public void updateList(List<Ad> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AdViewHolder(LayoutInflater.from(context).inflate(R.layout.item_ad, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AdViewHolder holder, int position) {
        Ad ad = list.get(position);

        holder.tvTitle.setText(ad.getTitle());
        holder.tvPrice.setText(formatPrice(ad.getPrice()) + " تومان");

        if (ad.getImageUri() != null && !ad.getImageUri().isEmpty()) {
            try {
                try {
                    context.getContentResolver().takePersistableUriPermission(Uri.parse(ad.getImageUri()), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                holder.imgProduct.setImageURI(Uri.parse(ad.getImageUri()));
            } catch (Exception e) {
                holder.imgProduct.setImageResource(R.drawable.ic_placeholder);
            }
        } else {
            holder.imgProduct.setImageResource(R.drawable.ic_placeholder);
        }

        String catDisplay = ad.getCategory();
        if(ad.getSubcategory() != null && !ad.getSubcategory().isEmpty()) {
            catDisplay += " > " + ad.getSubcategory();
        }
        holder.tvCategory.setText(catDisplay);
        holder.imgCategory.setImageResource(R.drawable.ic_label);

        holder.itemView.setOnClickListener(v -> showBottomSheet(ad));
        holder.itemView.setAnimation(android.view.animation.AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down));
    }

    private void showBottomSheet(Ad ad) {
        BottomSheetDialog sheet = new BottomSheetDialog(context, R.style.FinalBottomSheetDialogTheme);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_details, null);
        sheet.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvDetailTitle);
        TextView tvPrice = view.findViewById(R.id.tvDetailPrice);
        TextView tvCat = view.findViewById(R.id.tvDetailCategory);
        TextView tvDesc = view.findViewById(R.id.tvDetailDesc);
        ImageView imgBig = view.findViewById(R.id.imgDetail);
        Button btnClose = view.findViewById(R.id.btnClose);
        Button btnDelete = view.findViewById(R.id.btnDeleteSheet);
        Button btnEdit = view.findViewById(R.id.btnEditSheet);

        tvTitle.setText(ad.getTitle());
        tvPrice.setText(formatPrice(ad.getPrice()) + " تومان");

        String fullCat = ad.getCategory();
        if (ad.getSubcategory() != null && !ad.getSubcategory().isEmpty()) {
            fullCat += " / " + ad.getSubcategory();
        }
        tvCat.setText(fullCat);

        tvDesc.setText(ad.getDescription());

        if (ad.getImageUri() != null && !ad.getImageUri().isEmpty()) {
            try {
                imgBig.setImageURI(Uri.parse(ad.getImageUri()));
            } catch (Exception e) {
                imgBig.setImageResource(R.drawable.ic_placeholder);
            }
        } else {
            imgBig.setImageResource(R.drawable.ic_placeholder);
        }

        btnClose.setOnClickListener(v -> sheet.dismiss());

        btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("حذف محصول")
                    .setMessage("آیا مطمئن هستید که می‌خواهید این محصول را حذف کنید؟")
                    .setPositiveButton("بله، حذف کن", (dialog, which) -> {
                        if (ad.getImageUri() != null && ad.getImageUri().contains(context.getPackageName())) {
                            deleteImageFile(ad.getImageUri());
                        }

                        db.deleteAd(ad.getId());
                        Toast.makeText(context, "محصول حذف شد", Toast.LENGTH_SHORT).show();
                        sheet.dismiss();

                        if (onDeleteListener != null) {
                            onDeleteListener.run();
                        }
                    })
                    .setNegativeButton("خیر", null)
                    .show();
        });

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddEditActivity.class);
            intent.putExtra("ID", ad.getId());
            intent.putExtra("TITLE", ad.getTitle());
            intent.putExtra("PRICE", ad.getPrice());
            intent.putExtra("DESC", ad.getDescription());
            intent.putExtra("CAT", ad.getCategory());
            intent.putExtra("SUBCAT", ad.getSubcategory());
            intent.putExtra("IMG", ad.getImageUri());
            context.startActivity(intent);
            sheet.dismiss();
        });

        sheet.show();
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

    private String formatPrice(String price) {
        try {
            long p = Long.parseLong(price);
            DecimalFormat formatter = new DecimalFormat("#,###");
            String formatted = formatter.format(p);
            return toPersianDigits(formatted);
        } catch (Exception e) {
            return toPersianDigits(price);
        }
    }

    private String toPersianDigits(String str) {
        return str.replace("0", "۰").replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴").replace("5", "۵").replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹");
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class AdViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPrice, tvCategory;
        ImageView imgProduct, imgCategory;

        public AdViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            imgCategory = itemView.findViewById(R.id.imgCategory);
        }
    }
}