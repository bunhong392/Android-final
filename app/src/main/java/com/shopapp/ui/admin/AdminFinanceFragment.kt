package com.shopapp.ui.admin

import androidx.lifecycle.lifecycleScope
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.shopapp.data.model.Order
import com.shopapp.data.repository.Repository
import com.shopapp.databinding.FragmentAdminFinanceBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdminFinanceFragment : Fragment() {

    private var _binding: FragmentAdminFinanceBinding? = null
    private val binding get() = _binding!!

    private val colorPurpleLight = Color.parseColor("#AFA9EC")
    private val colorPurpleMid   = Color.parseColor("#7F77DD")
    private val colorPurpleDark  = Color.parseColor("#534AB7")
    private val colorPurpleDeep  = Color.parseColor("#3C3489")

    // All orders fetched once, then filtered locally
    private var allOrders: List<Order> = emptyList()

    // Active range label shown in tvDateRange
    private var activeRangeLabel = "All Time"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminFinanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AdminActivity)?.tvPageTitle?.text = "Finances"
        binding.swipeRefresh.setOnRefreshListener { loadFinanceData() }
        loadFinanceData()

        // ── View Range button ─────────────────────────────────────────────────
        binding.btnViewRange.setOnClickListener { showRangeMenu(it) }
    }

    // ── Range popup menu ──────────────────────────────────────────────────────

    private fun showRangeMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.apply {
            add(0, 1, 0, "Today")
            add(0, 2, 0, "This Week")
            add(0, 3, 0, "This Month")
            add(0, 4, 0, "This Year")
            add(0, 5, 0, "Custom Range…")
            add(0, 6, 0, "All Time")
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> applyRange("Today",      startOfToday(), endOfToday())
                2 -> applyRange("This Week",  startOfWeek(),  endOfToday())
                3 -> applyRange("This Month", startOfMonth(), endOfToday())
                4 -> applyRange("This Year",  startOfYear(),  endOfToday())
                5 -> showCustomRangePicker()
                6 -> applyRange("All Time",   0L, Long.MAX_VALUE)
            }
            true
        }
        popup.show()
    }

    private fun applyRange(label: String, from: Long, to: Long) {
        activeRangeLabel = label
        val displayFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.tvDateRange.text = if (label == "All Time") "All Time"
        else "${displayFmt.format(Date(from))} – ${displayFmt.format(Date(to))}"
        renderFinanceData(allOrders.filter { it.createdAt in from..to })
    }

    // ── Custom date range picker (two DatePickerDialogs chained) ─────────────

    private fun showCustomRangePicker() {
        val cal = Calendar.getInstance()
        // Pick START date
        DatePickerDialog(requireContext(), { _, sy, sm, sd ->
            val startCal = Calendar.getInstance().apply { set(sy, sm, sd, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
            // Pick END date
            DatePickerDialog(requireContext(), { _, ey, em, ed ->
                val endCal = Calendar.getInstance().apply { set(ey, em, ed, 23, 59, 59); set(Calendar.MILLISECOND, 999) }
                val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                applyRange(
                    "${fmt.format(startCal.time)} – ${fmt.format(endCal.time)}",
                    startCal.timeInMillis,
                    endCal.timeInMillis
                )
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ── Date helpers ──────────────────────────────────────────────────────────

    private fun startOfToday()  = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    private fun endOfToday()    = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
    private fun startOfWeek()   = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    private fun startOfMonth()  = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    private fun startOfYear()   = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

    // ── Data loading ──────────────────────────────────────────────────────────

    private fun loadFinanceData() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Repository.getAllOrders()
            binding.swipeRefresh.isRefreshing = false
            result.onSuccess { orders ->
                allOrders = orders
                // Re-apply the current active range after refresh
                when (activeRangeLabel) {
                    "Today"      -> applyRange("Today",      startOfToday(), endOfToday())
                    "This Week"  -> applyRange("This Week",  startOfWeek(),  endOfToday())
                    "This Month" -> applyRange("This Month", startOfMonth(), endOfToday())
                    "This Year"  -> applyRange("This Year",  startOfYear(),  endOfToday())
                    "All Time"   -> applyRange("All Time",   0L, Long.MAX_VALUE)
                    else         -> renderFinanceData(orders) // custom range — show all on refresh
                }
            }.onFailure {
                showZeroState()
            }
        }
    }

    private fun renderFinanceData(orders: List<Order>) {
        if (orders.isEmpty()) { showZeroState(); return }

        val totalRevenue = orders.sumOf { it.totalAmount }
        val grossProfit  = totalRevenue * 0.42
        binding.tvNetSales.text    = "$${"%.0f".format(totalRevenue)}"
        binding.tvGrossProfit.text = "$${"%.0f".format(grossProfit)}"

        val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
        val months   = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val salesByMonth = mutableMapOf<String, Float>().apply { months.forEach { put(it, 0f) } }

        orders.forEach { order ->
            val month = monthFmt.format(Date(order.createdAt))
            salesByMonth[month] = (salesByMonth[month] ?: 0f) + order.totalAmount.toFloat()
        }

        val monthValues  = months.map { salesByMonth[it] ?: 0f }
        val profitValues = monthValues.map { it * 0.42f }

        setupNetSalesChart(monthValues)
        setupGrossProfitChart(profitValues)
        setupRevenueChart(salesByMonth)
    }

    private fun showZeroState() {
        binding.tvNetSales.text    = "$0"
        binding.tvGrossProfit.text = "$0"
        setupNetSalesChart(listOf(0f))
        setupGrossProfitChart(listOf(0f))
        setupRevenueChart(emptyMap())
    }

    // ── Chart helpers (unchanged) ─────────────────────────────────────────────

    private fun setupNetSalesChart(data: List<Float>) {
        setupBarChart(binding.chartNetSales, data, colorPurpleLight, colorPurpleDark, "Net Sales")
    }

    private fun setupGrossProfitChart(data: List<Float>) {
        setupBarChart(binding.chartGrossProfit, data, colorPurpleLight, colorPurpleDark, "Gross Profit")
    }

    private fun setupBarChart(
        chart: BarChart, values: List<Float>,
        colorLight: Int, colorDark: Int, label: String
    ) {
        val entries = values.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
        val colors  = entries.indices.map { if (it % 2 == 0) colorLight else colorDark }
        val dataSet = BarDataSet(entries, label).apply {
            setColors(colors); setDrawValues(false)
        }
        chart.apply {
            this.data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false; legend.isEnabled = false
            setTouchEnabled(false); setDrawGridBackground(false); setDrawBorders(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false); setDrawAxisLine(false)
                textColor = Color.parseColor("#9896B8"); textSize = 9f
                labelCount = 6; granularity = 1f
            }
            axisLeft.apply {
                setDrawGridLines(true); gridColor = Color.parseColor("#F0EFF8")
                setDrawAxisLine(false); textColor = Color.parseColor("#9896B8"); textSize = 9f
            }
            axisRight.isEnabled = false; animateY(600); invalidate()
        }
    }

    private fun setupRevenueChart(salesByMonth: Map<String, Float>) {
        val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val entries = months.indices.map { i ->
            val base = salesByMonth[months[i]] ?: 0f
            BarEntry(i.toFloat(), floatArrayOf(
                base * 0.40f,
                base * 0.25f,
                base * 0.20f,
                base * 0.15f
            ))
        }
        val dataSet = BarDataSet(entries, "Revenue").apply {
            colors      = listOf(colorPurpleMid, colorPurpleDark, colorPurpleDeep, colorPurpleLight)
            stackLabels = arrayOf("Business", "Finance", "Tech", "Enterprise")
            setDrawValues(false)
        }
        binding.chartRevenue.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false; legend.isEnabled = false
            setTouchEnabled(false); setDrawGridBackground(false); setDrawBorders(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(months)
                setDrawGridLines(false); setDrawAxisLine(false)
                textColor = Color.parseColor("#9896B8"); textSize = 8f
                labelCount = 6; granularity = 1f
                valueFormatter = IndexAxisValueFormatter(
                    arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                )
            }
            axisLeft.apply {
                setDrawGridLines(true); gridColor = Color.parseColor("#F0EFF8")
                setDrawAxisLine(false); textColor = Color.parseColor("#9896B8"); textSize = 9f
            }
            axisRight.isEnabled = false; animateY(600); invalidate()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
