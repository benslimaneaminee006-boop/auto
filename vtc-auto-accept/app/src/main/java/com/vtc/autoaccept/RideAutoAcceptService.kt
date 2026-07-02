package com.vtc.autoaccept

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern

data class RideInfo(
    val price: Double,
    val pickupDistanceKm: Double,
    val rideDistanceKm: Double,
    val clientRating: Double? = null
)

class RideAutoAcceptService : AccessibilityService() {

    // Adapte ces regex à l'affichage réel de ton app (via Layout Inspector)
    private val pricePattern = Pattern.compile("(\\d+[.,]?\\d*)\\s?(€|MAD|DH)")
    private val distancePattern = Pattern.compile("(\\d+[.,]?\\d*)\\s?km")
    private val ratingPattern = Pattern.compile("(\\d[.,]\\d)\\s?(/5|★|étoiles)")

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val targetPkg = getSharedPreferences("config", MODE_PRIVATE)
            .getString("target_package", null) ?: return

        if (event.packageName?.toString() != targetPkg) return

        val root = rootInActiveWindow ?: return
        analyzeScreenAndMaybeAccept(root)
    }

    private fun analyzeScreenAndMaybeAccept(root: AccessibilityNodeInfo) {
        val texts = mutableListOf<String>()
        collectText(root, texts)

        val ride = extractRideInfo(texts) ?: return

        if (matchesDriverConditions(ride)) {
            clickAcceptButton(root)
        }
    }

    private fun collectText(node: AccessibilityNodeInfo, out: MutableList<String>) {
        node.text?.let { out.add(it.toString()) }
        node.contentDescription?.let { out.add(it.toString()) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, out) }
        }
    }

    private fun extractRideInfo(texts: List<String>): RideInfo? {
        var price: Double? = null
        var rideDistance: Double? = null
        var pickupDistance: Double? = null
        var rating: Double? = null

        val distancesFound = mutableListOf<Double>()

        for (text in texts) {
            val priceMatcher = pricePattern.matcher(text)
            if (priceMatcher.find() && price == null) {
                price = priceMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull()
            }

            val distanceMatcher = distancePattern.matcher(text)
            if (distanceMatcher.find()) {
                distanceMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull()?.let {
                    distancesFound.add(it)
                }
            }

            val ratingMatcher = ratingPattern.matcher(text)
            if (ratingMatcher.find() && rating == null) {
                rating = ratingMatcher.group(1)?.replace(",", ".")?.toDoubleOrNull()
            }
        }

        // Hypothèse par défaut : la 1ère distance trouvée = distance pour aller chercher le client,
        // la 2e = longueur de la course. À AJUSTER selon l'ordre réel d'affichage de ton app.
        pickupDistance = distancesFound.getOrNull(0)
        rideDistance = distancesFound.getOrNull(1) ?: distancesFound.getOrNull(0)

        if (price == null || pickupDistance == null || rideDistance == null) return null

        return RideInfo(
            price = price,
            pickupDistanceKm = pickupDistance,
            rideDistanceKm = rideDistance,
            clientRating = rating
        )
    }

    private fun matchesDriverConditions(ride: RideInfo): Boolean {
        val prefs = getSharedPreferences("config", MODE_PRIVATE)

        val minPrice = prefs.getFloat("min_price", 0f)
        val maxPickupKm = prefs.getFloat("max_pickup_km", 5f)
        val minPricePerKm = prefs.getFloat("min_price_per_km", 0f)
        val maxRideKm = prefs.getFloat("max_ride_km", 999f)
        val avoidLowRated = prefs.getBoolean("avoid_low_rated", false)

        if (ride.price < minPrice) return false
        if (ride.pickupDistanceKm > maxPickupKm) return false
        if (ride.rideDistanceKm > maxRideKm) return false
        if (ride.rideDistanceKm > 0 && (ride.price / ride.rideDistanceKm) < minPricePerKm) return false
        if (avoidLowRated && ride.clientRating != null && ride.clientRating < 4.0) return false

        return true
    }

    private fun clickAcceptButton(root: AccessibilityNodeInfo) {
        // Adapte les libellés à ceux de ton app ("Accepter", "Accept", etc.)
        val labels = listOf("Accepter", "ACCEPTER", "Accept")
        for (label in labels) {
            val nodes = root.findAccessibilityNodeInfosByText(label)
            val button = nodes?.firstOrNull { it.isClickable }
            if (button != null) {
                button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }
    }

    override fun onInterrupt() {}
}
