package com.rahulbethi.arcoreapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*

private const val BOTTOM_SHEET_PEEK_HEIGHT = 50f

class MainActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupBottomSheet()
        setupRecyclerView()
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
}
