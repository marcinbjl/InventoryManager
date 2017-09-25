package marian.inventoryapp;


import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import marian.inventoryapp.data.InventoryContract.InventoryEntry;

import static marian.inventoryapp.R.id.price;

public class InventoryCursorAdapter extends RecyclerView.Adapter<InventoryCursorAdapter.ViewHolder> {

    private static Cursor mProducts;
    private Context mContext;

    public InventoryCursorAdapter(Context context, Cursor products) {
        mContext = context;
        mProducts = products;
    }

    private static int getCursorRowClickedId(int pos) {
        if (mProducts != null) {
            if (mProducts.moveToPosition(pos)) {
                int idColumnIndex = mProducts.getColumnIndex(InventoryEntry._ID);
                return mProducts.getInt(idColumnIndex);
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View productView = inflater.inflate(R.layout.list_item, parent, false);

        return new ViewHolder(productView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        mProducts.moveToPosition(position);

        int nameColumnIndex = mProducts.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME);
        String name = mProducts.getString(nameColumnIndex);

        int quantityColumnIndex = mProducts.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_QUANTITY);
        int quantity = mProducts.getInt(quantityColumnIndex);

        int priceColumnIndex = mProducts.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PRICE);
        long priceInPennies = mProducts.getLong(priceColumnIndex);

        int thumbnailColumnIndex = mProducts.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PICTURE);
        String thumbnailPath = mProducts.getString(thumbnailColumnIndex);

        if (thumbnailPath != null) {
            Uri thumbnailUri = Uri.parse(thumbnailPath);
            viewHolder.thumbnailImageView.setImageURI(thumbnailUri);
        } else {
            viewHolder.thumbnailImageView.setImageResource(R.drawable.placeholder);
        }

        viewHolder.nameTextView.setText(String.valueOf(name));

        NumberFormat nf = NumberFormat.getInstance();
        String numberFormatted = nf.format(quantity);
        String quantityInfo = mContext.getResources().getString(R.string.quantity_format, numberFormatted);
        viewHolder.quantityTextView.setText(quantityInfo);

        BigDecimal price = new BigDecimal(priceInPennies).divide(new BigDecimal(AddProductActivity.BIGDECIMAL_100)).setScale(2, RoundingMode.DOWN);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        String priceFormatted = currencyFormat.format(price.doubleValue());

        String priceInfo = mContext.getResources().getString(R.string.price_format, priceFormatted);
        viewHolder.priceTextView.setText(priceInfo);

        viewHolder.saleButton.setOnClickListener(new View.OnClickListener() {
                                                     @Override
                                                     public void onClick(View v) {
                                                         int adapterPosition = viewHolder.getAdapterPosition();

                                                         int cursorRowId = getCursorRowClickedId(adapterPosition);

                                                         mProducts.moveToPosition(adapterPosition);
                                                         int quantityColumnIndex = mProducts.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_QUANTITY);
                                                         int currentQuantity = mProducts.getInt(quantityColumnIndex);

                                                         int newQuantity;
                                                         if (currentQuantity > 0) {
                                                             newQuantity = currentQuantity - 1;
                                                             ContentValues values = new ContentValues();
                                                             values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, newQuantity);
                                                             Uri currentProductUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, cursorRowId);
                                                             mContext.getContentResolver().update(currentProductUri, values, null, null);

                                                         }
                                                     }
                                                 }
        );
    }

    @Override
    public int getItemCount() {
        if (mProducts != null)
            return mProducts.getCount();
        return 0;
    }

    public void swapCursor(Cursor newCursor) {
        if (newCursor != null) {
            mProducts = newCursor;
            notifyDataSetChanged();

        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView nameTextView;
        public TextView quantityTextView;
        public TextView priceTextView;
        public ImageView thumbnailImageView;
        public Button saleButton;

        public ViewHolder(final View itemView) {
            super(itemView);
            nameTextView = (TextView) itemView.findViewById(R.id.name);
            quantityTextView = (TextView) itemView.findViewById(R.id.quantity);
            priceTextView = (TextView) itemView.findViewById(price);
            thumbnailImageView = (ImageView) itemView.findViewById(R.id.thumbnail);
            saleButton = (Button) itemView.findViewById(R.id.sale_btn);

            itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                int pos = getAdapterPosition();
                                                int cursorRowId = getCursorRowClickedId(pos);

                                                Uri currentProductUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, cursorRowId);

                                                Intent intent = new Intent(itemView.getContext(), AddProductActivity.class);
                                                intent.setData(currentProductUri);
                                                itemView.getContext().startActivity(intent);
                                            }
                                        }
            );
        }
    }
}