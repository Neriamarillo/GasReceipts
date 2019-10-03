package com.jn769.gasreceipts;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private Button button_gallery;
    private Button deleteButton;
    private Button cancelButton;
    private Button confirmButton;
    private File storageDir;
    private String imageFilePath;
    private File image;
    private TextView processedText;
    private TextView resultTextFromPicture;
    private Uri photoURI;
    private Bitmap imageBitmap;
    private TextRecognizer recognizer;
    private SparseArray<TextBlock> textBlocks = null;
    private ImageView galleryView;
    private String resultLine = null;
    private TextView processedText2;
    private TextView processedText3;
    private TextView processedText4;
    private TextView processedText5;
    private ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        Button button = findViewById(R.id.button);
//        button_gallery = (Button) findViewById(R.id.button_gallery);
        resultTextFromPicture = findViewById(R.id.resultTextView);

        recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "LOADING CAMERA!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                ActivityCompat.requestPermissions(MainActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
            }
        });


    }

    @Override
    public void onRestart() {
        super.onRestart();

    }

    @Override
    public void onStart() {
        super.onStart();
        if (resultLine != null) {
            resultTextFromPicture.setText(resultLine);
        }
    }

    // Request android permission, then take picture if passed
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(LOG_TAG, storageDir.getAbsolutePath());
                Log.i("RequestPermissions: Dir Exists?", String.valueOf(storageDir.exists()));
                if (!storageDir.exists()) {
                    storageDir.mkdirs();
                }
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Options menu (not being used currently)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // // Options menu actions (not being used currently)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Picture intent
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.i("PhotoFile:", String.valueOf(photoFile));
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(
                        this,
                        "com.jn769.gasreceipts.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    // Create image file name
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.i(LOG_TAG, "File name: " + imageFileName);
        assert storageDir != null;
        Log.i("Storage Dir Exits?", String.valueOf(storageDir.exists()));
        image = File.createTempFile(
                imageFileName,      /* prefix */
                ".jpg",      /* suffix */
                storageDir          /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        imageFilePath = image.getAbsolutePath();
        return image;
    }

    // OCR decoding process
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            try {
                setContentView(R.layout.ocr_result);

                processedText = findViewById(R.id.results);
                processedText2 = findViewById(R.id.results2);
                processedText3 = findViewById(R.id.results3);
                processedText4 = findViewById(R.id.results4);
                processedText5 = findViewById(R.id.results5);
                confirmButton = findViewById(R.id.confirm_button);
                button_gallery = findViewById(R.id.button_gallery);
                imageBitmap = decodeBitmapUri(this, photoURI);
                galleryAddPic();

                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setContentView(R.layout.activity_main);
                        recreate();
//                                processedText.setText("Total Amount of scanned receipt: " + resultLine + '\n');
                    }
                });

                button_gallery.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        loadImage();
                    }
                });
                Log.i(LOG_TAG, String.valueOf(recognizer.isOperational()));
                if (recognizer.isOperational() && imageBitmap != null) {
                    Frame frame = new Frame.Builder().setBitmap(imageBitmap).build();
                    textBlocks = recognizer.detect(frame);
                    String blocks = "";
                    String lines = "";
                    String words = "";
                    for (int index = 0; index < textBlocks.size(); index++) {
                        //extract scanned text blocks here
                        TextBlock tBlock = textBlocks.valueAt(index);
                        for (Text line : tBlock.getComponents()) {
                            for (Text element : line.getComponents()) {

                                if (line.getValue().toUpperCase().contains("USD".toUpperCase()) ||
                                        line.getValue().toUpperCase().matches("TOTAL".toUpperCase())) {
//                                    processedText.setText(String.format("Total Amount: %s\n\n", line.getValue()));
                                    Log.i("Line Value for Total Amount",line.getValue());
                                    Log.i("Processed Element: ", element.getValue());
                                    processedText.setText(String.format("Total Amount: $%s", line.getValue()));
                                    resultLine = line.getValue();
                                }
                                if (line.getValue().toUpperCase().contains("VISA".toUpperCase())) {
                                    processedText2.setText(String.format("Paid with: %s\n\n", line.getValue()));
                                    resultLine = line.getValue();
                                }
                                if (line.getValue().toUpperCase().contains("TIP".toUpperCase())) {
                                    processedText3.setText(String.format("Tip: $%s\n\n", line.getValue()));
                                    resultLine = line.getValue();
                                }
                                if (line.getValue().toUpperCase().contains("AMOUNT".toUpperCase())) {
                                    processedText4.setText(String.format("Amount:$@%s\n\n", line.getValue()));
                                    resultLine = line.getValue();
                                }
                                if (line.getValue().toUpperCase().contains("SUBTOTAL".toUpperCase())) {
                                    processedText5.setText(String.format("Subtotal: $%s\n\n", line.getValue()));
                                    resultLine = line.getValue();
                                }
                            }
                        }
                    }
                    if (textBlocks.size() == 0) {
                        processedText.setText(getString(R.string.scan_failed));
                    } else {
//                        confirmButton.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                setContentView(R.layout.activity_main);
//                                recreate();
////                                processedText.setText("Total Amount of scanned receipt: " + resultLine + '\n');
//                            }
//                        });
//
//                        button_gallery.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                loadImage();
//                            }
//                        });
                    }
                } else {
                    processedText.setText(getString(R.string.recognizer_failed));
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT)
                        .show();
                Log.e(LOG_TAG, e.toString());
            }
        }

    }

    // Make image available to external gallery
    private void galleryAddPic() {
        File file = new File(imageFilePath);
        Uri contentUri = Uri.fromFile(file);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    // Decode bitmap for ocr use
    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }

    // Loads current image in a new view
    private void loadImage() {
        setContentView(R.layout.gallery_view);
        Bitmap galleryBitmap = BitmapFactory.decodeFile(imageFilePath);
        ImageView galleryView = findViewById(R.id.imgGalleryImage);
        galleryView.setImageBitmap(galleryBitmap);
        deleteButton = findViewById(R.id.delete_image);
        cancelButton = findViewById(R.id.cancel_button);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteImage();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.activity_main);
                recreate();
            }
        });

    }

    private void deleteImage() {
        File file = new File(imageFilePath);
        file.delete();
        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(imageFilePath))));
        recreate();
    }
}
