package it.cammino.risuscito

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.support.design.bottomappbar.BottomAppBar
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import androidx.core.content.edit
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter.commons.utils.FastAdapterDiffUtil
import com.mikepenz.fastadapter.listeners.OnClickListener
import com.mikepenz.fastadapter.select.SelectExtension
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.mikepenz.itemanimators.SlideLeftAlphaAnimator
import it.cammino.risuscito.database.RisuscitoDatabase
import it.cammino.risuscito.dialogs.SimpleDialogFragment
import it.cammino.risuscito.items.CheckableItem
import it.cammino.risuscito.items.SimpleItem
import it.cammino.risuscito.services.ConsegnatiSaverService
import it.cammino.risuscito.utils.ThemeUtils
import it.cammino.risuscito.viewmodels.ConsegnatiViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.common_bottom_bar.*
import kotlinx.android.synthetic.main.layout_consegnati.*

class ConsegnatiFragment : Fragment(), SimpleDialogFragment.SimpleCallback {

    internal var cantoAdapter: FastItemAdapter<SimpleItem> = FastItemAdapter()

    private var mCantiViewModel: ConsegnatiViewModel? = null
    private var rootView: View? = null
    private var selectableAdapter: FastItemAdapter<CheckableItem>? = null
    //    private var mFab: FloatingActionButton? = null
    private var mBottomBar: BottomAppBar? = null
    private var mMainActivity: MainActivity? = null
    private var mLUtils: LUtils? = null
    private var mLastClickTime: Long = 0
    private var mRegularFont: Typeface? = null
    private val positionBRec = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Implement UI change code here once notification is received
            try {
                Log.d(javaClass.name, "BROADCAST_SINGLE_COMPLETED")
                Log.d(
                        javaClass.name,
                        "DATA_DONE: " + intent.getIntExtra(ConsegnatiSaverService.DATA_DONE, 0))
                val fragment = SimpleDialogFragment.findVisible(
                        (activity as AppCompatActivity?)!!, "CONSEGNATI_SAVING")
                fragment?.setProgress(intent.getIntExtra(ConsegnatiSaverService.DATA_DONE, 0))
            } catch (e: IllegalArgumentException) {
                Log.e(javaClass.name, e.localizedMessage, e)
            }

        }
    }
    private val completedBRec = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Implement UI change code here once notification is received
            try {
                Log.d(javaClass.name, "BROADCAST_SAVING_COMPLETED")
                val fragment = SimpleDialogFragment.findVisible(
                        (activity as AppCompatActivity?)!!, "CONSEGNATI_SAVING")
                fragment?.dismiss()
                //                updateConsegnatiList();
                chooseRecycler!!.visibility = View.GONE
                enableBottombar(false)
                selected_view!!.visibility = View.VISIBLE
//                mMainActivity!!.enableFab(true)
                enableFab(true)
                mCantiViewModel!!.titoliChoose = ArrayList()
            } catch (e: IllegalArgumentException) {
                Log.e(javaClass.name, e.localizedMessage, e)
            }

        }
    }

    private val themeUtils: ThemeUtils
        get() = (activity as MainActivity).themeUtils!!


//    private val fab: FloatingActionButton
//        get() {
//            Log.d(TAG, "getFab null?" + (mFab == null))
//            if (mFab == null) {
//                mFab = activity!!.findViewById(R.id.fab_pager)
//                mFab!!.visibility = View.VISIBLE
//                val icon = IconicsDrawable(activity!!)
//                        .icon(CommunityMaterial.Icon.cmd_pencil)
//                        .color(Color.WHITE)
//                        .sizeDp(24)
//                        .paddingDp(2)
//                mFab!!.setImageDrawable(icon)
//            }
//            return mFab as FloatingActionButton
//        }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.layout_consegnati, container, false)

        mCantiViewModel = ViewModelProviders.of(this).get(ConsegnatiViewModel::class.java)

        mMainActivity = activity as MainActivity?

        mRegularFont = ResourcesCompat.getFont(mMainActivity!!, R.font.googlesans_regular)

        mMainActivity!!.setupToolbarTitle(R.string.title_activity_consegnati)

//        fab
//                .setOnClickListener {
//                    mCantiViewModel!!.editMode = true
//                    updateChooseList()
//                    selected_view!!.visibility = View.INVISIBLE
//                    chooseRecycler!!.visibility = View.VISIBLE
//                    enableBottombar(true)
////                    mMainActivity!!.enableFab(false)
//                    enableFab(false)
//                    val mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
//                    if (!mSharedPrefs.getBoolean(Utility.INTRO_CONSEGNATI_2, false)) {
//                        managerIntro()
//                    }
//                }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBottomBar = if (mMainActivity!!.isOnTablet)
            bottom_bar
        else
            activity!!.bottom_bar

        activity!!.material_tabs.visibility = View.GONE
//        mMainActivity!!.enableFab(true)
        initFab()

//        mBottomBar!!.inflateMenu(R.menu.consegnati)
        mBottomBar!!.replaceMenu(R.menu.empty)
        IconicsMenuInflaterUtil.inflate(
                activity!!.menuInflater, activity, R.menu.consegnati, mBottomBar!!.menu, false)
        mBottomBar!!.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.select_none -> {
                    (selectableAdapter!!.getExtension<SelectExtension<CheckableItem>>(SelectExtension::class.java))!!.deselect()
                    true
                }
                R.id.select_all -> {
                    (selectableAdapter!!.getExtension<SelectExtension<CheckableItem>>(SelectExtension::class.java))!!.select()
                    true
                }
                R.id.cancel_change -> {
                    mCantiViewModel!!.editMode = false
                    chooseRecycler!!.visibility = View.INVISIBLE
                    enableBottombar(false)
                    selected_view!!.visibility = View.VISIBLE
//                    mMainActivity!!.enableFab(true)
                    enableFab(true)
                    mCantiViewModel!!.titoliChoose = ArrayList()
                    true
                }
                R.id.confirm_changes -> {
                    mCantiViewModel!!.editMode = false
                    SimpleDialogFragment.Builder(
                            (activity as AppCompatActivity?)!!, this@ConsegnatiFragment, "CONSEGNATI_SAVING")
                            .content(R.string.save_consegnati_running)
                            .showProgress()
                            .progressIndeterminate(false)
                            .progressMax(selectableAdapter!!.itemCount)
                            .show()

                    val mSelected = (selectableAdapter!!.getExtension<SelectExtension<CheckableItem>>(SelectExtension::class.java))!!.selectedItems
                    val mSelectedId = mSelected.mapTo(ArrayList()) { it.id }

                    val intent = Intent(activity!!.applicationContext, ConsegnatiSaverService::class.java)
                    intent.putIntegerArrayListExtra(ConsegnatiSaverService.IDS_CONSEGNATI, mSelectedId)
                    activity!!.applicationContext.startService(intent)
                    true
                }
                else -> false
            }

        }

        mLUtils = LUtils.getInstance(activity!!)

//        mBottomBar!!.setBackgroundColor(themeUtils.primaryColor())
        mBottomBar!!.backgroundTint = ColorStateList(arrayOf(intArrayOf()), intArrayOf(themeUtils.primaryColor()))

        //    Log.d(TAG, "onCreateView - editMode: " + mCantiViewModel.editMode);
//        val mSelectNone = if (mMainActivity!!.isOnTablet)
//            rootView!!.findViewById(R.id.select_none)
//        else
//            activity!!.findViewById<View>(R.id.select_none)
//        mSelectNone.setOnClickListener {
//            //            selectableAdapter.deselect();
//            (selectableAdapter!!.getExtension<SelectExtension<CheckableItem>>(SelectExtension::class.java))!!.deselect()
//        }
//
//        val mSelectAll = if (mMainActivity!!.isOnTablet)
//            select_all
//        else
//            activity!!.select_all
//        mSelectAll.setOnClickListener {
//            (selectableAdapter!!.getExtension<SelectExtension<CheckableItem>>(SelectExtension::class.java))!!.select()
//        }
//
//        val cancelChange = if (mMainActivity!!.isOnTablet)
//            cancel_change
//        else
//            activity!!.cancel_change
//
//        cancelChange.setOnClickListener {
//            mCantiViewModel!!.editMode = false
//            chooseRecycler!!.visibility = View.INVISIBLE
//            enableBottombar(false)
//            selected_view!!.visibility = View.VISIBLE
//            mMainActivity!!.enableFab(true)
//            mCantiViewModel!!.titoliChoose = ArrayList()
//        }
//
//        val confirmChanges = if (mMainActivity!!.isOnTablet)
//            confirm_changes
//        else
//            activity!!.confirm_changes
//        confirmChanges.setOnClickListener {
//            mCantiViewModel!!.editMode = false
//            SimpleDialogFragment.Builder(
//                    (activity as AppCompatActivity?)!!, this@ConsegnatiFragment, "CONSEGNATI_SAVING")
//                    .content(R.string.save_consegnati_running)
//                    .showProgress()
//                    .progressIndeterminate(false)
//                    .progressMax(selectableAdapter!!.itemCount)
//                    .show()
//
//            val mSelected = (selectableAdapter!!.getExtension<SelectExtension<CheckableItem>>(SelectExtension::class.java))!!.selectedItems
//            val mSelectedId = mSelected.mapTo(ArrayList()) { it.id }
//
//            val intent = Intent(activity!!.applicationContext, ConsegnatiSaverService::class.java)
//            intent.putIntegerArrayListExtra(ConsegnatiSaverService.IDS_CONSEGNATI, mSelectedId)
//            activity!!.applicationContext.startService(intent)
//        }

        val mOnClickListener = OnClickListener<SimpleItem> { _, _, item, _ ->
            if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY) return@OnClickListener true
            mLastClickTime = SystemClock.elapsedRealtime()
            val bundle = Bundle()
            bundle.putCharSequence("pagina", item.source!!.text)
            bundle.putInt("idCanto", item.id)
            // lancia l'activity che visualizza il canto passando il parametro creato
            startSubActivity(bundle)
            true
        }

        // Creating new adapter object
//        cantoAdapter = FastItemAdapter()
        cantoAdapter.withOnClickListener(mOnClickListener)
        FastAdapterDiffUtil.set(cantoAdapter, mCantiViewModel!!.titoli)

        cantiRecycler!!.adapter = cantoAdapter
//        val llm = LinearLayoutManager(context)
        val llm = if (mMainActivity!!.isGridLayout)
            GridLayoutManager(context, if (mMainActivity!!.hasThreeColumns) 3 else 2)
        else
            LinearLayoutManager(context)
        cantiRecycler!!.layoutManager = llm
        cantiRecycler!!.setHasFixedSize(true)
        val insetDivider = DividerItemDecoration(context!!, llm.orientation)
        insetDivider.setDrawable(
                ContextCompat.getDrawable(context!!, R.drawable.material_inset_divider)!!)
        cantiRecycler!!.addItemDecoration(insetDivider)
        cantiRecycler!!.itemAnimator = SlideLeftAlphaAnimator()

        // Creating new adapter object
        selectableAdapter = FastItemAdapter()
        selectableAdapter!!.withSelectable(true).setHasStableIds(true)

        // init the ClickListenerHelper which simplifies custom click listeners on views of the Adapter
        selectableAdapter!!.withOnPreClickListener { _, _, _, position ->
            selectableAdapter!!
                    .getAdapterItem(position)
                    .withSetSelected(!selectableAdapter!!.getAdapterItem(position).isSelected)
            selectableAdapter!!.notifyAdapterItemChanged(position)
            true
        }
        selectableAdapter!!.withEventHook(CheckableItem.CheckBoxClickEvent())
        FastAdapterDiffUtil.set(selectableAdapter!!, mCantiViewModel!!.titoliChoose)

        chooseRecycler!!.adapter = selectableAdapter
//        val llm2 = LinearLayoutManager(context)
        val llm2 = if (mMainActivity!!.isGridLayout)
            GridLayoutManager(context, if (mMainActivity!!.hasThreeColumns) 3 else 2)
        else
            LinearLayoutManager(context)
        chooseRecycler!!.layoutManager = llm2
        chooseRecycler!!.setHasFixedSize(true)
        val insetDivider2 = DividerItemDecoration(context!!, llm2.orientation)
        insetDivider.setDrawable(
                ContextCompat.getDrawable(context!!, R.drawable.material_inset_divider)!!)
        chooseRecycler!!.addItemDecoration(insetDivider2)
        chooseRecycler!!.itemAnimator = SlideLeftAlphaAnimator()
    }

    override fun onResume() {
        super.onResume()
        //    Log.d(getClass().getName(), "onResume: ");
//        activity!!
        LocalBroadcastManager.getInstance(activity!!)
                .registerReceiver(
                        positionBRec, IntentFilter(ConsegnatiSaverService.BROADCAST_SINGLE_COMPLETED))
//        activity!!
        LocalBroadcastManager.getInstance(activity!!)
                .registerReceiver(
                        completedBRec, IntentFilter(ConsegnatiSaverService.BROADCAST_SAVING_COMPLETED))
        if (mCantiViewModel!!.editMode) {
            chooseRecycler!!.visibility = View.VISIBLE
            enableBottombar(true)
            selected_view!!.visibility = View.INVISIBLE
//            mMainActivity!!.enableFab(false)
            enableFab(false)
        } else {
            chooseRecycler!!.visibility = View.GONE
            enableBottombar(false)
            selected_view!!.visibility = View.VISIBLE
//            mMainActivity!!.enableFab(true)
            enableFab(true)
        }
        val mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
        if (!mSharedPrefs.getBoolean(Utility.INTRO_CONSEGNATI, false)) {
            fabIntro()
        }
    }

    override fun onPause() {
        super.onPause()
//        activity!!.unregisterReceiver(positionBRec)
        LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(positionBRec)
//        activity!!.unregisterReceiver(completedBRec)
        LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(completedBRec)
    }

//    override fun onDestroy() {
//        enableBottombar(false)
//        enableFab(false)
//        super.onDestroy()
//    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        populateDb()
        subscribeUiConsegnati()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        IconicsMenuInflaterUtil.inflate(
                activity!!.menuInflater, activity, R.menu.help_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_help -> {
                if (mCantiViewModel!!.editMode)
                    managerIntro()
                else
                    fabIntro()
                return true
            }
        }
        return false
    }

    private fun startSubActivity(bundle: Bundle) {
        val intent = Intent(activity, PaginaRenderActivity::class.java)
        intent.putExtras(bundle)
        mLUtils!!.startActivityWithTransition(intent)
    }

    private fun updateChooseList() {
        Thread(
                Runnable {
                    val mDao = RisuscitoDatabase.getInstance(context!!).consegnatiDao()
                    val canti = mDao.choosen
                    @Suppress("SENSELESS_COMPARISON")
                    @Suppress("SENSELESS_COMPARISON")
                    if (canti != null && mCantiViewModel!!.titoliChoose.isEmpty()) {
                        val newList = ArrayList<CheckableItem>()
                        for (canto in canti) {
                            val checkableItem = CheckableItem()
                            newList.add(
                                    checkableItem
                                            .withTitle(resources.getString(LUtils.getResId(canto.titolo, R.string::class.java)))
                                            .withPage(resources.getString(LUtils.getResId(canto.pagina, R.string::class.java)))
                                            .withColor(canto.color!!)
                                            .withSetSelected(canto.consegnato > 0)
                                            .withId(canto.id)
                            )
                        }
                        mCantiViewModel!!.titoliChoose = newList.sortedWith(compareBy { it.title.toString() })
                        FastAdapterDiffUtil.set(selectableAdapter!!, mCantiViewModel!!.titoliChoose)
                    }
                })
                .start()
    }

    private fun getFab(): FloatingActionButton {
//        return if (mMainActivity!!.isOnTablet) fab_pager else mMainActivity!!.getFab()
        return mMainActivity!!.getFab()
    }

    private fun enableBottombar(enabled: Boolean) {
        if (mMainActivity!!.isOnTablet)
            mBottomBar!!.visibility = if (enabled) View.VISIBLE else View.GONE
        else
            mMainActivity!!.enableBottombar(enabled)
    }

    private fun enableFab(enabled: Boolean) {
        mMainActivity!!.enableFab(enabled)
    }

    private fun initFab() {
        val icon = IconicsDrawable(activity!!)
                .icon(CommunityMaterial.Icon.cmd_pencil)
                .color(Color.WHITE)
                .sizeDp(24)
                .paddingDp(2)
        val onClick = View.OnClickListener {
            mCantiViewModel!!.editMode = true
            updateChooseList()
            selected_view!!.visibility = View.INVISIBLE
            chooseRecycler!!.visibility = View.VISIBLE
            enableBottombar(true)
            enableFab(false)
            val mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
            if (!mSharedPrefs.getBoolean(Utility.INTRO_CONSEGNATI_2, false)) {
                managerIntro()
            }
        }
//        if (mMainActivity!!.isOnTablet) {
//            fab_pager.setImageDrawable(icon)
//            fab_pager.setOnClickListener(onClick)
//        } else
        mMainActivity!!.initFab(icon, onClick)
    }

    override fun onPositive(tag: String) {}

    override fun onNegative(tag: String) {}

    override fun onNeutral(tag: String) {}

    private fun fabIntro() {
        TapTargetView.showFor(
                activity!!, // `this` is an Activity
                TapTarget.forView(
                        getFab(),
                        getString(R.string.title_activity_consegnati),
                        getString(R.string.showcase_consegnati_howto))
                        .outerCircleColorInt(
                                themeUtils.primaryColor()) // Specify a color for the outer circle
                        .targetCircleColorInt(Color.WHITE) // Specify a color for the target circle
                        .textTypeface(mRegularFont) // Specify a typeface for the text
                        .titleTextColor(R.color.primary_text_default_material_dark)
                        .textColor(R.color.secondary_text_default_material_dark)
                        .tintTarget(false) // Whether to tint the target view's color
                ,
                object : TapTargetView.Listener() { // The listener can listen for regular clicks, long clicks or cancels
                    override fun onTargetDismissed(view: TapTargetView?, userInitiated: Boolean) {
                        super.onTargetDismissed(view, userInitiated)
                        PreferenceManager.getDefaultSharedPreferences(activity).edit { putBoolean(Utility.INTRO_CONSEGNATI, true) }
                    }
                })
    }

    private fun managerIntro() {
        TapTargetSequence(activity!!)
                .continueOnCancel(true)
                .targets(
                        TapTarget.forToolbarMenuItem(
                                mBottomBar,
                                R.id.confirm_changes,
                                getString(R.string.title_activity_consegnati),
                                getString(R.string.showcase_consegnati_confirm))
                                .outerCircleColorInt(
                                        themeUtils.primaryColor()) // Specify a color for the outer circle
                                .targetCircleColorInt(Color.WHITE) // Specify a color for the target circle
                                .textTypeface(mRegularFont) // Specify a typeface for the text
                                .titleTextColor(R.color.primary_text_default_material_dark)
                                .textColor(R.color.secondary_text_default_material_dark),
                        TapTarget.forToolbarMenuItem(
                                mBottomBar,
                                R.id.cancel_change,
                                getString(R.string.title_activity_consegnati),
                                getString(R.string.showcase_consegnati_cancel))
                                .outerCircleColorInt(
                                        themeUtils.primaryColor()) // Specify a color for the outer circle
                                .targetCircleColorInt(Color.WHITE) // Specify a color for the target circle
                                .textTypeface(mRegularFont) // Specify a typeface for the text
                                .titleTextColor(R.color.primary_text_default_material_dark)
                                .textColor(R.color.secondary_text_default_material_dark))
                .listener(
                        object : TapTargetSequence.Listener { // The listener can listen for regular clicks, long clicks or cancels
                            override fun onSequenceFinish() {
                                PreferenceManager.getDefaultSharedPreferences(activity).edit { putBoolean(Utility.INTRO_CONSEGNATI_2, true) }
                            }

                            override fun onSequenceStep(tapTarget: TapTarget, b: Boolean) {}

                            override fun onSequenceCanceled(tapTarget: TapTarget) {
                                PreferenceManager.getDefaultSharedPreferences(activity).edit { putBoolean(Utility.INTRO_CONSEGNATI_2, true) }
                            }
                        })
                .start()
    }

    private fun populateDb() {
        mCantiViewModel!!.createDb()
    }

    private fun subscribeUiConsegnati() {
        mCantiViewModel!!
                .indexResult
                .observe(
                        this,
                        Observer { cantos ->
                            Log.d(TAG, "onChanged: " + (cantos != null))
                            if (cantos != null) {
                                val newList = ArrayList<SimpleItem>()
                                for (canto in cantos) {
                                    val sampleItem = SimpleItem()
                                    sampleItem
                                            .withTitle(resources.getString(LUtils.getResId(canto.titolo, R.string::class.java)))
                                            .withPage(resources.getString(LUtils.getResId(canto.pagina, R.string::class.java)))
                                            .withSource(resources.getString(LUtils.getResId(canto.source, R.string::class.java)))
                                            .withColor(canto.color!!)
                                            .withId(canto.id)
                                    newList.add(sampleItem)
                                }
                                mCantiViewModel!!.titoli = newList.sortedWith(compareBy { it.title.toString() })
                                cantoAdapter.set(mCantiViewModel!!.titoli)
                                no_consegnati!!.visibility = if (cantoAdapter.adapterItemCount > 0) View.INVISIBLE else View.VISIBLE
                            }
                        })
    }

    companion object {
        private val TAG = ConsegnatiFragment::class.java.canonicalName
    }
}
