package net.vite.wallet.exchange.history

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.vite.wallet.R
import net.vite.wallet.utils.blueWhiteGreyDarkSelect
import net.vite.wallet.utils.viewbinding.findMyView


class FilterBottomSheetDialog(
    context: Context,
    var filter: OrdersFilter,
    val fragment: HistoryOrderListFragment,
) : BottomSheetDialog(context) {
    val dateFilters by findMyView<ViewGroup>(R.id.date_filters_container)
    val typeFilters by findMyView<ViewGroup>(R.id.filter_types)
    val statusFilters by findMyView<ViewGroup>(R.id.filter_status_list_container)
    val coinFilterText by findMyView<TextView>(R.id.coin_filter_text)
    val coinFilter by findMyView<View>(R.id.coin_filter)
    val clear by findMyView<View>(R.id.clear)
    val confirm by findMyView<View>(R.id.confirm)
    val closeBtn by findMyView<View>(R.id.closeBtn)


    fun dateIdToRange(id: Int) = when (id) {
        R.id.date_1_day -> DateRange.ONE_DAY
        R.id.date_1_week -> DateRange.ONE_WEEK
        R.id.date_1_month -> DateRange.ONE_MONTH
        R.id.date_3_months -> DateRange.THREE_MONTH
        else -> DateRange.ALL
    }

    fun typeIdToSide(id: Int) = when (id) {
        R.id.filter_type_sell -> 1
        R.id.filter_type_buy -> 0
        else -> null
    }

    fun statusIdToStatus(id: Int) = when (id) {
        R.id.filter_status_ongoing -> 3
        R.id.filter_status_completed -> 4
        R.id.filter_status_cancelled -> 7
        R.id.filter_status_failed -> 9
        else -> null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bottom_sheet_orders_filter)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        closeBtn.setOnClickListener {
            dismiss()
        }
        dateFilters.children.forEach {
            it.setOnClickListener {
                onDateTextViewClick(it.id)
            }
        }
        typeFilters.children.forEach {
            it.setOnClickListener {
                onTypeViewClick(it.id)
            }
        }
        statusFilters.children.forEach {
            it.setOnClickListener {
                onStatusViewClick(it.id)
            }
        }

        coinFilter.setOnClickListener {
            fragment.chooseSymbol()
        }

        clear.setOnClickListener {
            filter = OrdersFilter.EMPTY
            filterChanged()
        }

        confirm.setOnClickListener {
            fragment.setFilter(filter)
            dismiss()
        }

        filterChanged()
    }


    private fun onDateTextViewClick(id: Int) {
        val range = dateIdToRange(id)
        filter = filter.copy(dateRange = range)
        filterChanged()
    }


    private fun onTypeViewClick(id: Int) {
        val side = typeIdToSide(id)
        filter = filter.copy(side = side)
        filterChanged()
    }

    private fun onStatusViewClick(id: Int) {
        val status = statusIdToStatus(id)
        filter = filter.copy(status = status)
        filterChanged()
    }

    private fun updateSelectedStatus(containerViewGroup: ViewGroup, id: Int) {
        containerViewGroup.children.mapNotNull { it as? TextView }.forEach {
            it.blueWhiteGreyDarkSelect(it.id == id)
        }
    }

    private val selectedDateId: Int
        get() = when (filter.dateRange) {
            DateRange.ONE_DAY -> R.id.date_1_day
            DateRange.ONE_WEEK -> R.id.date_1_week
            DateRange.ONE_MONTH -> R.id.date_1_month
            DateRange.THREE_MONTH -> R.id.date_3_months
            else -> R.id.date_all
        }

    private val selectedTypeId: Int
        get() = when (filter.side) {
            0 -> R.id.filter_type_buy
            1 -> R.id.filter_type_sell
            else -> R.id.filter_type_all
        }

    private val selectedStatusId: Int
        get() = when (filter.status) {
            3 -> R.id.filter_status_ongoing
            4 -> R.id.filter_status_completed
            7 -> R.id.filter_status_cancelled
            9 -> R.id.filter_status_failed
            else -> R.id.filter_status_all
        }


    fun filterChanged() {
        updateSelectedStatus(dateFilters, selectedDateId)
        updateSelectedStatus(typeFilters, selectedTypeId)
        updateSelectedStatus(statusFilters, selectedStatusId)
        coinFilterText.text = filter.symbol ?: context.getString(R.string.all)
    }

}