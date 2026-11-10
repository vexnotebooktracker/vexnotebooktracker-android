package com.bodepudi.vexnotebooktracker.ui.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import com.bodepudi.vexnotebooktracker.R
import com.bodepudi.vexnotebooktracker.databinding.ActivityAboutBinding
import com.bodepudi.vexnotebooktracker.utils.DebugLogger
import java.io.BufferedReader
import java.io.InputStreamReader
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.bodepudi.vexnotebooktracker.utils.WindowInsetsUtils
import androidx.activity.OnBackPressedCallback

@AndroidEntryPoint
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AboutActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityAboutBinding.inflate(layoutInflater)
            // Android 16 (API 36) enforces edge-to-edge; a dark system bar style keeps the^M
            // white status bar icons this app has always used over its orange app bar.^M
            enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
            setContentView(binding.root)

            WindowInsetsUtils.applyTopBarInsets(binding.appBarLayout)
            WindowInsetsUtils.applyContentInsets(binding.contentContainer)
            setupBackPressHandler()

            setupToolbar()
            loadAboutContent()

            DebugLogger.d("AboutActivity", " About screen initialized")

        } catch (e: Exception) {
            Log.e("AboutActivity", " Error in onCreate", e)
            Toast.makeText(
                this,
                getString(R.string.error_loading_about, e.message ?: "Unknown error"),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            DebugLogger.d("AboutActivity", " Back button clicked")
            finish()
        }
    }

    private fun loadAboutContent() {
        try {
            // Read about.txt from assets folder
            val fileName = getString(R.string.about_content_file)
            val content = readTextFromAssets(fileName)

            if (content.isNotEmpty()) {
                binding.tvAboutContent.text = content
                DebugLogger.d("AboutActivity", " Loaded ${content.length} characters from $fileName")
            } else {
                showErrorContent(getString(R.string.about_content_empty))
            }

        } catch (e: Exception) {
            Log.e("AboutActivity", " Error loading about.txt: ${e.message}", e)
            showErrorContent(getString(R.string.about_unable_to_load, e.message ?: "Unknown error"))
        }
    }

    // Read text file from assets folder
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
            Log.e("AboutActivity", "Error reading $fileName from assets", e)
            throw e
        }
    }

    // Show error message in TextView
    private fun showErrorContent(message: String) {
        binding.tvAboutContent.text = getString(R.string.about_error_title) + "\n\n" + message + "\n\n" + getString(R.string.about_error_instructions)

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Predictive back is enabled by default for apps targeting SDK 36, and the system then
     * dispatches through OnBackPressedDispatcher instead of calling onBackPressed().
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                DebugLogger.d("AboutActivity", "⬅ System back button pressed")
                finish()
            }
        })
    }
}
