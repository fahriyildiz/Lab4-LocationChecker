package com.zybooks.locationchecker;

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.contract.ActivityResultContracts.TakePicture

class MainActivity : AppCompatActivity() {
    private var photoFile: File? = null
    private lateinit var profilePicture: ImageView
    private lateinit var locationTextView: TextView
    private lateinit var checkLocationButton: Button
    private lateinit var locationManager: LocationManager
    private var mediaPlayer: MediaPlayer? = null

    private val locationPermissionCode = 1
    private val cameraPermissionCode = 2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        profilePicture = findViewById(R.id.profilePicture)
        locationTextView = findViewById(R.id.locationTextView)
        checkLocationButton = findViewById(R.id.checkLocationButton)

        checkLocationButton.setOnClickListener {
            playSound()
            fetchLocation()
        }

        findViewById<Button>(R.id.uploadButton).setOnClickListener { playSound()
            takePhotoClick() }

    }

    private fun takePhotoClick() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionCode
            )
        } else {
            // Permission already granted
            launchCamera()
        }
    }

    private fun launchCamera() {
        // Create the File for saving the photo
        photoFile = createImageFile()

        // Create a content URI to grant camera app write permission to mPhotoFile
        val photoUri = FileProvider.getUriForFile(
            this,
            "com.zybooks.locationchecker.fileprovider", photoFile!!
        )

        // Start camera app
        takePicture.launch(photoUri)
    }

    private val takePicture = registerForActivityResult(
        TakePicture()
    ) { success ->
        if (success) {
            displayPhoto()
        }
    }

    private fun createImageFile(): File {

        // Create a unique image filename
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFilename = "photo_$timeStamp.jpg"

        // Get file path where the app can save a private image
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(storageDir, imageFilename)
    }

    private fun displayPhoto() {
        // Get ImageView dimensions
        val targetWidth = profilePicture.width
        val targetHeight = profilePicture.height

        // Get bitmap dimensions
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(photoFile!!.absolutePath, bmOptions)
        val photoWidth = bmOptions.outWidth
        val photoHeight = bmOptions.outHeight

        // Determine how much to scale down the image
        val scaleFactor = Math.min(photoWidth / targetWidth, photoHeight / targetHeight)

        // Decode the image file into a smaller bitmap that fills the ImageView
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor
        val bitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath, bmOptions)

        // Display smaller bitmap
        profilePicture.setImageBitmap(bitmap)
    }


    private fun playSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.sound)
        mediaPlayer?.start()

        // Optional: Set a completion listener to release resources when the sound finishes playing
        mediaPlayer?.setOnCompletionListener {
            stopSound()
        }
    }

    private fun stopSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }


    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        } else {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0f,
                locationListener
            )
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            showLocationDialog(location.latitude, location.longitude)
            locationManager.removeUpdates(this)
        }

        override fun onStatusChanged(provider: String?, status:            Int, extras: Bundle?) {
        }

        override fun onProviderEnabled(provider: String) {
        }

        override fun onProviderDisabled(provider: String) {
        }
    }

    private fun showLocationDialog(latitude: Double, longitude: Double) {
        val message = getString(R.string.location_message, latitude, longitude)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.location_dialog_title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        alertDialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationButton.isEnabled = true
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        else if (requestCode == cameraPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }



}
