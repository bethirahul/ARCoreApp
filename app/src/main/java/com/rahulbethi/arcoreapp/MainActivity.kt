package com.rahulbethi.arcoreapp

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

private const val BOTTOM_SHEET_PEEK_HEIGHT = 50f
private const val DOUBLE_TAP_MAX_DURATION = 1000L // 1 sec

class MainActivity : AppCompatActivity() {

    lateinit var arFragment: ArFragment

    var count = 0

    private val models = mutableListOf(
        Model(R.drawable.chair,      "Chair",   R.raw.chair     ),
        Model(R.drawable.desk,       "Desk",    R.raw.desk      ),
        Model(R.drawable.garbagebin, "Bin",     R.raw.garbagebin),
        Model(R.drawable.houseplant, "Plant",   R.raw.houseplant),
        Model(R.drawable.lamp,       "Lamp",    R.raw.lamp      ),
        Model(R.drawable.laptop,     "Laptop",  R.raw.laptop    ),
        Model(R.drawable.printer,    "Printer", R.raw.printer   ),
        Model(R.drawable.speaker,    "Speaker", R.raw.speaker   )
    )

    private lateinit var selectedModel: Model

    val viewNodes = mutableListOf<Node>()

    private lateinit var photoSaver: PhotoSaver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        arFragment = fragment as ArFragment
        setupBottomSheet()
        setupRecyclerView()
        setupDoubleTapArPlaneListener()
        setupFab()

        photoSaver = PhotoSaver(this)

        getCurrentScene().addOnUpdateListener {
            rotateViewNodesTowardsUser()
        }
    }

    private fun setupFab() {
        fab.setOnClickListener {
            photoSaver.takePhoto(arFragment.arSceneView)
        }
    }

    private fun setupDoubleTapArPlaneListener() {
        var firstTapTime = 0L
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            if(firstTapTime == 0L) {
                firstTapTime = System.currentTimeMillis()
            } else if(System.currentTimeMillis() - firstTapTime < DOUBLE_TAP_MAX_DURATION) {
                firstTapTime = 0L
                loadModel { modelRenderable, viewRenderable ->
                    addNodeToScene(hitResult.createAnchor(), modelRenderable, viewRenderable)
                }
            } else {
                firstTapTime = System.currentTimeMillis()
            }
        }
    }

    private fun setupRecyclerView() {
        rvModels.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvModels.adapter = ModelAdapter(models).apply {
            selectedModel.observe(this@MainActivity, Observer {
                this@MainActivity.selectedModel = it
                val newTitle = "AR objects (${it.title})"
                tvModel.text = newTitle
            })
        }
    }

    private fun setupBottomSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, BOTTOM_SHEET_PEEK_HEIGHT, resources.displayMetrics).toInt()
        bottomSheetBehavior.addBottomSheetCallback(object :
        BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomSheet.bringToFront()
            }
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
        })
    }

    private fun getCurrentScene() = arFragment.arSceneView.scene

    private fun createDeleteButton(): Button {
        return Button(this).apply {
            text = "DELETE"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
        }
    }

    private fun rotateViewNodesTowardsUser() {
        for(node in viewNodes) {
            node.renderable?.let {
                val camPos = getCurrentScene().camera.worldPosition
                val viewNodePos = node.worldPosition
                val dir = Vector3.subtract(camPos, viewNodePos)
                node.worldRotation = Quaternion.lookRotation(dir, Vector3.up())
            }
        }
    }

    private fun addNodeToScene(anchor: Anchor, modelRenderable: ModelRenderable, viewRenderable: ViewRenderable) {
        val anchorNode = AnchorNode(anchor)
        val modelNode = TransformableNode(arFragment.transformationSystem).apply {
            renderable = modelRenderable
            localScale = Vector3(0.5f, 0.5f, 0.5f)
            name = count.toString()
            count += 1
            setParent(anchorNode)
            getCurrentScene().addChild(anchorNode)
            select()
        }
        val viewNode = Node().apply {
            renderable = null
            setParent(modelNode)
            val box = modelNode.renderable?.collisionShape as Box
            localPosition = Vector3(0f, box.size.y, 0f)
            (viewRenderable.view as Button).setOnClickListener {
                getCurrentScene().removeChild(anchorNode)
                viewNodes.remove(this)
            }
        }
        viewNodes.add(viewNode)
        modelNode.setOnTapListener { hitTestResult, _ ->
            if(!modelNode.isTransforming) {
                if(viewNode.renderable == null) {
                    viewNode.renderable = viewRenderable
                    val name = hitTestResult.node?.name
                    val message = "Opened $name Remove button"
                    Toast.makeText(
                        this, message,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewNode.renderable = null
                }
            }
        }
    }

    private fun loadModel(callback: (ModelRenderable, ViewRenderable) -> Unit) {
        val modelRenderable = ModelRenderable.builder()
            .setSource(this, selectedModel.modelResourceId).build()

        val viewRenderable = ViewRenderable.builder()
            .setView(this, createDeleteButton()).build()

        CompletableFuture.allOf(modelRenderable, viewRenderable).thenAccept {
            callback(modelRenderable.get(), viewRenderable.get())
        }.exceptionally {
            Toast.makeText(this, "Error in loading model: $it", Toast.LENGTH_LONG).show()
            null
        }
    }
}
