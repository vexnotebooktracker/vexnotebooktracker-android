package org.vexteams.vexnotebooktracker.ui.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.vexteams.vexnotebooktracker.R
import org.vexteams.vexnotebooktracker.databinding.ActivityAboutBinding
import org.vexteams.vexnotebooktracker.utils.DebugLogger
import java.io.BufferedReader
import java.io.InputStreamReader

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
            setContentView(binding.root)

            setupToolbar()
            loadAboutContent()

            DebugLogger.d("AboutActivity", "📄 About screen initialized")

        } catch (e: Exception) {
            Log.e("AboutActivity", "💥 Error in onCreate", e)
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
            DebugLogger.d("AboutActivity", "⬅️ Back button clicked")
            finish()
        }
    }

    private fun loadAboutContent() {
        try {
            // ✅ Read about.txt from assets folder
            val fileName = getString(R.string.about_content_file)
            val content = readTextFromAssets(fileName)

            if (content.isNotEmpty()) {
                binding.tvAboutContent.text = content
                DebugLogger.d("AboutActivity", "✅ Loaded ${content.length} characters from $fileName")
            } else {
                showErrorContent(getString(R.string.about_content_empty))
            }

        } catch (e: Exception) {
            Log.e("AboutActivity", "❌ Error loading about.txt: ${e.message}", e)
            showErrorContent(getString(R.string.about_unable_to_load, e.message ?: "Unknown error"))
        }
    }

    // ✅ Read text file from assets folder
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

    // ✅ Show error message in TextView
    private fun showErrorContent(message: String) {
        binding.tvAboutContent.text = getString(
            R.string.about_error_title
        ) + "\n\n" + message + "\n\n" + getString(
            R.string.about_error_instructions
        )

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        DebugLogger.d("AboutActivity", "⬅️ System back button pressed")
        super.onBackPressed()
    }
}