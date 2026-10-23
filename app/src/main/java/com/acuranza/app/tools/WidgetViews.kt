package com.acuranza.app.tools

import android.content.Context
import android.widget.RemoteViews
import com.acuranza.app.R
import com.acuranza.app.model.Money

/**
 * The single rendering path for every widget: builds the exact RemoteViews each provider ships,
 * from plain data. The updaters feed real figures; the in-app Add Widgets screen feeds SAMPLE
 * figures — previews are the actual widget rendering, so they can never drift from it.
 */
object WidgetViews {
    private const val HIDDEN = "₹ ••••"

    fun month(
        context: Context,
        f: WidgetFigures,
        show: Boolean,
        title: String,
        ringFraction: Float,
        budgetCaption: String,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_month)
        views.setTextViewText(R.id.widget_title, title)
        views.setTextViewText(R.id.widget_saved, if (show) Money.format(f.saved, showPaise = false) else HIDDEN)
        views.setTextColor(
            R.id.widget_saved,
            context.getColor(if (show && f.saved < 0) R.color.widget_overdue else R.color.widget_ink),
        )
        val rate = f.ratePercent
        views.setTextViewText(
            R.id.widget_rate,
            when {
                !show -> ""
                rate == null -> "No income yet"
                f.saved < 0 -> "Overspent"
                else -> "$rate% saved"
            },
        )
        views.setTextViewText(R.id.widget_income, if (show) Money.compact(f.income) else HIDDEN)
        views.setTextViewText(R.id.widget_spent, if (show) Money.compact(f.spent) else HIDDEN)
        val ring = WidgetCharts.ring(
            sizePx = 200,
            progress = if (show) ringFraction else 0f,
            trackColor = context.getColor(R.color.widget_track),
            progressColor = context.getColor(R.color.widget_accent),
            strokeWidthPx = 24f,
        )
        views.setImageViewBitmap(R.id.widget_ring, ring)
        views.setTextViewText(R.id.widget_bar_caption, if (show) budgetCaption else "")
        return views
    }

    fun goal(context: Context, goal: GoalProgress?, show: Boolean): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_goal)
        if (goal == null) {
            views.setViewVisibility(R.id.widget_goal_body, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_empty, android.view.View.VISIBLE)
            return views
        }
        views.setViewVisibility(R.id.widget_goal_body, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_empty, android.view.View.GONE)
        views.setTextViewText(R.id.widget_goal_saved, if (show) Money.format(goal.saved, showPaise = false) else HIDDEN)
        views.setTextViewText(
            R.id.widget_goal_caption,
            when {
                !show -> ""
                goal.percent >= 100 -> "Goal reached"
                else -> "${Money.compact(goal.remaining)} to go"
            },
        )
        views.setTextViewText(R.id.widget_goal_target, if (show) "of ${Money.compact(goal.target)} · ${goal.percent}%" else HIDDEN)
        val ring = WidgetCharts.ring(
            sizePx = 200,
            progress = if (show) goal.percent / 100f else 0f,
            trackColor = context.getColor(R.color.widget_track),
            progressColor = context.getColor(R.color.widget_income),
            strokeWidthPx = 24f,
        )
        views.setImageViewBitmap(R.id.widget_goal_ring, ring)
        return views
    }

    fun categories(context: Context, slices: List<CategorySlice>, show: Boolean): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_categories)
        if (slices.isEmpty()) {
            views.setViewVisibility(R.id.widget_donut_frame, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_cat_legend, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_cat_empty, android.view.View.VISIBLE)
            return views
        }
        views.setViewVisibility(R.id.widget_donut_frame, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_cat_legend, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_cat_empty, android.view.View.GONE)
        val total = slices.sumOf { it.amount }
        val donut = WidgetCharts.donut(
            sizePx = 220,
            slices = slices.map { it.color to it.fraction },
            strokeWidthPx = 28f,
            gapDegrees = 3f,
        )
        views.setImageViewBitmap(R.id.widget_donut, donut)
        views.setTextViewText(R.id.widget_cat_total, if (show) Money.compact(total) else HIDDEN)
        val legend = slices.joinToString("\n") { slice ->
            val amount = if (show) Money.compact(slice.amount) else HIDDEN
            "${slice.name} · $amount"
        }
        views.setTextViewText(R.id.widget_cat_legend, legend)
        return views
    }

    fun quickAdd(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_add)
        views.setOnClickPendingIntent(R.id.widget_add_expense, QuickLaunch.pending(context, QuickLaunch.ROUTE_ADD_EXPENSE, 7_201))
        views.setOnClickPendingIntent(R.id.widget_add_income, QuickLaunch.pending(context, QuickLaunch.ROUTE_ADD_INCOME, 7_202))
        views.setOnClickPendingIntent(R.id.widget_add_transfer, QuickLaunch.pending(context, QuickLaunch.ROUTE_TRANSFER, 7_203))
        views.setOnClickPendingIntent(R.id.widget_add_transactions, QuickLaunch.pending(context, QuickLaunch.ROUTE_TRANSACTIONS, 7_204))
        return views
    }

    fun shortcuts(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_shortcuts)
        views.setOnClickPendingIntent(R.id.widget_shortcut_bills, QuickLaunch.pending(context, QuickLaunch.ROUTE_BILLS, 7_301))
        views.setOnClickPendingIntent(R.id.widget_shortcut_budgets, QuickLaunch.pending(context, QuickLaunch.ROUTE_PLANNING, 7_302))
        views.setOnClickPendingIntent(R.id.widget_shortcut_reports, QuickLaunch.pending(context, QuickLaunch.ROUTE_REPORTS, 7_303))
        views.setOnClickPendingIntent(R.id.widget_shortcut_accounts, QuickLaunch.pending(context, QuickLaunch.ROUTE_ACCOUNTS, 7_304))
        return views
    }
}
