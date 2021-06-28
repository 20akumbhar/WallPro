@file:Suppress("DEPRECATION")

package com.wallpaper.wallpro.activities

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wallpaper.wallpro.BuildConfig
import com.wallpaper.wallpro.R
import com.wallpaper.wallpro.utils.sdk29AndUp
import com.varunest.sparkbutton.SparkButton
import com.varunest.sparkbutton.SparkEventListener
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


class WallpaperActivity : AppCompatActivity() {
    private lateinit var wallpaperImageView: ImageView
    private lateinit var setWallpaperButton: ImageButton
    private lateinit var downloadWallpaperButton: ImageButton
    private lateinit var favoriteWallpaperButton: SparkButton

    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    private var readPermissionGranted = false
    private var writePermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_wallpaper)
        var isDisabled = true
        wallpaperImageView = findViewById(R.id.wallpaper_mainImageView)
        setWallpaperButton = findViewById(R.id.wallpaper_set_button)
        downloadWallpaperButton = findViewById(R.id.wallapaper_download_button)
        favoriteWallpaperButton = findViewById(R.id.wallpaper_favorite_btn)


        val url = "https://source.unsplash.com/1100x2000/"
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.error)
            .timeout(10000)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    isDisabled = false
                    return true
                }


                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d("resource", "OnResourceReady : loaded")
                    isDisabled = false
                    return false
                }
            })
            .centerCrop()
            .into(wallpaperImageView)

        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                readPermissionGranted =
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
                writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                    ?: writePermissionGranted


            }

        downloadWallpaperButton.setOnClickListener {
            try {
                updateOrRequestPermissions()
                Glide.with(this)
                    .asBitmap()
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            if (minSdk29) {
                                val result = savePhotoToExternalStorage(
                                    "wallpaper" + System.currentTimeMillis().toString(), resource
                                )
                                if (result) {
                                    Toast.makeText(
                                        this@WallpaperActivity,
                                        "Wallpaper Downloaded",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@WallpaperActivity,
                                        "Something went wrong",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                val intent = Intent(Intent.ACTION_VIEW)
                                val uri: Uri? = savewallpaperAndGeturi(
                                    resource,
                                    System.currentTimeMillis().toString()
                                )
                                if (uri != null) {
                                    intent.setDataAndType(uri, "image/*")
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(
                                        this@WallpaperActivity,
                                        "null uri",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        setWallpaperButton.setOnClickListener {
            if (!isDisabled) {
                val bitmapDrawable: BitmapDrawable = wallpaperImageView.drawable as BitmapDrawable
                val bitmap: Bitmap = bitmapDrawable.bitmap
                try {
                    val intent = Intent(Intent.ACTION_ATTACH_DATA)
                    intent.setDataAndType(getUri(bitmap), "image/jpg")
                    intent.putExtra("mimiType", "image/jpg")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivityForResult(Intent.createChooser(intent, "set as"), 200)


                } catch (e: Exception) {
                    Log.e("intent:", e.message!!)
                    Toast.makeText(this, "Something went wrong.", Toast.LENGTH_SHORT).show()
                }

            }
        }

        favoriteWallpaperButton.setEventListener(object : SparkEventListener {
            override fun onEvent(button: ImageView, buttonState: Boolean) {
                if (buttonState) {
                    // Button is active
                } else {
                    // Button is inactive
                }
            }

            override fun onEventAnimationEnd(button: ImageView?, buttonState: Boolean) {
            }

            override fun onEventAnimationStart(button: ImageView?, buttonState: Boolean) {
            }
        })


    }

    private fun savewallpaperAndGeturi(bitmap: Bitmap, id: String): Uri? {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", this@WallpaperActivity.packageName, "this")
        intent.data = uri
        startActivity(intent)

        val folder = File(Environment.getExternalStorageDirectory().toString() + "/WallPro")
        folder.mkdirs()

        val file = File(folder, "$id.jpeg")
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()

            return FileProvider.getUriForFile(
                this@WallpaperActivity,
                BuildConfig.APPLICATION_ID+".provider",
                file
            )
        } catch (e: FileNotFoundException) {
            Toast.makeText(this@WallpaperActivity, "File not found" + e.message, Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        } catch (e: IOException) {
            Toast.makeText(this@WallpaperActivity, "IOException" + e.message, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        return null
    }


    private fun getUri(bmp: Bitmap): Uri? {
        var bmpUri: Uri? = null
        try {
            val file = File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "wallpaper_" + System.currentTimeMillis() + ".jpg"
            )
            val out = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.close()
            bmpUri = FileProvider.getUriForFile(
                this,
                this.applicationContext.packageName + ".provider",
                file
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bmpUri
    }

    private fun updateOrRequestPermissions() {
        val hasWritePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        writePermissionGranted = hasWritePermission || minSdk29

        val permissionsToRequest: Array<String> = if (!writePermissionGranted) {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            arrayOf()
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest)
        }
    }

    private fun savePhotoToExternalStorage(displayName: String, bmp: Bitmap): Boolean {
        val imageCollection = sdk29AndUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bmp.width)
            put(MediaStore.Images.Media.HEIGHT, bmp.height)
        }
        return try {
            contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                        throw IOException("Couldn't save bitmap")
                    }
                }
            } ?: throw IOException("Couldn't create MediaStore entry")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    override fun onStart() {
        super.onStart()
        val user = Firebase.auth.currentUser
        if (user == null) {
            Firebase.auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Firebase.firestore.collection("wallpaper-data")
                            .document("data")
                            .update("clients", FieldValue.increment(1))
                        Log.d("Authentication :", "signInAnonymously:success")
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Authentication :", "signInAnonymously:failure", task.exception)
                    }
                }
        }
    }
}