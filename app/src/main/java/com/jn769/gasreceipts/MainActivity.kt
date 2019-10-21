package com.jn769.gasreceipts

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.vision.text.TextBlock
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    var button_gallery: Button? = null
    var deleteButton: Button? = null
    var cancelButton: Button? = null
    var confirmButton: Button? = null
    var storageDir: File? = null
    var imageFilePath: String? = null
    var image: File? = null
    var processedText: TextView? = null
    var resultTextFromPicture: TextView? = null
    var photoURI: Uri? = null
    var imageBitmap: Bitmap? = null
    //    private TextRecognizer recognizer;
    val textBlocks: SparseArray<TextBlock>? = null
    val galleryView: ImageView? = null
    val resultLine: String? = null
    var processedText2: TextView? = null
    var processedText3: TextView? = null
    var processedText4: TextView? = null
    var processedText5: TextView? = null
    val imageView: ImageView? = null

    internal var recognizer: FirebaseVisionTextRecognizer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val button = findViewById<Button>(R.id.button)
        //        button_gallery = (Button) findViewById(R.id.button_gallery);
        resultTextFromPicture = findViewById(R.id.resultTextView)

        //        recognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();

        //        recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        button.setOnClickListener { view ->
            Snackbar.make(view, "LOADING CAMERA!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_PERMISSION)
        }


    }

    public override fun onRestart() {
        super.onRestart()

    }

    public override fun onStart() {
        super.onStart()

    }

    // Request android permission, then take picture if passed
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(LOG_TAG, storageDir!!.absolutePath)
                Log.i("RequestPermissions: ", storageDir!!.exists().toString())
                if (!storageDir!!.exists()) {
                    storageDir!!.mkdirs()
                }
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(this@MainActivity, "Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Options menu (not being used currently)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // // Options menu actions (not being used currently)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)

    }

    // Picture intent
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            //            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
                Log.i("PhotoFile:", photoFile.toString())
            } catch (ex: IOException) {
                // Error occurred while creating the File
                ex.printStackTrace()
            }

            // Continue only if the File was successfully created

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(
                        this,
                        "com.jn769.gasreceipts.fileprovider",
                        photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }


    // Create image file name
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        //        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.i(LOG_TAG, "File name: $imageFileName")
        assert(storageDir != null)
        Log.i("Storage Dir Exits?", storageDir!!.exists().toString())
        image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir          /* directory */
        )
        // Save a file: path for use with ACTION_VIEW intents
        imageFilePath = image!!.absolutePath
        return image as File
    }

    // MLKit
    private fun runTextRecognition(imageBitmap: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(imageBitmap)
        val recognizer = FirebaseVision.getInstance()
                .onDeviceTextRecognizer
        recognizer.processImage(image)
                .addOnSuccessListener { texts -> processTextRecognitionResult(texts) }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    //                                mTextButton.setEnabled(true);
                    e.printStackTrace()
                }
    }

    private fun processTextRecognitionResult(texts: FirebaseVisionText) {
        val floatList = ArrayList<Float>()
        val blocks = texts.textBlocks
        if (blocks.size == 0) {
            showToast("No text found")
            return
        }
        //        mGraphicOverlay.clear();
        showToast(blocks.size.toString())
        for (i in blocks.indices) {
            val lines = blocks[i].lines
            //            processedText.setText(blocks.get(i).getText());
            Log.i(LOG_TAG, "processTextRecognitionResult: " + blocks[i].text + "Block No: " + i)
            //            Log.i("LINES FLOAT: ", blocks.get(i).getText() + " at i " + i + " and k: " + i);

            Log.i("LINES SIZE: ", lines.size.toString())
            //            if (blocks.get(i).getText().contains("$")) {


            for (k in lines.indices) {

                try {
                    // TODO: Takes the string and parses it to a float. Then adds it to a list.
                    //                        if (lines.get(k).getText().contains("$")) {
                    val floatNum: Float = (lines[k].text
                            .substring(lines[k].text.lastIndexOf('$') + 1)).toFloat()

                    //                    Log.i("FLOAT $$: ", blocks.get(i).getText() + " at i " + i + " and k: " + k);
                    //                    Log.i("LINES FLOAT: ", lines.get(k).getText() + " at i " + i + " and k: " + k);

                    floatList.add(floatNum)
                    Log.i("FLOAT: ", floatNum.toString())


                } catch (ignored: NumberFormatException) {

                }

            }
            //                break;
            //            }

            //            if (blocks.get(i).getText().toUpperCase().contains("FUEL SALE".toUpperCase())) {
            ////                processedText2.setText(lines.get(i).getText());
            //                for (int j = 0; j < lines.size(); j++) {
            //                    if (lines.get(j).getText().toUpperCase().contains("FUEL SALE".toUpperCase())) {
            ////                        Log.i("LINE J", lines.get(j).getText());
            //                        processedText2.setText(lines.get(j).getText());
            //                    }
            //                }
            //            }
            //            for (int j = 0; j < lines.size(); j++) {
            //                if (lines.get(j).getText().toUpperCase().contains("FUEL SALE".toUpperCase())) {
            //                    Log.i("LINE J", lines.get(j).getText());
            ////                    processedText2.setText(lines.get(j).getText());
            //                } else {
            ////                    processedText.setText("NOTHING FOUND WOMP WOMP");
            //                }
            //                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
            ////                float floatNum = 0.0f;
            ////                for (int k = 0; k < elements.size(); k++) {
            ////                    try {
            ////                        if (elements.get(k).getText().contains("$")) {
            ////                            floatNum = Float.parseFloat(elements.get(k).getText());
            ////                        }
            ////                    } catch (NumberFormatException nfe) {
            ////                        i++;
            ////                    }
            ////                    floatList.add(floatNum);
            ////                    Log.i("FLOAT: ", String.valueOf(floatNum));
            //
            ////                    Log.i("LINE J", lines.get(j).getText());
            ////                    Log.i("ELEMENT K", elements.get(k).getText());
            ////                    if (lines.get(k).getText().toUpperCase().equals("FUEL SALE")) {
            //////                        processedText3.setText(lines.get(k).getText());
            ////                    }
            //
            //////                    processedText.setText(elements.get(k).getText());
            //////                    Graphic textGraphic = new TextGraphic(mGraphicOverlay, elements.get(k));
            //////                    mGraphicOverlay.add(textGraphic);
            ////
            ////                }
            //            }

            //            for (int j = 0; j < lines.size(); j++) {
            //                if (lines.get(j).getText().toUpperCase().contains("FUEL SALE".toUpperCase())) {
            //                    Log.i("LINE J", lines.get(j).getText());
            ////                    processedText2.setText(lines.get(j).getText());
            //                } else {
            ////                    processedText.setText("NOTHING FOUND WOMP WOMP");
            //                }
            ////                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
            ////                for (int k = 0; k < elements.size(); k++) {
            //////                    Log.i("LINE J", lines.get(j).getText());
            //////                    Log.i("ELEMENT K", elements.get(k).getText());
            ////                    if (lines.get(k).getText().toUpperCase().equals("FUEL SALE")) {
            ////                        processedText3.setText(lines.get(k).getText());
            ////                    }
            ////
            ////////                    processedText.setText(elements.get(k).getText());
            ////////                    Graphic textGraphic = new TextGraphic(mGraphicOverlay, elements.get(k));
            ////////                    mGraphicOverlay.add(textGraphic);
            //////
            ////                }
            //            }
        }

        processedText?.text = String.format("TOTAL: $%s", findLargestFloat(floatList))


    }

    // TODO: Takes a float list and finds the largest float value (the total price).

    private fun findLargestFloat(input: ArrayList<Float>): Float {
        input.sort()
        return if (input.size == 0) {
            0.0f
        } else input[input.size - 1]

    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            setContentView(R.layout.ocr_result)
            processedText = findViewById(R.id.results)
            processedText2 = findViewById(R.id.results2)
            processedText3 = findViewById(R.id.results3)
            processedText4 = findViewById(R.id.results4)
            processedText5 = findViewById(R.id.results5)
            confirmButton = findViewById(R.id.confirm_button)
            button_gallery = findViewById(R.id.button_gallery)

            imageBitmap = rotateBitmapOrientation(imageFilePath)
            if (imageBitmap != null) {
                runTextRecognition(imageBitmap!!)
            }
            confirmButton!!.setOnClickListener {
                setContentView(R.layout.activity_main)
                recreate()
                //                                processedText.setText("Total Amount of scanned receipt: " + resultLine + '\n');
            }

            button_gallery!!.setOnClickListener { loadImage() }

        }

    }

    // Make image available to external gallery
    private fun galleryAddPic() {
        val file = File(imageFilePath!!)
        val contentUri = Uri.fromFile(file)
//        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri)
        val mediaScanIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_SEARCH, contentUri)
        mediaScanIntent.data = contentUri
        this.sendBroadcast(mediaScanIntent)
    }

    // Decode bitmap for ocr use
    @Throws(FileNotFoundException::class)
    private fun decodeBitmapUri(ctx: Context, uri: Uri): Bitmap? {
        val targetW = 600
        val targetH = 600
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeStream(ctx.contentResolver.openInputStream(uri), null, bmOptions)
        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight

        val scaleFactor = min(photoW / targetW, photoH / targetH)
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor

        return BitmapFactory.decodeStream(ctx.contentResolver
                .openInputStream(uri), null, bmOptions)
    }

    // Loads current image in a new view
    private fun loadImage() {
        setContentView(R.layout.gallery_view)
        //        Bitmap galleryBitmap = BitmapFactory.decodeFile(imageFilePath);
        val galleryBitmap = rotateBitmapOrientation(imageFilePath)
        val galleryView = findViewById<ImageView>(R.id.imgGalleryImage)
        galleryView.setImageBitmap(galleryBitmap)
        deleteButton = findViewById(R.id.delete_image)
        cancelButton = findViewById(R.id.cancel_button)

        deleteButton!!.setOnClickListener { deleteImage() }

        cancelButton!!.setOnClickListener {
            setContentView(R.layout.ocr_result)
            recreate()
        }

    }


    private fun deleteImage() {
        val file = File(imageFilePath!!)
        file.delete()
        this.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(imageFilePath!!))))
        recreate()
    }

    companion object {

        private const val LOG_TAG = "MainActivity"
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_WRITE_PERMISSION = 20
        const val PICK_PHOTO_CODE = 1046


        private fun rotateBitmapOrientation(photoFilePath: String?): Bitmap {
            // Create and configure BitmapFactory
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            BitmapFactory.decodeFile(photoFilePath, bounds)
            val opts = BitmapFactory.Options()
            val bm = BitmapFactory.decodeFile(photoFilePath, opts)
            // Read EXIF Data
            var exif: ExifInterface? = null
            try {
                exif = ExifInterface(photoFilePath!!)
                Log.i("EXIF", "Exif: $exif")
            } catch (e: IOException) {
                e.printStackTrace()
            }

            assert(exif != null)
            val orientString = exif!!.getAttribute(ExifInterface.TAG_ORIENTATION)
            val orientation = if (orientString != null) Integer.parseInt(orientString) else ExifInterface.ORIENTATION_NORMAL
            Log.i("BITMAP ROTATION", "rotateBitmapOrientation: $orientation")
            var rotationAngle = 0
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270
            // Rotate Bitmap
            val matrix = Matrix()
            matrix.setRotate(rotationAngle.toFloat(), bm.width.toFloat() / 2, bm.height.toFloat() / 2)
            // Return result
            return Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true)
        }
    }
}
