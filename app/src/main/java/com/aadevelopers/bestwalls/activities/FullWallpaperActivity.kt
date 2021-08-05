@file:Suppress("DEPRECATION")

package com.aadevelopers.bestwalls.activities

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
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.aadevelopers.bestwalls.BuildConfig
import com.aadevelopers.bestwalls.R
import com.aadevelopers.bestwalls.utils.currentAdNumber
import com.aadevelopers.bestwalls.utils.sdk29AndUp
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.github.ybq.android.spinkit.SpinKitView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.varunest.sparkbutton.SparkButton
import com.varunest.sparkbutton.SparkEventListener
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class FullWallpaperActivity : AppCompatActivity() {
    private var mInterstitialAd: InterstitialAd? = null
    private var backInterstitialAd: InterstitialAd? = null
    private var downloadDisabled = false
    private val TAG = "AdInterstitial"
    private lateinit var wallpaperImageView: ImageView
    private lateinit var setWallpaperButton: ImageButton
    private lateinit var downloadWallpaperButton: ImageButton
    private lateinit var sourceBtn: ImageButton
    private lateinit var favoriteWallpaperButton: SparkButton
    private var favoriteArray = mutableListOf<String>()
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    private var mRewardedAd: RewardedAd? = null
    lateinit var mAdView: AdView

    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private var imageUrl: String = ""
    private var thumbnail: String = ""
    private var wallpaperId: String = ""
    private var imageSource: String? = ""
    private var isPremium: Boolean = false
    var isDisabled=true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_wallpaper)
        isDisabled = true
        wallpaperImageView = findViewById(R.id.wallpaper_mainImageView)
        setWallpaperButton = findViewById(R.id.wallpaper_set_button)
        downloadWallpaperButton = findViewById(R.id.wallapaper_download_button)
        favoriteWallpaperButton = findViewById(R.id.wallpaper_favorite_btn)
        sourceBtn = findViewById(R.id.source_btn)

        imageUrl = intent.getStringExtra("url")!!
        thumbnail = intent.getStringExtra("thumbnail")!!
        wallpaperId = intent.getStringExtra("wallpaperId")!!
        imageSource = intent.getStringExtra("source")
        isPremium = intent.getBooleanExtra("isPremium", false)
        MobileAds.initialize(this) {}

        mAdView = findViewById(R.id.wallpaper_adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
        findViewById<Button>(R.id.load_rewarded_btn).setOnClickListener {
            loadRewardedAd()
        }
        if (isPremium) {
            loadRewardedAd()
        } else {
            Glide.with(this@FullWallpaperActivity)
                .load(imageUrl)
                .error(R.drawable.error)
                .timeout(10000)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
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
        }



        Firebase.firestore.collection("favoriteArray")
            .document(Firebase.auth.currentUser!!.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    favoriteArray = snapshot["favoriteIds"] as MutableList<String>
                    val index = favoriteArray.indexOf(wallpaperId)
                    if (index > -1) {
                        favoriteWallpaperButton.isChecked = true
                    }
                } else {
                    Log.d("firebase :", "Current data: null")
                }
            }



        loadAds()
        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                readPermissionGranted =
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
                writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                    ?: writePermissionGranted


            }

        sourceBtn.setOnClickListener {
            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.bottom_sheet, null)
            if (imageSource != null && imageSource != "") {
                view.findViewById<TextView>(R.id.bottom_source).text = imageSource
            }
            dialog.setContentView(view)
            dialog.show()
        }

        downloadWallpaperButton.setOnClickListener {
            try {
                if (downloadDisabled) {
                    return@setOnClickListener
                }
                updateOrRequestPermissions()
                downloadDisabled = true

                if (mInterstitialAd != null) {
                    mInterstitialAd?.show(this)
                } else {
                    Log.d(TAG, "The download interstitial ad wasn't ready yet.")
                }

                Glide.with(this)
                    .asBitmap()
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
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
                                downloadDisabled = if (result) {
                                    Toast.makeText(
                                        this@FullWallpaperActivity,
                                        "Wallpaper Downloaded",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    false
                                } else {
                                    Toast.makeText(
                                        this@FullWallpaperActivity,
                                        "Something went wrong",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    false
                                }
                            } else {
                                val intent = Intent(Intent.ACTION_VIEW)
                                val uri: Uri? = saveWallpaperAndGetUri(
                                    resource,
                                    System.currentTimeMillis().toString()
                                )
                                downloadDisabled = if (uri != null) {
                                    intent.setDataAndType(uri, "image/*")
                                    startActivity(intent)
                                    false
                                } else {
                                    Toast.makeText(
                                        this@FullWallpaperActivity,
                                        "null uri",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    false
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
                if (mInterstitialAd != null) {
                    mInterstitialAd?.show(this)
                } else {
                    Log.d(TAG, "The interstitial ad wasn't ready yet.")
                }
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
                if (!buttonState) {
                    var wallId = ""
                    Firebase.firestore.collection("favorites")
                        .whereEqualTo("userId", Firebase.auth.currentUser!!.uid)
                        .whereEqualTo("wallpaperId", wallpaperId)
                        .get()
                        .addOnSuccessListener {
                            for (dc in it) {
                                wallId = dc.id
                            }
                            Firebase.firestore.collection("favorites")
                                .document(wallId)
                                .delete()
                            Firebase.firestore.collection("favoriteArray")
                                .document(Firebase.auth.currentUser!!.uid)
                                .update("favoriteIds", FieldValue.arrayRemove(wallpaperId))
                        }

                        .addOnFailureListener {
                            favoriteWallpaperButton.isChecked = true
                            Toast.makeText(
                                this@FullWallpaperActivity,
                                "Not removed from favorite",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                } else {
                    val wallpaper = hashMapOf(
                        "image" to imageUrl,
                        "thumbnail" to thumbnail,
                        "userId" to (Firebase.auth.currentUser?.uid ?: ""),
                        "wallpaperId" to wallpaperId,
                        "timestamp" to Timestamp(Date())
                    )
                    Firebase.firestore.collection("favorites")
                        .add(
                            wallpaper
                        ).addOnSuccessListener { documentReference ->
                            Firebase.firestore.collection("favoriteArray")
                                .document(Firebase.auth.currentUser!!.uid)
                                .update("favoriteIds", FieldValue.arrayUnion(wallpaperId))
                            Log.d(
                                "firebase :",
                                "DocumentSnapshot written with ID: ${documentReference.id}"
                            )
                        }
                        .addOnFailureListener {
                            favoriteWallpaperButton.isChecked = false
                            Toast.makeText(
                                this@FullWallpaperActivity,
                                "Not added to favorite",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                }
            }

            override fun onEventAnimationEnd(button: ImageView?, buttonState: Boolean) {
            }

            override fun onEventAnimationStart(button: ImageView?, buttonState: Boolean) {
            }
        })


    }

    private fun loadRewardedAd() {
        val loader = findViewById<SpinKitView>(R.id.loader)
        val rewardedLayout: RelativeLayout = findViewById<RelativeLayout>(R.id.rewarded_layout)
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            this,
            getString(R.string.Rewarded_Ad_Id),
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    Toast.makeText(
                        this@FullWallpaperActivity,
                        "video not loaded",
                        Toast.LENGTH_SHORT
                    ).show()
                    mRewardedAd = null
                    finish()
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d(TAG, "Ad was loaded.")
                    loader.visibility = View.GONE
                    rewardedLayout.visibility = View.VISIBLE
                    mRewardedAd = rewardedAd
                    if (mRewardedAd != null) {
                        mRewardedAd?.show(
                            this@FullWallpaperActivity
                        ) {
                            Log.d(TAG, "User earned the reward.")
                            rewardedLayout.visibility=View.GONE
                            loader.visibility=View.VISIBLE
                            Glide.with(this@FullWallpaperActivity)
                                .load(imageUrl)
                                .error(R.drawable.error)
                                .timeout(10000)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
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
                        }


                    } else {
                        Log.d(TAG, "The rewarded ad wasn't ready yet.")
                    }
                }


            })

        mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad was shown.")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                // Called when ad fails to show.
                Log.d(TAG, "Ad failed to show.")
                Glide.with(this@FullWallpaperActivity)
                    .load(imageUrl)
                    .error(R.drawable.error)
                    .timeout(10000)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
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

            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, "Ad was dismissed.")
                Toast.makeText(this@FullWallpaperActivity, "video closed", Toast.LENGTH_SHORT)
                    .show()
                mRewardedAd = null
                finish()
            }
        }


    }

    private fun saveWallpaperAndGetUri(bitmap: Bitmap, id: String): Uri? {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", this@FullWallpaperActivity.packageName, "this")
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
                this@FullWallpaperActivity,
                BuildConfig.APPLICATION_ID + ".provider",
                file
            )
        } catch (e: FileNotFoundException) {
            Toast.makeText(
                this@FullWallpaperActivity,
                "File not found" + e.message,
                Toast.LENGTH_SHORT
            )
                .show()
            e.printStackTrace()
        } catch (e: IOException) {
            Toast.makeText(
                this@FullWallpaperActivity,
                "IOException" + e.message,
                Toast.LENGTH_SHORT
            ).show()
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
                        Firebase.firestore.collection("favoriteArray")
                            .document((Firebase.auth.currentUser?.uid.toString()))
                            .set(hashMapOf("favoriteIds" to mutableListOf<String>()))
                        Log.d("Authentication :", "signInAnonymously:success")
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Authentication :", "signInAnonymously:failure", task.exception)
                    }
                }
        }
    }

    private fun loadAds() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            getString(R.string.Inter_Ad_Id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    loadAds()
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                }
            })
        InterstitialAd.load(
            this,
            getString(R.string.Back_Inter_Ad_Id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    loadAds()
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    backInterstitialAd = interstitialAd
                }
            })


        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed.")
                loadAds()

            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                Log.d(TAG, "Ad failed to show.")

            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
            }

        }
        backInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed.")
                loadAds()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                Log.d(TAG, "Ad failed to show.")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
            }

        }

    }

    override fun onBackPressed() {

        if (backInterstitialAd != null) {
            Log.e(TAG, "" + currentAdNumber)
            currentAdNumber++
            if (currentAdNumber % 3 == 0)
                backInterstitialAd?.show(this)
            else
                super.onBackPressed()

        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
            finish()
        }
    }
}