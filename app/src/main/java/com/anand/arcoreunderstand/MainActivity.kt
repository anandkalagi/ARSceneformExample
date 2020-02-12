package com.anand.arcoreunderstand

import android.R
import android.app.AlertDialog
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import sun.jvm.hotspot.utilities.IntArray
import java.lang.ref.WeakReference
import java.util.function.BiFunction


class MainActivity : AppCompatActivity() {
    private var modelLoader: ModelLoader? = null

    private lateinit var fragment: ArFragment
    private val pointer = PointerDrawable()
    private var isTracking = false
    private var isHitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        fragment = (supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment?)!!

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        fragment?.arSceneView?.scene?.addOnUpdateListener { frameTime: FrameTime? ->
                fragment!!.onUpdate(frameTime)
                onUpdate()
            }
        initializeGallery()
        modelLoader = ModelLoader(WeakReference(this),fragment)
    }

    private fun initializeGallery() {
        val gallery = findViewById<LinearLayout>(R.id.gallery_layout)

        val andy = ImageView(this)
        andy.setImageResource(R.drawable.droid_thumb)
        andy.contentDescription = "andy"
        andy.setOnClickListener { addObject(Uri.parse("andy_dance.sfb")) }
        gallery.addView(andy)

        val cabin = ImageView(this)
        cabin.setImageResource(R.drawable.cabin_thumb)
        cabin.contentDescription = "cabin"
        cabin.setOnClickListener { addObject(Uri.parse("Cabin.sfb")) }
        gallery.addView(cabin)

        val house = ImageView(this)
        house.setImageResource(R.drawable.house_thumb)
        house.contentDescription = "house"
        house.setOnClickListener { addObject(Uri.parse("House.sfb")) }
        gallery.addView(house)

        val igloo = ImageView(this)
        igloo.setImageResource(R.drawable.igloo_thumb)
        igloo.contentDescription = "igloo"
        igloo.setOnClickListener { addObject(Uri.parse("igloo.sfb")) }
        gallery.addView(igloo)

    }


    private fun onUpdate() {
        val trackingChanged = updateTracking()
        val contentView: View = findViewById(R.id.content)
        if (trackingChanged) {
            if (isTracking) {
                contentView.getOverlay().add(pointer)
            } else {
                contentView.getOverlay().remove(pointer)
            }
            contentView.invalidate()
        }
        if (isTracking) {
            val hitTestChanged: Boolean = updateHitTest()
            if (hitTestChanged) {
                pointer.setEnabled(isHitting)
                contentView.invalidate()
            }
        }
    }

    private fun updateTracking(): Boolean {
        val frame: Frame? = fragment!!.arSceneView.arFrame
        val wasTracking = isTracking
        isTracking = frame != null &&
                frame.getCamera().getTrackingState() === TrackingState.TRACKING
        return isTracking != wasTracking
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateHitTest(): Boolean {
        val frame = fragment!!.arSceneView.arFrame
        val pt = getScreenCenter()
        val hits: List<HitResult>
        val wasHitting = isHitting
        isHitting = false
        if (frame != null) {
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && (trackable as Plane).isPoseInPolygon(hit.hitPose)) {
                    isHitting = true
                    break
                }
            }
        }
        return wasHitting != isHitting
    }

    private fun getScreenCenter(): Point {
        val vw = findViewById<View>(android.R.id.content)
        return Point(vw.width / 2, vw.height / 2)
    }
    private fun addObject(model: Uri) {
        val frame = fragment!!.arSceneView.arFrame
        val pt = getScreenCenter()
        val hits: List<HitResult>
        if (frame != null) {
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane &&
                    trackable.isPoseInPolygon(hit.hitPose)
                ) {
                    modelLoader?.loadModel(hit.createAnchor(), model)
                    break
                }
            }
        }
    }

    class ModelLoader (owner: WeakReference<MainActivity?>?,val fragment: ArFragment) {
        private val owner: WeakReference<MainActivity?>?
        fun loadModel(anchor: Anchor?, uri: Uri?) {
            if (owner?.get() == null) {
                Log.d(TAG, "Activity is null.  Cannot load model.")
                return
            }
            ModelRenderable.builder()
                .setSource(owner?.get(), uri)
                .build()
                .handle(
                    BiFunction<ModelRenderable?, Throwable?, Any?> { renderable: ModelRenderable?, throwable: Throwable? ->
                        if (throwable != null) {
                            onException(throwable)
                        } else {
                            addNodeToScene(anchor, renderable,fragment)
                        }
                        null
                    }
                )
            return
        }

        fun addNodeToScene(anchor: Anchor?, renderable: ModelRenderable?,fragment: ArFragment) {
            val anchorNode = AnchorNode(anchor)
            val node = TransformableNode(fragment.getTransformationSystem())
            node.renderable = renderable
            node.setParent(anchorNode)
            fragment.getArSceneView().getScene().addChild(anchorNode)
            node.select()
        }

        fun onException(throwable: Throwable) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(fragment.context)
            builder.setMessage(throwable.message)
                .setTitle("Codelab error!")
            val dialog: AlertDialog = builder.create()
            dialog.show()
            return
        }

        companion object {
            private val TAG: String? = "ModelLoader"
        }

        init {
            this.owner = owner
        }
    }
}
