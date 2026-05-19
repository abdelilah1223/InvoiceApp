import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.elbaroudi.invoice.R

class ServicesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.services_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // WhatsApp Button
        view.findViewById<Button>(R.id.whatsappButton).setOnClickListener {
            openWhatsApp()
        }

        // Social Media Buttons
        view.findViewById<ImageButton>(R.id.facebookButton).setOnClickListener {
            openFacebook()
        }

        view.findViewById<ImageButton>(R.id.instagramButton).setOnClickListener {
            openInstagram()
        }

        view.findViewById<ImageButton>(R.id.emailButton).setOnClickListener {
            sendEmail()
        }
    }

    private fun openWhatsApp() {
        try {
            val url = "https://wa.me/+212766981152"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp app is required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFacebook() {
        try {
            val url = "https://www.facebook.com/abdo.high.710896"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Facebook app is required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInstagram() {
        try {
            val url = "https://www.instagram.com/abdohigh?igsh=MWVsY2xnYTlibzZjNg=="
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Instagram app is required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("albaroudiabdelilah92@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Inquiry about ProSolutions services")
            }
            startActivity(Intent.createChooser(intent, "Send email"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No email app installed", Toast.LENGTH_SHORT).show()
        }
    }
}