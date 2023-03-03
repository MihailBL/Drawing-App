package com.example.drawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yuku.ambilwarna.AmbilWarnaDialog
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var drawingViewXML: DrawingView? = null
    private var ibBrushSize: ImageButton? = null
    private var imageButtonCurrentPaint: ImageButton? = null
    private var ibColorPickerBtn: ImageButton? = null
    private var colorPickerSelectedColor = 0
    private var customProgressBarDialog: Dialog? = null

    private var checkIfItIsWritingStorage: Boolean = true
    private var savedImageAbsolutePath: String = ""

    /** Create launcher for storage permission */
    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE && !this.checkIfItIsWritingStorage){
                    if (isGranted) {
                        Toast.makeText(this, "Storage Access Granted!", Toast.LENGTH_LONG).show()
                        // Create intent to access the other applications
                        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)
                    }
                    else if (!isGranted)
                        Toast.makeText(this, "Storage Access Denied!", Toast.LENGTH_LONG).show()
                }
                else if (permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE && this.checkIfItIsWritingStorage){
                    if (isGranted)
                        Log.e("StorageAccess", "Saved to Storage: \n${Environment.getExternalStorageDirectory()}/Download")
                }
            }
        }

    /** Create activity result launcher for media storage*/
    val openGalleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()){ result -> // 'result' holds the data(ex.: selectedImages)
        if (result.resultCode == RESULT_OK && result.data != null){
            // TAKE THE 'data' FROM 'result' AND ASSIGNED TO AN IMAGEVIEW
            val imageBackground: ImageView = findViewById(R.id.iv_BackgroundImageFrameLayout)
            // Get URI(Location) of the data
            imageBackground.setImageURI(result.data?.data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        this.drawingViewXML = findViewById(R.id.drawingView)
        this.drawingViewXML?.setSizeForBrush(5.toFloat())


        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_PaintColors)

        this.imageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        // Set black color as default
        this.imageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_selected))

        this.ibBrushSize = findViewById(R.id.ib_BrushSizeChooser)
        this.ibBrushSize?.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        // Color Picker
        this.ibColorPickerBtn = findViewById(R.id.ib_ColorPickerBtn)
        this.ibColorPickerBtn?.setOnClickListener {
            openColorPickerDialogue()
        }

        // Storage request permission
        val ibGallery = findViewById<ImageButton>(R.id.ib_gallery)
        ibGallery.setOnClickListener {
            this.checkIfItIsWritingStorage = false
            requestStoragePermission()
        }

        // Undo Button
        val ibUndo = findViewById<ImageButton>(R.id.ib_undo)
        ibUndo.setOnClickListener {
            this.drawingViewXML?.onClickUndo()
        }

        // Redo Button
        val ibRedo = findViewById<ImageButton>(R.id.ib_redo)
        ibRedo.setOnClickListener {
            this.drawingViewXML?.onClickRedo()
        }

        //Clear Button
        val ibClear = findViewById<ImageButton>(R.id.ib_clear)
        ibClear.setOnClickListener {
            this.drawingViewXML?.onClickClear()
        }

        // Save Button
        val ibSave = findViewById<ImageButton>(R.id.ib_saveImage)
        ibSave.setOnClickListener {
            this.checkIfItIsWritingStorage = true

            if (isStorageAllowed()){
                showCustomProgressBarDialog()

                 lifecycleScope.launch {
                     // Get the frameLayout cuz it contains the background and drawingView
                     val flDrawingViewContainer = findViewById<FrameLayout>(R.id.fl_drawingViewContainer)
                     val bitMapImage = getBitmapFromView(flDrawingViewContainer)
                     saveBitMapFile(bitMapImage)
                 }
            }else
                requestStoragePermission()
        }

        // Share Button
        val ibShare = findViewById<ImageButton>(R.id.ib_share)
        ibShare.setOnClickListener {
            shareImage(this.savedImageAbsolutePath)
        }
    }

    /** Method for showing the brush size dialog(SHOWING LIKE A POP-UP) */
    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)

        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")

        // Set listeners for buttons
        val smallBrushBtn = brushDialog.findViewById<ImageButton>(R.id.ib_SmallBrush)
        smallBrushBtn.setOnClickListener {
            this.drawingViewXML?.setSizeForBrush(5.toFloat())
            brushDialog.dismiss()
        }
        val mediumBrushBtn = brushDialog.findViewById<ImageButton>(R.id.ib_MediumBrush)
        mediumBrushBtn.setOnClickListener {
            this.drawingViewXML?.setSizeForBrush(12.toFloat())
            brushDialog.dismiss()
        }
        val largeBrushBtn = brushDialog.findViewById<ImageButton>(R.id.ib_LargeBrush)
        largeBrushBtn.setOnClickListener {
            this.drawingViewXML?.setSizeForBrush(21.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    /** Method for setting the paint color */
    fun paintColorClicked(view: View){
        if (view != this.imageButtonCurrentPaint){
            val selectedPaintColor = view as ImageButton
            // Get the tag(ex.: @color/blue) as string
            val colorTagString = selectedPaintColor.tag.toString()
            this.drawingViewXML?.setColor(colorTagString)

            // Change the color image drawable to selected
            selectedPaintColor.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_selected))
            this.imageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_normal))
            this.imageButtonCurrentPaint = view
        }
    }

    /** Color picker dialog using implemented third-party library 'yuku' */
    private fun openColorPickerDialogue(){
        val colorPickerDialog = AmbilWarnaDialog(this, this.colorPickerSelectedColor, object : OnAmbilWarnaListener {

            override fun onCancel(dialog: AmbilWarnaDialog?) {
                dialog?.dialog?.dismiss()
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                colorPickerSelectedColor = color
                makeARGBColor()
            }

        })
        colorPickerDialog.show()
    }

    /** method for converting int color to ARGB */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun makeARGBColor(){
        this.drawingViewXML?.setColorFromInt(colorPickerSelectedColor)
    }

    /** Method for requesting storage permissions */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestStoragePermission(){
        // if 'shouldShowRequestPermissionRationale' is true means that request dialog was already opened
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog("Drawing App", "Drawing App needs access to gallery." +
                    "Otherwise you cannot set up background image.")
        }
        else{
            this.requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    /** Custom Dialog for Showing denied request permissions */
    private fun showRationaleDialog(title: String, message: String){
        val customDialog = Dialog(this)
        customDialog.setContentView(R.layout.rationale_permission_custom_dialog)

        customDialog.findViewById<TextView>(R.id.tv_permissionRationaleDialogTitle).text = title
        customDialog.findViewById<TextView>(R.id.tv_permissionRationaleDialogMessage).text = message
        customDialog.findViewById<TextView>(R.id.btn_permissionRationaleDialogCancel).setOnClickListener {
            customDialog.dismiss()
        }
        customDialog.setCancelable(false)
        customDialog.show()
    }

    /** Check If the Read Storage is allowed */
    private fun isStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    /** Method for creating bitmap from the view
     * @param view: Drawing view container. Place where user draws */
    private fun getBitmapFromView(view: View): Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        // bind canvas to the bitmap
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        // draw view on the canvas
        view.draw(canvas)

        return returnedBitmap
    }

    /** Method for saving the bitmap in specified location
     * @param: bitmapImage:  parametar that requires bitmap created from the user drawing container. */
    private suspend fun saveBitMapFile(bitmapImage: Bitmap): String{
        var result = ""
        withContext(Dispatchers.IO){
            if (bitmapImage != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    bitmapImage.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    // Create file path
                    val file = File(Environment.getExternalStorageDirectory().absolutePath + "/Download"
                            + File.separator + "DrawingApp" + System.currentTimeMillis()/1000 + ".png")

                    val fileOutput = FileOutputStream(file)

                    fileOutput.write(bytes.toByteArray())
                    fileOutput.close()

                    result = file.absolutePath

                    runOnUiThread {
                        cancelCustomProgressBarDialog()
                        if (result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,"File saved successfully: $result", Toast.LENGTH_LONG).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity, "Something Went Wrong!", Toast.LENGTH_LONG).show()
                        }
                    }
                }catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        this.savedImageAbsolutePath = result
        return result
    }

    /** Method for sharing an image.
     * @param result:  The path that we return after saving the image on device's storage */
    private fun shareImage(result: String){
        /** PROVIDES A WAY FOR APPS TO PASS NEWLY CREATED/DOWNLOADED MEDIA FILE TO THE MEDIA SCANNER */
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, URI ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, URI)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    /** Custom Progress Bar Dialog */
    private fun showCustomProgressBarDialog(){
        this.customProgressBarDialog = Dialog(this)
        this.customProgressBarDialog?.setContentView(R.layout.custom_progress_bar_dialog)
        this.customProgressBarDialog?.setCancelable(false)
        this.customProgressBarDialog?.show()
    }

    /** Cancel Progress Dialog */
    private fun cancelCustomProgressBarDialog(){
        if (this.customProgressBarDialog != null){
            this.customProgressBarDialog?.dismiss()
            this.customProgressBarDialog = null
        }
    }
}