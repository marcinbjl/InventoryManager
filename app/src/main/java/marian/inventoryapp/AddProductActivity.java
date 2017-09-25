package marian.inventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import marian.inventoryapp.data.InventoryContract.InventoryEntry;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

public class AddProductActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final int BIGDECIMAL_100 = 100;
    private static final String LOG_TAG = AddProductActivity.class.getSimpleName();
    private static final int GALLERY_REQUEST_CODE = 11;
    private static final int CAMERA_REQUEST_CODE = 12;
    private static final int DESIRED_THUMBNAIL_WIDTH_DP = 100;
    private static final int JPEG_COMPRESSION_QUALITY = 100;
    private static final int EXISTING_PRODUCT_LOADER = 0;
    private static final int DECIMAL_DIGITS_2 = 2;
    private static final int DECIMAL_DIGITS_0 = 0;
    private static final int IN_SAMPLE_SIZE = 4;
    Uri mCurrentProductUri;
    String mThumbnailPath;
    Uri mPictureUri;
    private ImageView mImageView;
    private EditText mNameEditText;
    private EditText mQuantityEditText;
    private EditText mPriceEditText;
    private EditText mEmailEditText;
    private EditText mModifyQuantityEditText;
    private boolean mProductHasChanged = false;

    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle outState) {
        super.onCreate(outState);
        setContentView(R.layout.activity_add_product);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        if (mCurrentProductUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_product));
            View editProductLayoutBottom = findViewById(R.id.edit_product_bottom);
            editProductLayoutBottom.setVisibility(View.GONE);

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            invalidateOptionsMenu();

        } else {
            setTitle(getString(R.string.editor_activity_title_edit_product));

            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }


        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mEmailEditText = (EditText) findViewById(R.id.edit_product_supplier);
        mModifyQuantityEditText = (EditText) findViewById(R.id.modify_product_quantity);
        mModifyQuantityEditText.setText("1");
        mImageView = (ImageView) findViewById(R.id.thumbnail);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showThumbnailDialog();

            }
        });

        Button plusBtn = (Button) findViewById(R.id.quantity_plus);
        plusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modifyQuantity("+");
            }
        });

        Button minusBtn = (Button) findViewById(R.id.quantity_minus);
        minusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modifyQuantity("-");
            }
        });

        Button orderBtn = (Button) findViewById(R.id.order_button);
        orderBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String emailString = mEmailEditText.getText().toString().trim();

                if (!TextUtils.isEmpty(emailString)) {
                    orderMore();
                } else {
                    Toast.makeText(AddProductActivity.this, getString(R.string.empty_product_email), Toast.LENGTH_SHORT).show();

                }
            }
        });

        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mEmailEditText.setOnTouchListener(mTouchListener);
        mModifyQuantityEditText.setOnTouchListener(mTouchListener);
        plusBtn.setOnTouchListener(mTouchListener);
        mImageView.setOnTouchListener(mTouchListener);
        minusBtn.setOnTouchListener(mTouchListener);

        invalidateOptionsMenu();
    }

    private void saveProduct() {

        if (TextUtils.isEmpty(mThumbnailPath)) {
            Toast.makeText(this, getString(R.string.empty_product_image),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String nameString = mNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String emailString = mEmailEditText.getText().toString().trim();


        if (TextUtils.isEmpty(nameString)) {
            Toast.makeText(this, getString(R.string.empty_product_name),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(quantityString)) {
            Toast.makeText(this, getString(R.string.empty_product_quantity),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(priceString)) {
            Toast.makeText(this, getString(R.string.empty_product_price),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(InventoryEntry.COLUMN_PRODUCT_SUPPLIER, emailString);


        int quantity = Integer.parseInt((quantityString));

        BigDecimal inputPrice = new BigDecimal(priceString).setScale(DECIMAL_DIGITS_2, RoundingMode.DOWN);
        BigDecimal pennies = inputPrice.multiply(new BigDecimal(BIGDECIMAL_100)).setScale(DECIMAL_DIGITS_0, BigDecimal.ROUND_DOWN);
        long price = pennies.longValueExact();

        values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, quantity);
        values.put(InventoryEntry.COLUMN_PRODUCT_PRICE, price);
        values.put(InventoryEntry.COLUMN_PRODUCT_PICTURE, mThumbnailPath);


        if (mCurrentProductUri == null) {
            // This is a NEW PRODUCT, so insert a new PRODUCT into the provider,
            Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
            }

        } else {
            // Otherwise this is an EXISTING product
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

            if (rowsAffected == 0) {

                Toast.makeText(this, getString(R.string.editor_update_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {

                Toast.makeText(this, getString(R.string.editor_update_product_successful),
                        Toast.LENGTH_SHORT).show();
            }

        }
        finish();
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveProduct();
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(AddProductActivity.this);
                    return true;
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(AddProductActivity.this);
                            }
                        };

                showUnsavedChangesDialog(discardButtonClickListener);

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_PRODUCT_NAME,
                InventoryEntry.COLUMN_PRODUCT_QUANTITY,
                InventoryEntry.COLUMN_PRODUCT_PRICE,
                InventoryEntry.COLUMN_PRODUCT_SUPPLIER,
                InventoryEntry.COLUMN_PRODUCT_PICTURE
        };

        return new CursorLoader(this,
                mCurrentProductUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {

            int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PRICE);
            int supplierColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_SUPPLIER);
            int thumbnailColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PICTURE);

            String name = cursor.getString(nameColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);

            long priceInPennies = cursor.getLong(priceColumnIndex);

            BigDecimal price = new BigDecimal(priceInPennies).divide(new BigDecimal(BIGDECIMAL_100)).setScale(2, RoundingMode.DOWN);

            String supplier = cursor.getString(supplierColumnIndex);

            mThumbnailPath = cursor.getString(thumbnailColumnIndex);
            if (mThumbnailPath != null) {
                Uri thumbnailUri = Uri.parse(mThumbnailPath);
                mImageView.setImageURI(thumbnailUri);
            }

            mNameEditText.setText(name);
            mEmailEditText.setText(supplier);
            mQuantityEditText.setText(Integer.toString(quantity));
            mPriceEditText.setText(price.toString());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        mNameEditText.setText("");
        mEmailEditText.setText("");
        mQuantityEditText.setText("");
        mPriceEditText.setText("");

    }

    public void pickFromGalleryIntent() {
        Intent pickPhotoIntent = new Intent();
        pickPhotoIntent.setType("image/*");
        pickPhotoIntent.setAction(Intent.ACTION_GET_CONTENT);

        if (pickPhotoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(pickPhotoIntent, "Select a Picture"), GALLERY_REQUEST_CODE);
        }
    }

    public void takePhotoIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();

                Log.v(LOG_TAG, "Error occurred while creating the File");
            }
            if (photoFile != null) {
                mPictureUri = FileProvider.getUriForFile(this,
                        "marian.inventoryapp.fileprovider",
                        photoFile);

                //grant uri permission to camera packages
                List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    this.grantUriPermission(packageName, mPictureUri, FLAG_GRANT_WRITE_URI_PERMISSION | FLAG_GRANT_READ_URI_PERMISSION);
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY_REQUEST_CODE)
                onSelectFromGalleryResult(data);
            else if (requestCode == CAMERA_REQUEST_CODE)
                downsampleAndSave();
        }
    }

    private void onSelectFromGalleryResult(Intent data) {
        mPictureUri = data.getData();
        try {
            createImageFile();
            downsampleAndSave();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downsampleAndSave() {

        try {

            InputStream inputStreamBounds = getContentResolver().openInputStream(mPictureUri);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStreamBounds, null, options);

            options.inScaled = true;
            options.inSampleSize = IN_SAMPLE_SIZE;
            options.inDensity = options.outWidth;
            options.inTargetDensity = convertDpToPx(DESIRED_THUMBNAIL_WIDTH_DP) * options.inSampleSize;
            options.inJustDecodeBounds = false;

            InputStream inputStream = getContentResolver().openInputStream(mPictureUri);

            Bitmap thumbnail = BitmapFactory.decodeStream(inputStream, null, options);

            FileOutputStream fOut = new FileOutputStream(mThumbnailPath);
            thumbnail.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, fOut);
            fOut.close();

            mImageView.setImageBitmap(thumbnail);

        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "inventory" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mThumbnailPath = image.getAbsolutePath();

        return image;
    }

    private void showDeleteConfirmationDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteProduct() {
        if (mCurrentProductUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    private void modifyQuantity(String operator) {
        String currentQuantity = mQuantityEditText.getText().toString().trim();
        String quantityDifference = mModifyQuantityEditText.getText().toString().trim();

        if (TextUtils.isEmpty(currentQuantity)) {
            currentQuantity = "0";
        }

        if (TextUtils.isEmpty((quantityDifference))) {
            quantityDifference = "0";

        }

        int currentQuantityInt = Integer.parseInt((currentQuantity));
        int quantityDifferenceInt = Integer.parseInt(quantityDifference);

        int newQuantity = 0;
        if (operator.equals("+")) {
            newQuantity = currentQuantityInt + quantityDifferenceInt;
        } else {
            if (quantityDifferenceInt <= currentQuantityInt) {
                newQuantity = currentQuantityInt - quantityDifferenceInt;
            }
        }

        String updatedQuantity = String.valueOf(newQuantity);
        mQuantityEditText.setText(updatedQuantity);
    }

    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void orderMore() {

        String email[] = {mEmailEditText.getText().toString().trim()};
        String subject = "Product order - " + mNameEditText.getText().toString().trim();

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, email);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void showThumbnailDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(AddProductActivity.this);
        builder.setTitle(R.string.editor_choose_action)
                .setItems(R.array.thumbnail_actions, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            takePhotoIntent();
                        } else {
                            pickFromGalleryIntent();
                        }

                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private int convertDpToPx(int desiredWidthDp) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return (int) (desiredWidthDp * metrics.density);
    }

}




