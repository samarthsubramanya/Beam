package com.beam.app.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreTransaction
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val API_KEY = BillingConfig.ANDROID_API_KEY

private object StoreBilling : Billing, PurchasesDelegate {
    override val supported = true
    private val _isPro = MutableStateFlow(false)
    override val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    override fun start() {
        if (API_KEY.isBlank() || Purchases.isConfigured) return
        Purchases.logLevel = LogLevel.WARN
        Purchases.configure(PurchasesConfiguration(apiKey = API_KEY))
        Purchases.sharedInstance.delegate = this
        Purchases.sharedInstance.getCustomerInfo(onError = {}, onSuccess = ::update)
    }

    override fun restore(onDone: (Boolean) -> Unit) {
        if (!Purchases.isConfigured) return onDone(false)
        Purchases.sharedInstance.restorePurchases(
            onError = { onDone(false) },
            onSuccess = { info -> update(info); onDone(_isPro.value) },
        )
    }

    private fun update(info: CustomerInfo) {
        _isPro.value = info.entitlements[BillingConfig.ENTITLEMENT_ID]?.isActive == true
    }

    override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) = update(customerInfo)

    override fun onPurchasePromoProduct(
        product: StoreProduct,
        startPurchase: (
            onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
            onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
        ) -> Unit,
    ) {}
}

actual fun createBilling(): Billing = StoreBilling

@Composable
actual fun PaywallContent(onDismiss: () -> Unit) {
    if (API_KEY.isBlank()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Purchases aren't set up yet.", textAlign = TextAlign.Center)
            Button(onClick = onDismiss) { Text("Close") }
        }
        return
    }
    Paywall(PaywallOptions(dismissRequest = onDismiss) { shouldDisplayDismissButton = true })
}
