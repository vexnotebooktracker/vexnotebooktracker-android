package com.bodepudi.vexnotebooktracker.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.gson.annotations.SerializedName

@Parcelize
data class VexUrlResponse(
    @SerializedName("shortUrl")
    val shortUrl: String,
    @SerializedName("startDate")
    val startDate: String,
    @SerializedName("endDate")
    val endDate: String,
    @SerializedName("redirectionTypeId")
    val redirectionTypeId: Int,
    @SerializedName("preventViewingOnMobile")
    val preventViewingOnMobile: Boolean
) : Parcelable

@Parcelize
data class VexTeamResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("teamNumber")
    val teamNumber: String,
    @SerializedName("teamName")
    val teamName: String,
    @SerializedName("organization")
    val organization: String,
    @SerializedName("city")
    val city: String,
    @SerializedName("region")
    val region: String,
    @SerializedName("country")
    val country: String
) : Parcelable

@Parcelize
data class VexEventResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("startDate")
    val startDate: String,
    @SerializedName("endDate")
    val endDate: String,
    @SerializedName("location")
    val location: String,
    @SerializedName("division")
    val division: String
) : Parcelable

// Data model for table display with urlId support
@Parcelize
data class TableRowData(
    @SerializedName("field")
    val field: String,      // Field name/label
    @SerializedName("value")
    val value: String,      // Field value
    @SerializedName("type")
    val type: String = "text", // Type for styling: "url", "date", "number", "team", "views", "boolean"
    @SerializedName("urlId")
    val urlId: Int? = null  // Optional: for linking back to original DashboardUrlRecord
) : Parcelable