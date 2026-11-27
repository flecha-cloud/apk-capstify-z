
package com.capstify.adminz

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class OrderData(
    val id: Long,
    val platform: String,
    val product: String,
    val quantity: String,
    val price: String,
    val orderNumber: String,
    val orderDate: String,
    val courier: String = "",
    val tracking: String = "",
    val city: String = "",
    val recipient: String = "",
    val address: String = "",
    val status: String = "Packing Dulu Bre",
    val locked: Boolean = false
)

object OrderStorage {

    private const val PREFS_NAME = "capstify_orders"
    private const val ORDERS_KEY = "saved_orders"

    fun load(context: Context): List<OrderData> {
        return try {
            val preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            val savedData = preferences.getString(
                ORDERS_KEY,
                "[]"
            ) ?: "[]"

            val jsonArray = JSONArray(savedData)

            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index)
                        ?: continue

                    add(
                        OrderData(
                            id = item.optLong("id"),
                            platform = item.optString("platform"),
                            product = item.optString("product"),
                            quantity = item.optString("quantity"),
                            price = item.optString("price"),
                            orderNumber = item.optString("orderNumber"),
                            orderDate = item.optString("orderDate"),
                            courier = item.optString("courier"),
                            tracking = item.optString("tracking"),
                            city = item.optString("city"),
                            recipient = item.optString("recipient"),
                            address = item.optString("address"),
                            status = item.optString(
                                "status",
                                "Packing Dulu Bre"
                            ),
                            locked = item.optBoolean("locked", false)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(
        context: Context,
        orders: List<OrderData>
    ) {
        val jsonArray = JSONArray()

        orders.forEach { order ->
            val item = JSONObject().apply {
                put("id", order.id)
                put("platform", order.platform)
                put("product", order.product)
                put("quantity", order.quantity)
                put("price", order.price)
                put("orderNumber", order.orderNumber)
                put("orderDate", order.orderDate)
                put("courier", order.courier)
                put("tracking", order.tracking)
                put("city", order.city)
                put("recipient", order.recipient)
                put("address", order.address)
                put("status", order.status)
                put("locked", order.locked)
            }

            jsonArray.put(item)
        }

        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putString(ORDERS_KEY, jsonArray.toString())
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .remove(ORDERS_KEY)
            .apply()
    }
}
