package com.bodepudi.vexnotebooktracker.ui.simplyfoursteps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import com.bodepudi.vexnotebooktracker.R
import com.bodepudi.vexnotebooktracker.databinding.ActivitySimplyFourStepsBinding
import com.bodepudi.vexnotebooktracker.utils.DebugLogger
import java.io.BufferedReader
import java.io.InputStreamReader
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.bodepudi.vexnotebooktracker.utils.WindowInsetsUtils
import androidx.activity.OnBackPressedCallback

@AndroidEntryPoint
class SimplyFourStepsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimplyFourStepsBinding

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SimplyFourStepsActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivitySimplyFourStepsBinding.inflate(layoutInflater)
            // Android 16 (API 36) enforces edge-to-edge; a dark system bar style keeps the
            // white status bar icons this app has always used over its orange app bar.
            enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
            setContentView(binding.root)

            WindowInsetsUtils.applyTopBarInsets(binding.appBarLayout)
            WindowInsetsUtils.applyContentInsets(binding.contentContainer)
            setupBackPressHandler()

            setupToolbar()
            loadSimplyFourStepsContent()

            DebugLogger.d("SimplyFourStepsActivity", "Simply Four Steps screen initialized")

        } catch (e: Exception) {
            Log.e("SimplyFourStepsActivity", "Error in onCreate", e)
            Toast.makeText(
                this,
                getString(R.string.error_loading_simply_four_steps, e.message ?: "Unknown error"),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            DebugLogger.d("SimplyFourStepsActivity", "Back button clicked")
            finish()
        }
    }

    private fun loadSimplyFourStepsContent() {
        try {
            val fileName = getString(R.string.simply_four_steps_content_file)
            val content = readTextFromAssets(fileName)

            if (content.isNotEmpty()) {
                binding.tvSimplyFourStepsContent.text = content
                DebugLogger.d("SimplyFourStepsActivity", "Loaded ${content.length} characters from $fileName")
            } else {
                showErrorContent(getString(R.string.simply_four_steps_content_empty))
            }

        } catch (e: Exception) {
            Log.e("SimplyFourStepsActivity", "Error loading simply_four_steps.txt: ${e.message}", e)
            showErrorContent(getString(R.string.simply_four_steps_unable_to_load, e.message ?: "Unknown error"))
        }
    }

    private fun readTextFromAssets(fileName: String): String {
        return try {
            val inputStream = assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }

            reader.close()
            inputStream.close()

            stringBuilder.toString()

        } catch (e: Exception) {
            Log.e("SimplyFourStepsActivity", "Error reading $fileName from assets", e)
            throw e
        }
    }

    private fun showErrorContent(message: String) {
        binding.tvSimplyFourStepsContent.text = getString(
            R.string.simply_four_steps_error_title
        ) + "\n\n" + message + "\n\n" + getString(
            R.string.simply_four_steps_error_instructions
        )

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Predictive back is enabled by default for apps targeting SDK 36, and the system then
     * dispatches through OnBackPressedDispatcher instead of calling onBackPressed().
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                DebugLogger.d("SimplyFourStepsActivity", "System back button pressed")
                finish()
            }
        })
    }
}