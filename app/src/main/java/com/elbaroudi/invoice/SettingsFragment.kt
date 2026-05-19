package com.elbaroudi.invoice

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView



class SettingsFragment : Fragment() {

    private lateinit var rootView: View
    private lateinit var titleText: TextView
    private lateinit var colorSectionTitle: TextView
    private lateinit var languageSectionTitle: TextView
    private lateinit var colorsRecyclerView: RecyclerView
    private lateinit var languageSwitch: FrameLayout
    private lateinit var languageButton: View
    private lateinit var arabicLabel: TextView
    private lateinit var englishLabel: TextView

    private var selectedColor = "#FFFFFF"
    private var language = "ar"
    private val colors = listOf(
        ColorItem("أزرق", "#4A90E2"),
        ColorItem("أخضر", "#50C878"),
        ColorItem("أحمر", "#FF6B6B"),
        ColorItem("بنفسجي", "#9B59B6"),
        ColorItem("ذهبي", "#FFD700"),
        ColorItem("أبيض", "#FFFFFF", "#333333", "#DDDDDD"),
        ColorItem("أسود", "#000000", "#FFFFFF"),
        ColorItem("تركواز", "#1ABC9C"),
        ColorItem("برتقالي", "#FFA500"),
        ColorItem("زهري", "#FF9FF3")
    )


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize views
        titleText = rootView.findViewById(R.id.titleText)
        colorSectionTitle = rootView.findViewById(R.id.colorSectionTitle)
        languageSectionTitle = rootView.findViewById(R.id.languageSectionTitle)
        colorsRecyclerView = rootView.findViewById(R.id.colorsRecyclerView)
        languageSwitch = rootView.findViewById(R.id.languageSwitch)
        languageButton = rootView.findViewById(R.id.languageButton)
        arabicLabel = rootView.findViewById(R.id.arabicLabel)
        englishLabel = rootView.findViewById(R.id.englishLabel)

        // Setup RecyclerView
        colorsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        colorsRecyclerView.adapter = ColorAdapter(colors, ::onColorSelected)

        // Load saved settings
        loadSettings()

        // Setup language switch
        updateLanguageUI()
        languageSwitch.setOnClickListener {
            toggleLanguage()
        }

        return rootView
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadSettings() {
        val sharedPref = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        selectedColor = sharedPref?.getString("bgColor", "#FFFFFF") ?: "#FFFFFF"
        language = sharedPref?.getString("language", "ar") ?: "ar"

        // Update UI with loaded settings
        updateBackgroundColor()
        updateLanguageUI()
    }

    private fun saveColor(color: String) {
        selectedColor = color
        requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit()?.apply {
            putString("bgColor", color)
            apply()
        }
        updateBackgroundColor()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toggleLanguage() {
        language = if (language == "ar") "en" else "ar"
        requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)?.edit()?.apply {
            putString("language", language)
            apply()
        }
        updateLanguageUI()

        // Here you would typically restart the activity or update the app locale
        // For simplicity, we're just updating the UI in this example
    }

    private fun updateBackgroundColor() {
        rootView.setBackgroundColor(Color.parseColor(selectedColor))

        // Update text colors based on background
        val textColor = getContrastColor(selectedColor)
        titleText.setTextColor(Color.parseColor(textColor))
        colorSectionTitle.setTextColor(Color.parseColor(textColor))
        languageSectionTitle.setTextColor(Color.parseColor(textColor))
        arabicLabel.setTextColor(Color.parseColor(textColor))
        englishLabel.setTextColor(Color.parseColor(textColor))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateLanguageUI() {
        // Animate language button position
        languageSwitch.post {
            val translationX = if (language == "ar") 0f else languageSwitch.width - languageButton.width - resources.getDimension(R.dimen.language_button_margin) * 2
            languageButton.animate().translationX(translationX).setDuration(300).start()
        }

        // Update label styles
        arabicLabel.alpha = if (language == "ar") 1f else 0.7f
        arabicLabel.typeface = if (language == "ar") resources.getFont(R.font.cairo_bold) else resources.getFont(R.font.cairo_medium)
        englishLabel.alpha = if (language == "en") 1f else 0.7f
        englishLabel.typeface = if (language == "en") resources.getFont(R.font.cairo_bold) else resources.getFont(R.font.cairo_medium)

        // Update text based on language
        titleText.text = if (language == "ar") "الإعدادات" else "Settings"
        colorSectionTitle.text = if (language == "ar") "لون الخلفية" else "Background Color"
        languageSectionTitle.text = if (language == "ar") "اللغة" else "Language"
    }

    private fun onColorSelected(color: String) {
        saveColor(color)
    }

    private fun getContrastColor(hexColor: String): String {
        val hex = hexColor.replace("#", "")
        val r = hex.substring(0, 2).toInt(16)
        val g = hex.substring(2, 4).toInt(16)
        val b = hex.substring(4, 6).toInt(16)
        val brightness = (r * 299 + g * 587 + b * 114) / 1000
        return if (brightness > 128) "#000000" else "#FFFFFF"
    }

    inner class ColorAdapter(
        private val colors: List<ColorItem>,
        private val onColorSelected: (String) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_color, parent, false)
            return ColorViewHolder(view)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            holder.bind(colors[position])
        }

        override fun getItemCount() = colors.size

        inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val colorCard: FrameLayout = itemView.findViewById(R.id.colorCard)
            private val checkIcon: View = itemView.findViewById(R.id.checkIcon)

            fun bind(colorItem: ColorItem) {
                // Create a new drawable for each color
                val drawable = ContextCompat.getDrawable(itemView.context, R.drawable.color_card_background)?.mutate()

                if (drawable is GradientDrawable) {
                    try {
                        // Set the background color
                        drawable.setColor(Color.parseColor(colorItem.code))

                        // Handle white color with border
                        if (colorItem.code == "#FFFFFF") {
                            val borderColor = colorItem.borderColor ?: "#DDDDDD"
                            drawable.setStroke(4, Color.parseColor(borderColor))
                        } else {
                            drawable.setStroke(0, Color.TRANSPARENT)
                        }
                    } catch (e: IllegalArgumentException) {
                        // Handle invalid color format
                        drawable.setColor(Color.WHITE)
                        drawable.setStroke(2, Color.GRAY)
                    }
                }

                colorCard.background = drawable

                checkIcon.visibility = if (selectedColor == colorItem.code) View.VISIBLE else View.GONE

                itemView.setOnClickListener {
                    onColorSelected(colorItem.code)
                }
            }
        }
    }

    data class ColorItem(
        val name: String,
        val code: String,
        val textColor: String? = null,
        val borderColor: String? = null
    )
}