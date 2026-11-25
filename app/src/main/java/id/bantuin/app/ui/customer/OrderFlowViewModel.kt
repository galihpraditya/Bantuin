package id.bantuin.app.ui.customer

import androidx.lifecycle.ViewModel

/**
 * ViewModel ringan untuk mempertahankan draft pemesanan across rotation & back stack customer flow.
 */
class OrderFlowViewModel : ViewModel() {
    val flow = OrderFlowManager()
}
