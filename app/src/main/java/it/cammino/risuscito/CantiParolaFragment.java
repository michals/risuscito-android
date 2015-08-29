package it.cammino.risuscito;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.alexkolpa.fabtoolbar.FabToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.cammino.risuscito.adapters.PosizioneRecyclerAdapter;
import it.cammino.risuscito.objects.PosizioneItem;
import it.cammino.risuscito.objects.PosizioneTitleItem;
import it.cammino.risuscito.utils.ThemeUtils;

public class CantiParolaFragment extends Fragment {

    private int posizioneDaCanc;
    //    private String titoloDaCanc;
    private int idDaCanc;
    private String timestampDaCanc;
    private View rootView;
    private ShareActionProvider mShareActionProvider;
    private DatabaseCanti listaCanti;
    private SQLiteDatabase db;
    public ActionMode mMode;
    private boolean mSwhitchMode;
    //    private View mActionModeView;
//    private List<PosizioneItem> posizioniList;
    private List<Pair<PosizioneTitleItem, List<PosizioneItem>>> posizioniList;
    private int longclickedPos, longClickedChild;
    private RecyclerView recyclerView;
    private PosizioneRecyclerAdapter cantoAdapter;
    private boolean actionModeOk;
//	private int prevOrientation;

    private long mLastClickTime = 0;

    public static final int TAG_INSERT_PAROLA = 333;

    private LUtils mLUtils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(
                R.layout.activity_lista_personalizzata, container, false);

        //crea un istanza dell'oggetto DatabaseCanti
        listaCanti = new DatabaseCanti(getActivity());
//		updateLista();

        rootView.findViewById(R.id.button_pulisci).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.i(getClass().toString(), "cantiparola");
                db = listaCanti.getReadableDatabase();
                String sql = "DELETE FROM CUST_LISTS" +
                        " WHERE _id =  1 ";
                db.execSQL(sql);
                db.close();
                updateLista();
                mShareActionProvider.setShareIntent(getDefaultIntent());
            }
        });

//        ((ObservableScrollView) rootView.findViewById(R.id.parolaScrollView)).setScrollViewCallbacks(new ObservableScrollViewCallbacks() {
//            @Override
//            public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {}
//
//            @Override
//            public void onDownMotionEvent() {}
//
//            @Override
//            public void onUpOrCancelMotionEvent(ScrollState scrollState) {
//                FloatingActionsMenu fab1 = ((CustomLists) getParentFragment()).getFab1();
////                Log.i(getClass().toString(), "scrollState: " + scrollState);
//                if (scrollState == ScrollState.UP) {
//                    if (fab1.isVisible())
//                        fab1.hide();
//                } else if (scrollState == ScrollState.DOWN) {
//                    if (!fab1.isVisible())
//                        fab1.show();
//                }
//            }
//        });

        mLUtils = LUtils.getInstance(getActivity());
        mMode = null;
        mSwhitchMode = false;

        updateLista();

        OnClickListener click = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
                    return;
                mLastClickTime = SystemClock.elapsedRealtime();
                View parent = (View) v.getParent().getParent();
                if (parent.findViewById(R.id.addCantoGenerico).getVisibility() == View.VISIBLE) {
                    if (mSwhitchMode)
                        scambioConVuoto(parent, Integer.valueOf(((TextView) parent.findViewById(R.id.text_id_posizione)).getText().toString()));
                    else {
                        if (mMode == null) {
                            Bundle bundle = new Bundle();
                            bundle.putInt("fromAdd", 1);
                            bundle.putInt("idLista", 1);
                            bundle.putInt("position", Integer.valueOf(((TextView) parent.findViewById(R.id.text_id_posizione)).getText().toString()));
                            startSubActivity(bundle);
                        }
                    }
                }
                else {
                    if (!mSwhitchMode)
                        if (mMode != null) {
                            posizioneDaCanc = Integer.valueOf(((TextView) parent.findViewById(R.id.text_id_posizione)).getText().toString());
                            idDaCanc = Integer.valueOf(((TextView) v.findViewById(R.id.text_id_canto)).getText().toString());
                            timestampDaCanc = ((TextView) v.findViewById(R.id.text_timestamp)).getText().toString();
                            snackBarRimuoviCanto(v);
                        }
                        else
                            openPagina(v);
                    else {
                        scambioCanto(v, Integer.valueOf(((TextView) parent.findViewById(R.id.text_id_posizione)).getText().toString()));
                    }
                }
            }
        };

        OnLongClickListener longClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                View parent = (View) v.getParent().getParent();
                posizioneDaCanc = Integer.valueOf(((TextView) parent.findViewById(R.id.text_id_posizione)).getText().toString());
                idDaCanc = Integer.valueOf(((TextView) v.findViewById(R.id.text_id_canto)).getText().toString());
                timestampDaCanc = ((TextView) v.findViewById(R.id.text_timestamp)).getText().toString();
                snackBarRimuoviCanto(v);
                return true;
            }
        };

        recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_list);

        // Creating new adapter object
        cantoAdapter = new PosizioneRecyclerAdapter(getActivity(), posizioniList, click, longClick);
        recyclerView.setAdapter(cantoAdapter);

        // Setting the layoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
//            ((CustomLists) getParentFragment()).fabDelete.setEnabled(false);
//            ((CustomLists) getParentFragment()).fabEdit.setEnabled(false);
//            if (LUtils.hasHoneycomb()) {
            ((CustomLists) getParentFragment()).fabDelete.setVisibility(View.GONE);
            ((CustomLists) getParentFragment()).fabEdit.setVisibility(View.GONE);
//            }
            FabToolbar fab1 = ((CustomLists) getParentFragment()).getFab();
//            if (!fab1.isShowing())
            fab1.scrollUp();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

//    @Override
//    public void onResume() {
////    	Log.i("CANTI PAROLA", "ON RESUME");
//        super.onResume();
//        updateLista();
//        if (mMode != null && mActionModeView != null)
//            mActionModeView.setBackgroundColor(getThemeUtils().accentColorLight());
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.i(getClass().getName(), "requestCode: " + requestCode);
        if (requestCode == TAG_INSERT_PAROLA && resultCode == Activity.RESULT_OK) {
            updateLista();
            cantoAdapter.notifyDataSetChanged();
            mShareActionProvider.setShareIntent(getDefaultIntent());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        if (listaCanti != null)
            listaCanti.close();
        if (mMode != null)
            mMode.finish();
        super.onDestroy();
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem shareItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        ViewPager tempPager = (ViewPager) getActivity().findViewById(R.id.view_pager);
        if (mShareActionProvider != null && tempPager.getCurrentItem() == 0)
            mShareActionProvider.setShareIntent(getDefaultIntent());
    }

    private Intent getDefaultIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getTitlesList());
        intent.setType("text/plain");
        return intent;
    }

//    private void updateLista() {
//
//        String titoloCanto = getTitoloFromPosition(1);
//
//        if (titoloCanto.equalsIgnoreCase("")) {
//            rootView.findViewById(R.id.addCantoIniziale).setVisibility(View.VISIBLE);
//            rootView.findViewById(R.id.cantoInizialeContainer).setVisibility(View.GONE);
//            rootView.findViewById(R.id.addCantoIniziale).setOnClickListener(new OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                        return;
//                    mLastClickTime = SystemClock.elapsedRealtime();
//                    if (mSwhitchMode)
//                        scambioConVuoto(1);
//                    else {
//                        if (mMode == null) {
//                            Bundle bundle = new Bundle();
//                            bundle.putInt("fromAdd", 1);
//                            bundle.putInt("idLista", 1);
//                            bundle.putInt("position", 1);
//                            startSubActivity(bundle);
//                        }
//                    }
//                }
//            });
//        }
//        else {
//            rootView.findViewById(R.id.addCantoIniziale).setVisibility(View.GONE);
//            View view = rootView.findViewById(R.id.cantoInizialeContainer);
//            view.setVisibility(View.VISIBLE);
//            view.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                        return;
//                    mLastClickTime = SystemClock.elapsedRealtime();
////                    openPagina(v, R.id.cantoInizialeText);
//                    if (!mSwhitchMode)
//                        if (mMode != null) {
//                            posizioneDaCanc = 1;
//                            titoloDaCanc = Utility.duplicaApostrofi(((TextView) view.findViewById(R.id.cantoInizialeText)).getText().toString());
//                            snackBarRimuoviCanto(view);
//                        }
//                        else
//                            openPagina(view, R.id.cantoInizialeText);
//                    else {
//                        scambioCanto(view, R.id.cantoInizialeText, 1);
//                    }
//                }
//            });
//            view.setOnLongClickListener(new OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View view) {
//                    posizioneDaCanc = 1;
//                    titoloDaCanc = Utility.duplicaApostrofi(((TextView) rootView.findViewById(R.id.cantoInizialeText)).getText().toString());
//                    snackBarRimuoviCanto(view);
//                    return true;
//                }
//            });
//
//            TextView temp = (TextView) view.findViewById(R.id.cantoInizialeText);
//            temp.setText(titoloCanto.substring(10));
//
//            int tempPagina = Integer.valueOf(titoloCanto.substring(0,3));
//            String pagina = String.valueOf(tempPagina);
//            TextView textPage = (TextView) view.findViewById(R.id.cantoInizialePage);
//            textPage.setText(pagina);
//
//            String colore = titoloCanto.substring(3, 10);
//            if (colore.equalsIgnoreCase(Utility.GIALLO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_yellow);
//            if (colore.equalsIgnoreCase(Utility.GRIGIO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_grey);
//            if (colore.equalsIgnoreCase(Utility.VERDE))
//                textPage.setBackgroundResource(R.drawable.bkg_round_green);
//            if (colore.equalsIgnoreCase(Utility.AZZURRO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_blue);
//            if (colore.equalsIgnoreCase(Utility.BIANCO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_white);
//        }
//
//        titoloCanto = getTitoloFromPosition(2);
//
//        if (titoloCanto.equalsIgnoreCase("")) {
//            rootView.findViewById(R.id.addPrimaLettura).setVisibility(View.VISIBLE);
//            rootView.findViewById(R.id.primaLetturaContainer).setVisibility(View.GONE);
//            rootView.findViewById(R.id.addPrimaLettura).setOnClickListener(new OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                        return;
//                    mLastClickTime = SystemClock.elapsedRealtime();
//                    if (mSwhitchMode)
//                        scambioConVuoto(2);
//                    else {
//                        if (mMode == null) {
//                            Bundle bundle = new Bundle();
//                            bundle.putInt("fromAdd", 1);
//                            bundle.putInt("idLista", 1);
//                            bundle.putInt("position", 2);
//                            startSubActivity(bundle);
//                        }
//                    }
//                }
//            });
//        }
//        else {
//            rootView.findViewById(R.id.addPrimaLettura).setVisibility(View.GONE);
//            View view = rootView.findViewById(R.id.primaLetturaContainer);
//            view.setVisibility(View.VISIBLE);
//            view.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View view) {
//
////                    openPagina(v, R.id.primaLetturaText);
//                    if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                        return;
//                    mLastClickTime = SystemClock.elapsedRealtime();
//                    if (!mSwhitchMode)
//                        if (mMode != null) {
//                            posizioneDaCanc = 2;
//                            titoloDaCanc = Utility.duplicaApostrofi(((TextView) view.findViewById(R.id.primaLetturaText)).getText().toString());
//                            snackBarRimuoviCanto(view);
//                        }
//                        else
//                            openPagina(view, R.id.primaLetturaText);
//                    else {
//                        scambioCanto(view, R.id.primaLetturaText, 2);
//                    }
//                }
//            });
//            view.setOnLongClickListener(new OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View view) {
//                    posizioneDaCanc = 2;
//                    titoloDaCanc = Utility.duplicaApostrofi(((TextView) rootView.findViewById(R.id.primaLetturaText)).getText().toString());
//                    snackBarRimuoviCanto(view);
//                    return true;
//                }
//            });
//
//            TextView temp = (TextView) view.findViewById(R.id.primaLetturaText);
//            temp.setText(titoloCanto.substring(10));
//
//            int tempPagina = Integer.valueOf(titoloCanto.substring(0,3));
//            String pagina = String.valueOf(tempPagina);
//            TextView textPage = (TextView) view.findViewById(R.id.primaLetturaPage);
//            textPage.setText(pagina);
//
//            String colore = titoloCanto.substring(3, 10);
//            if (colore.equalsIgnoreCase(Utility.GIALLO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_yellow);
//            if (colore.equalsIgnoreCase(Utility.GRIGIO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_grey);
//            if (colore.equalsIgnoreCase(Utility.VERDE))
//                textPage.setBackgroundResource(R.drawable.bkg_round_green);
//            if (colore.equalsIgnoreCase(Utility.AZZURRO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_blue);
//            if (colore.equalsIgnoreCase(Utility.BIANCO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_white);
//        }
//
//        titoloCanto = getTitoloFromPosition(3);
//
//        if (titoloCanto.equalsIgnoreCase("")) {
//            rootView.findViewById(R.id.addSecondaLettura).setVisibility(View.VISIBLE);
//            rootView.findViewById(R.id.secondaLetturaContainer).setVisibility(View.GONE);
//            rootView.findViewById(R.id.addSecondaLettura).setOnClickListener(new OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                        return;
//                    mLastClickTime = SystemClock.elapsedRealtime();
//                    if (mSwhitchMode)
//                        scambioConVuoto(3);
//                    else {
//                        if (mMode == null) {
//                            Bundle bundle = new Bundle();
//                            bundle.putInt("fromAdd", 1);
//                            bundle.putInt("idLista", 1);
//                            bundle.putInt("position", 3);
//                            startSubActivity(bundle);
//                        }
//                    }
//                }
//            });
//        }
//        else {
//            rootView.findViewById(R.id.addSecondaLettura).setVisibility(View.GONE);
//            View view = rootView.findViewById(R.id.secondaLetturaContainer);
//            view.setVisibility(View.VISIBLE);
//            view.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                        return;
//                    mLastClickTime = SystemClock.elapsedRealtime();
////                    openPagina(v, R.id.secondaLetturaText);
//                    if (!mSwhitchMode)
//                        if (mMode != null) {
//                            posizioneDaCanc = 3;
//                            titoloDaCanc = Utility.duplicaApostrofi(((TextView) view.findViewById(R.id.secondaLetturaText)).getText().toString());
//                            snackBarRimuoviCanto(view);
//                        }
//                        else
//                            openPagina(view, R.id.secondaLetturaText);
//                    else {
//                        scambioCanto(view, R.id.secondaLetturaText, 3);
//                    }
//                }
//            });
//            view.setOnLongClickListener(new OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View view) {
//                    posizioneDaCanc = 3;
//                    titoloDaCanc = Utility.duplicaApostrofi(((TextView) rootView.findViewById(R.id.secondaLetturaText)).getText().toString());
//                    snackBarRimuoviCanto(view);
//                    return true;
//                }
//            });
//
//            TextView temp = (TextView) view.findViewById(R.id.secondaLetturaText);
//            temp.setText(titoloCanto.substring(10));
//
//            int tempPagina = Integer.valueOf(titoloCanto.substring(0,3));
//            String pagina = String.valueOf(tempPagina);
//            TextView textPage = (TextView) view.findViewById(R.id.secondaLetturaPage);
//            textPage.setText(pagina);
//
//            String colore = titoloCanto.substring(3, 10);
//            if (colore.equalsIgnoreCase(Utility.GIALLO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_yellow);
//            if (colore.equalsIgnoreCase(Utility.GRIGIO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_grey);
//            if (colore.equalsIgnoreCase(Utility.VERDE))
//                textPage.setBackgroundResource(R.drawable.bkg_round_green);
//            if (colore.equalsIgnoreCase(Utility.AZZURRO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_blue);
//            if (colore.equalsIgnoreCase(Utility.BIANCO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_white);
//        }
//
//        titoloCanto = getTitoloFromPosition(4);
//
//        if (titoloCanto.equalsIgnoreCase("")) {
//            rootView.findViewById(R.id.addTerzaLettura).setVisibility(View.VISIBLE);
//            rootView.findViewById(R.id.terzaLetturaContainer).setVisibility(View.GONE);
//            rootView.findViewById(R.id.addTerzaLettura).setOnClickListener(new OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                        return;
//                    mLastClickTime = SystemClock.elapsedRealtime();
//                    if (mSwhitchMode)
//                        scambioConVuoto(4);
//                    else {
//                        if (mMode == null) {
//                            Bundle bundle = new Bundle();
//                            bundle.putInt("fromAdd", 1);
//                            bundle.putInt("idLista", 1);
//                            bundle.putInt("position", 4);
//                            startSubActivity(bundle);
//                        }
//                    }
//                }
//            });
//        }
//        else {
//            rootView.findViewById(R.id.addTerzaLettura).setVisibility(View.GONE);
//            View view = rootView.findViewById(R.id.terzaLetturaContainer);
//            view.setVisibility(View.VISIBLE);
//            view.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                        return;
//                    mLastClickTime = SystemClock.elapsedRealtime();
////                    openPagina(v, R.id.terzaLetturaText);
//                    if (!mSwhitchMode)
//                        if (mMode != null) {
//                            posizioneDaCanc = 4;
//                            titoloDaCanc = Utility.duplicaApostrofi(((TextView) view.findViewById(R.id.terzaLetturaText)).getText().toString());
//                            snackBarRimuoviCanto(view);
//                        }
//                        else
//                            openPagina(view, R.id.terzaLetturaText);
//                    else {
//                        scambioCanto(view, R.id.terzaLetturaText, 4);
//                    }
//                }
//            });
//            view.setOnLongClickListener(new OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View view) {
//                    posizioneDaCanc = 4;
//                    titoloDaCanc = Utility.duplicaApostrofi(((TextView) rootView.findViewById(R.id.terzaLetturaText)).getText().toString());
//                    snackBarRimuoviCanto(view);
//                    return true;
//                }
//            });
//
//            TextView temp = (TextView) view.findViewById(R.id.terzaLetturaText);
//            temp.setText(titoloCanto.substring(10));
//
//            int tempPagina = Integer.valueOf(titoloCanto.substring(0,3));
//            String pagina = String.valueOf(tempPagina);
//            TextView textPage = (TextView) view.findViewById(R.id.terzaLetturaPage);
//            textPage.setText(pagina);
//
//            String colore = titoloCanto.substring(3, 10);
//            if (colore.equalsIgnoreCase(Utility.GIALLO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_yellow);
//            if (colore.equalsIgnoreCase(Utility.GRIGIO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_grey);
//            if (colore.equalsIgnoreCase(Utility.VERDE))
//                textPage.setBackgroundResource(R.drawable.bkg_round_green);
//            if (colore.equalsIgnoreCase(Utility.AZZURRO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_blue);
//            if (colore.equalsIgnoreCase(Utility.BIANCO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_white);
//        }
//
//        SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(getActivity());
//
//        if (pref.getBoolean(Utility.SHOW_PACE, false)) {
//
//            rootView.findViewById(R.id.groupCantoPace).setVisibility(View.VISIBLE);
//
//            titoloCanto = getTitoloFromPosition(6);
//
//            if (titoloCanto.equalsIgnoreCase("")) {
//                rootView.findViewById(R.id.addCantoPace).setVisibility(View.VISIBLE);
//                rootView.findViewById(R.id.cantoPaceContainer).setVisibility(View.GONE);
//                rootView.findViewById(R.id.addCantoPace).setOnClickListener(new OnClickListener() {
//
//                    @Override
//                    public void onClick(View v) {
//                        if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                            return;
//                        mLastClickTime = SystemClock.elapsedRealtime();
//                        if (mSwhitchMode)
//                            scambioConVuoto(6);
//                        else {
//                            if (mMode == null) {
//                                Bundle bundle = new Bundle();
//                                bundle.putInt("fromAdd", 1);
//                                bundle.putInt("idLista", 1);
//                                bundle.putInt("position", 6);
//                                startSubActivity(bundle);
//                            }
//                        }
//                    }
//                });
//            }
//            else {
//                rootView.findViewById(R.id.addCantoPace).setVisibility(View.GONE);
//                View view = rootView.findViewById(R.id.cantoPaceContainer);
//                view.setVisibility(View.VISIBLE);
//                view.setOnClickListener(new OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                            return;
//                        mLastClickTime = SystemClock.elapsedRealtime();
////                        openPagina(v, R.id.cantoPaceText);
//                        if (!mSwhitchMode)
//                            if (mMode != null) {
//                                posizioneDaCanc = 6;
//                                titoloDaCanc = Utility.duplicaApostrofi(((TextView) view.findViewById(R.id.cantoPaceText)).getText().toString());
//                                snackBarRimuoviCanto(view);
//                            }
//                            else
//                                openPagina(view, R.id.cantoPaceText);
//                        else {
//                            scambioCanto(view, R.id.cantoPaceText, 6);
//                        }
//                    }
//                });
//                view.setOnLongClickListener(new OnLongClickListener() {
//                    @Override
//                    public boolean onLongClick(View view) {
//                        posizioneDaCanc = 6;
//                        titoloDaCanc = Utility.duplicaApostrofi(((TextView) rootView.findViewById(R.id.cantoPaceText)).getText().toString());
//                        snackBarRimuoviCanto(view);
//                        return true;
//                    }
//                });
//
//                TextView temp = (TextView) view.findViewById(R.id.cantoPaceText);
//                temp.setText(titoloCanto.substring(10));
//
//                int tempPagina = Integer.valueOf(titoloCanto.substring(0,3));
//                String pagina = String.valueOf(tempPagina);
//                TextView textPage = (TextView) view.findViewById(R.id.cantoPacePage);
//                textPage.setText(pagina);
//
//                String colore = titoloCanto.substring(3, 10);
//                if (colore.equalsIgnoreCase(Utility.GIALLO))
//                    textPage.setBackgroundResource(R.drawable.bkg_round_yellow);
//                if (colore.equalsIgnoreCase(Utility.GRIGIO))
//                    textPage.setBackgroundResource(R.drawable.bkg_round_grey);
//                if (colore.equalsIgnoreCase(Utility.VERDE))
//                    textPage.setBackgroundResource(R.drawable.bkg_round_green);
//                if (colore.equalsIgnoreCase(Utility.AZZURRO))
//                    textPage.setBackgroundResource(R.drawable.bkg_round_blue);
//                if (colore.equalsIgnoreCase(Utility.BIANCO))
//                    textPage.setBackgroundResource(R.drawable.bkg_round_white);
//            }
//        }
//        else
//            rootView.findViewById(R.id.groupCantoPace).setVisibility(View.GONE);
//
//        titoloCanto = getTitoloFromPosition(5);
//
//        if (titoloCanto.equalsIgnoreCase("")) {
//            rootView.findViewById(R.id.addCantoFinale).setVisibility(View.VISIBLE);
//            rootView.findViewById(R.id.cantoFinaleContainer).setVisibility(View.GONE);
//            rootView.findViewById(R.id.addCantoFinale).setOnClickListener(new OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                        return;
//                    mLastClickTime = SystemClock.elapsedRealtime();
//                    if (mSwhitchMode)
//                        scambioConVuoto(5);
//                    else {
//                        if (mMode == null) {
//                            Bundle bundle = new Bundle();
//                            bundle.putInt("fromAdd", 1);
//                            bundle.putInt("idLista", 1);
//                            bundle.putInt("position", 5);
//                            startSubActivity(bundle);
//                        }
//                    }
//                }
//            });
//        }
//        else {
//            rootView.findViewById(R.id.addCantoFinale).setVisibility(View.GONE);
//            View view = rootView.findViewById(R.id.cantoFinaleContainer);
//            view.setVisibility(View.VISIBLE);
//            view.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                        return;
//                    mLastClickTime = SystemClock.elapsedRealtime();
////                    openPagina(v, R.id.cantoFinaleText);
//                    if (!mSwhitchMode)
//                        if (mMode != null) {
//                            posizioneDaCanc = 5;
//                            titoloDaCanc = Utility.duplicaApostrofi(((TextView) view.findViewById(R.id.cantoFinaleText)).getText().toString());
//                            snackBarRimuoviCanto(view);
//                        }
//                        else
//                            openPagina(view, R.id.cantoFinaleText);
//                    else {
//                        scambioCanto(view, R.id.cantoFinaleText, 5);
//                    }
//                }
//            });
//            view.setOnLongClickListener(new OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View view) {
//                    posizioneDaCanc = 5;
//                    titoloDaCanc = Utility.duplicaApostrofi(((TextView) rootView.findViewById(R.id.cantoFinaleText)).getText().toString());
//                    snackBarRimuoviCanto(view);
//                    return true;
//                }
//            });
//
//            TextView temp = (TextView) view.findViewById(R.id.cantoFinaleText);
//            temp.setText(titoloCanto.substring(10));
//
//            int tempPagina = Integer.valueOf(titoloCanto.substring(0,3));
//            String pagina = String.valueOf(tempPagina);
//            TextView textPage = (TextView) view.findViewById(R.id.cantoFinalePage);
//            textPage.setText(pagina);
//
//            String colore = titoloCanto.substring(3, 10);
//            if (colore.equalsIgnoreCase(Utility.GIALLO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_yellow);
//            if (colore.equalsIgnoreCase(Utility.GRIGIO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_grey);
//            if (colore.equalsIgnoreCase(Utility.VERDE))
//                textPage.setBackgroundResource(R.drawable.bkg_round_green);
//            if (colore.equalsIgnoreCase(Utility.AZZURRO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_blue);
//            if (colore.equalsIgnoreCase(Utility.BIANCO))
//                textPage.setBackgroundResource(R.drawable.bkg_round_white);
//        }
//
//    }

    private void updateLista() {

        if (posizioniList == null)
            posizioniList = new ArrayList<>();
        else
            posizioniList.clear();

//        OnClickListener click = new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (SystemClock.elapsedRealtime() - mLastClickTime < Utility.CLICK_DELAY)
//                    return;
//                mLastClickTime = SystemClock.elapsedRealtime();
//                View parent = (View) v.getParent().getParent();
//                if (parent.findViewById(R.id.addCantoGenerico).getVisibility() == View.VISIBLE) {
//                    if (mSwhitchMode)
//                        scambioConVuoto(Integer.valueOf(((TextView) parent.findViewById(R.id.text_id_posizione)).getText().toString()));
//                    else {
//                        if (mMode == null) {
//                            Bundle bundle = new Bundle();
//                            bundle.putInt("fromAdd", 1);
//                            bundle.putInt("idLista", 1);
//                            bundle.putInt("position", Integer.valueOf(((TextView) parent.findViewById(R.id.text_id_posizione)).getText().toString()));
//                            startSubActivity(bundle);
//                        }
//                    }
//                }
//                else {
//                    if (!mSwhitchMode)
//                        if (mMode != null) {
//                            posizioneDaCanc = Integer.valueOf(((TextView) parent.findViewById(R.id.text_id_posizione)).getText().toString());
//                            idDaCanc = Integer.valueOf(((TextView) v.findViewById(R.id.text_id_canto)).getText().toString());
//                            timestampDaCanc = ((TextView) v.findViewById(R.id.text_timestamp)).getText().toString();
//                            snackBarRimuoviCanto(v);
//                        }
//                        else
//                            openPagina(v);
//                    else {
//                        scambioCanto(v, Integer.valueOf(((TextView) parent.findViewById(R.id.text_id_posizione)).getText().toString()));
//                    }
//                }
//            }
//        };
//
//        OnLongClickListener longClick = new OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                View parent = (View) v.getParent().getParent();
//                posizioneDaCanc = Integer.valueOf(((TextView) parent.findViewById(R.id.text_id_posizione)).getText().toString());
//                idDaCanc = Integer.valueOf(((TextView) v.findViewById(R.id.text_id_canto)).getText().toString());
//                timestampDaCanc = ((TextView) v.findViewById(R.id.text_timestamp)).getText().toString();
//                snackBarRimuoviCanto(v);
//                return true;
//            }
//        };

        posizioniList.add(getCantofromPosition(getString(R.string.canto_iniziale), 1, 0));
        posizioniList.add(getCantofromPosition(getString(R.string.prima_lettura), 2, 1));
        posizioniList.add(getCantofromPosition(getString(R.string.seconda_lettura), 3, 2));
        posizioniList.add(getCantofromPosition(getString(R.string.terza_lettura), 4, 3));

        SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (pref.getBoolean(Utility.SHOW_PACE, false)) {
            posizioniList.add(getCantofromPosition(getString(R.string.canto_pace), 6, 4));
            posizioniList.add(getCantofromPosition(getString(R.string.canto_fine), 5, 5));
        }
        else
            posizioniList.add(getCantofromPosition(getString(R.string.canto_fine), 5, 4));

//        recyclerView = (RecyclerView) rootView.findViewById(R.id.parolaList);
//
//        // Creating new adapter object
//        cantoAdapter = new PosizioneRecyclerAdapter(getActivity(), posizioniList, click, longClick);
//        recyclerView.setAdapter(cantoAdapter);
//
//        // Setting the layoutManager
//        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    }

    private void startSubActivity(Bundle bundle) {
        Intent intent = new Intent(getActivity(), GeneralInsertSearch.class);
        intent.putExtras(bundle);
        getParentFragment().startActivityForResult(intent, TAG_INSERT_PAROLA);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.hold_on);
    }

    private void openPagina(View v) {
        // recupera il titolo della voce cliccata
//        String cantoCliccato = ((TextView) v.findViewById(id)).getText().toString();
//        cantoCliccato = Utility.duplicaApostrofi(cantoCliccato);
//
//        // crea un manipolatore per il DB in modalit� READ
//        db = listaCanti.getReadableDatabase();
//
//        // esegue la query per il recupero del nome del file della pagina da visualizzare
//        String query = "SELECT source, _id" +
//                "  FROM ELENCO" +
//                "  WHERE titolo =  '" + cantoCliccato + "'";
//        Cursor cursor = db.rawQuery(query, null);
//
//        // recupera il nome del file
//        cursor.moveToFirst();
//        String pagina = cursor.getString(0);
//        int idCanto = cursor.getInt(1);
//
//        // chiude il cursore
//        cursor.close();
//        db.close();

        // crea un bundle e ci mette il parametro "pagina", contente il nome del file della pagina da visualizzare
        Bundle bundle = new Bundle();
        bundle.putString("pagina", ((TextView) v.findViewById(R.id.text_source_canto)).getText().toString());
        bundle.putInt("idCanto", Integer.valueOf(((TextView) v.findViewById(R.id.text_id_canto)).getText().toString()));

        Intent intent = new Intent(getActivity(), PaginaRenderActivity.class);
        intent.putExtras(bundle);
        mLUtils.startActivityWithTransition(intent, v, Utility.TRANS_PAGINA_RENDER);
    }

    //recupera il titolo del canto in posizione "position" nella lista
//    private String getTitoloFromPosition(int position) {
//
//        db = listaCanti.getReadableDatabase();
//
//        String query = "SELECT B.titolo, color, pagina" +
//                "  FROM CUST_LISTS A" +
//                "  	   , ELENCO B" +
//                "  WHERE A._id = 1" +
//                "  AND   A.position = " + position +
//                "  AND   A.id_canto = B._id";
//        Cursor cursor = db.rawQuery(query, null);
//
//        int total = cursor.getCount();
//        String result = "";
//
//        if (total == 1) {
//            cursor.moveToFirst();
////	    	result =  cursor.getString(1) + cursor.getString(0);
//            result =  Utility.intToString(cursor.getInt(2), 3) + cursor.getString(1) + cursor.getString(0);
//        }
//
//        cursor.close();
//        db.close();
//
//        return result;
//    }

    //recupera il titolo del canto in posizione "position" nella lista
    private Pair<PosizioneTitleItem, List<PosizioneItem>> getCantofromPosition(String titoloPosizione, int position, int tag) {

        db = listaCanti.getReadableDatabase();

        String query = "SELECT B.titolo, B.color, B.pagina, B.source, B._id, A.timestamp" +
                "  FROM CUST_LISTS A" +
                "  	   , ELENCO B" +
                "  WHERE A._id = 1" +
                "  AND   A.position = " + position +
                "  AND   A.id_canto = B._id";
        Cursor cursor = db.rawQuery(query, null);

        int total = cursor.getCount();

//        PosizioneItem result;

//        if (total == 0) {
//            cursor.moveToFirst();
//            result = new PosizioneItem(titoloPosizione
//                    , 1
//                    , position
//                    , cursor.getInt(2)
//                    , cursor.getString(0)
//                    , cursor.getString(1)
//                    , cursor.getInt(4)
//                    , cursor.getString(3)
//                    , cursor.getString(5)
//                    , tag);
//        }
//        else {
//            result = new PosizioneItem(titoloPosizione
//                    , 1
//                    , position
//                    , tag);
//        }
        List<PosizioneItem> list = new ArrayList<>();
        if (total > 0) {
            cursor.moveToFirst();

            list.add(new PosizioneItem(
                    cursor.getInt(2)
                    , cursor.getString(0)
                    , cursor.getString(1)
                    , cursor.getInt(4)
                    , cursor.getString(3)
                    , cursor.getString(5)));

            while (cursor.moveToNext()) {
                list.add(new PosizioneItem(
                        cursor.getInt(2)
                        , cursor.getString(0)
                        , cursor.getString(1)
                        , cursor.getInt(4)
                        , cursor.getString(3)
                        , cursor.getString(5)));
            }
        }

        Pair<PosizioneTitleItem, List<PosizioneItem>> result = new Pair(new PosizioneTitleItem(titoloPosizione
                , 1
                , position
                , tag
                , false), list);

        cursor.close();
        db.close();

        return result;

    }

    private String getTitlesList() {

        Locale l = getActivity().getResources().getConfiguration().locale;
        String result = "";
        String temp;

        //titolo
        result +=  "-- " + getString(R.string.title_activity_canti_parola).toUpperCase(l) + " --\n";

        //canto iniziale
        temp = getTitoloToSendFromPosition(0);

        result += getResources().getString(R.string.canto_iniziale).toUpperCase(l);
        result += "\n";

        if (temp.equalsIgnoreCase(""))
            result += ">> " + getString(R.string.to_be_chosen) + " <<";
        else
            result += temp;

        result += "\n";

        //prima lettura
        temp = getTitoloToSendFromPosition(1);

        result += getResources().getString(R.string.prima_lettura).toUpperCase(l);
        result += "\n";

        if (temp.equalsIgnoreCase(""))
            result += ">> " + getString(R.string.to_be_chosen) + " <<";
        else
            result += temp;

        result += "\n";

        //seconda lettura
        temp = getTitoloToSendFromPosition(2);

        result += getResources().getString(R.string.seconda_lettura).toUpperCase(l);
        result += "\n";

        if (temp.equalsIgnoreCase(""))
            result += ">> " + getString(R.string.to_be_chosen) + " <<";
        else
            result += temp;

        result += "\n";

        //terza lettura
        temp = getTitoloToSendFromPosition(3);

        result += getResources().getString(R.string.terza_lettura).toUpperCase(l);
        result += "\n";

        if (temp.equalsIgnoreCase(""))
            result += ">> " + getString(R.string.to_be_chosen) + " <<";
        else
            result += temp;

        result += "\n";

        //deve essere messo anche il canto alla pace? legge le impostazioni
        SharedPreferences pref =  PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (pref.getBoolean(Utility.SHOW_PACE, false)) {
            //canto alla pace
            temp = getTitoloToSendFromPosition(4);

            result += getResources().getString(R.string.canto_pace).toUpperCase(l);
            result += "\n";

            if (temp.equalsIgnoreCase(""))
                result += ">> " + getString(R.string.to_be_chosen) + " <<";
            else
                result += temp;

            result += "\n";

            //canto finale
            temp = getTitoloToSendFromPosition(5);

            result += getResources().getString(R.string.canto_fine).toUpperCase(l);
            result += "\n";

            if (temp.equalsIgnoreCase(""))
                result += ">> " + getString(R.string.to_be_chosen) + " <<";
            else
                result += temp;
        }
        else {
            //canto finale
            temp = getTitoloToSendFromPosition(4);

            result += getResources().getString(R.string.canto_fine).toUpperCase(l);
            result += "\n";

            if (temp.equalsIgnoreCase(""))
                result += ">> " + getString(R.string.to_be_chosen) + " <<";
            else
                result += temp;
        }
//		else
//			Log.i("CANTO ALLA PACE", "IGNORATO");

        //canto finale
//        temp = getTitoloToSendFromPosition(5);
//
//        result += getResources().getString(R.string.canto_fine).toUpperCase(l);
//        result += "\n";
//
//        if (temp.equalsIgnoreCase(""))
//            result += ">> " + getString(R.string.to_be_chosen) + " <<";
//        else
//            result += temp;

        return result;

    }

    //recupera il titolo del canto in posizione "position" nella lista "list"
    private String getTitoloToSendFromPosition(int position) {

//        db = listaCanti.getReadableDatabase();
//
//        String query = "SELECT B.titolo, B.pagina" +
//                "  FROM CUST_LISTS A" +
//                "  	   , ELENCO B" +
//                "  WHERE A._id = 1" +
//                "  AND   A.position = " + position +
//                "  AND   A.id_canto = B._id";
//        Cursor cursor = db.rawQuery(query, null);
//
//        int total = cursor.getCount();
        String result = "";

        List<PosizioneItem> items = posizioniList.get(position).second;

        if (items.size() > 0) {
            for (PosizioneItem tempItem: items) {
                result += tempItem.getTitolo() + " - " + getString(R.string.page_contracted) + tempItem.getPagina();
                result += "\n";
            }
        }

//        Log.i(getClass().getName(), "item[" + position + "]-->ismChoosen(): " + item.ismChoosen());
//        if (item.ismChoosen()) {
////            cursor.moveToFirst();
////            result =  cursor.getString(0) + " - " + getString(R.string.page_contracted) + cursor.getInt(1);
//            result =  item.getTitolo() + " - " + getString(R.string.page_contracted) + item.getPagina();
//        }

//        cursor.close();
//        db.close();

        return result;
    }

    public void snackBarRimuoviCanto(View view) {
//        Snackbar.make(getActivity().findViewById(R.id.main_content), R.string.list_remove, Snackbar.LENGTH_LONG)
//                .setAction(R.string.snackbar_remove, new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        db = listaCanti.getReadableDatabase();
//                       snackBarRimuoviCanto String sql = "DELETE FROM CUST_LISTS" +
//                                "  WHERE _id =  1 " +
//                                "    AND position = " + posizioneDaCanc +
//                                "	 AND id_canto = (SELECT _id FROM ELENCO" +
//                                "					WHERE titolo = '" + titoloDaCanc + "')";
//                        db.execSQL(sql);
//                        db.close();
//                        updateLista();
//                        mShareActionProvider.setShareIntent(getDefaultIntent());
//                    }
//                })
//                .setActionTextColor(getThemeUtils().accentColor())
//                .show();
        if (mMode != null)
            mMode.finish();
//        mActionModeView = view;
        View parent = (View) view.getParent().getParent();
        longclickedPos = Integer.valueOf(((TextView)parent.findViewById(R.id.tag)).getText().toString());
        longClickedChild = Integer.valueOf(((TextView)view.findViewById(R.id.item_tag)).getText().toString());
        mMode = ((AppCompatActivity) getActivity()).startSupportActionMode(new ModeCallback());
    }

    private ThemeUtils getThemeUtils() {
        return ((MainActivity)getActivity()).getThemeUtils();
    }

    private final class ModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Create the menu from the xml file
//            MenuInflater inflater = getActivity().getMenuInflater();
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
//                ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
//            mActionModeView.setBackgroundColor(getThemeUtils().accentColorLight());
            posizioniList.get(longclickedPos).second.get(longClickedChild).setmSelected(true);
            cantoAdapter.notifyItemChanged(longclickedPos);
            getActivity().getMenuInflater().inflate(R.menu.menu_actionmode_lists, menu);
            Drawable drawable = DrawableCompat.wrap(menu.findItem(R.id.action_remove_item).getIcon());
            DrawableCompat.setTint(drawable, getResources().getColor(R.color.icon_ative_black));
            menu.findItem(R.id.action_remove_item).setIcon(drawable);
            drawable = DrawableCompat.wrap(menu.findItem(R.id.action_switch_item).getIcon());
            DrawableCompat.setTint(drawable, getResources().getColor(R.color.icon_ative_black));
            menu.findItem(R.id.action_switch_item).setIcon(drawable);
            actionModeOk = false;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // Here, you can checked selected items to adapt available actions
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
//                ((AppCompatActivity)getActivity()).getSupportActionBar().show();
            mSwhitchMode = false;
//            TypedValue typedValue = new TypedValue();
//            Resources.Theme theme = getActivity().getTheme();
//            theme.resolveAttribute(R.attr.customSelector, typedValue, true);
//            mActionModeView.setBackgroundResource(typedValue.resourceId);
//            mActionModeView = null;
            if (!actionModeOk) {
//            if (posizioniList.get(longclickedPos).second.size() > 0)
                posizioniList.get(longclickedPos).second.get(longClickedChild).setmSelected(false);
                cantoAdapter.notifyItemChanged(longclickedPos);
            }
            if (mode == mMode)
                mMode = null;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch(item.getItemId()) {
                case R.id.action_remove_item:
                    db = listaCanti.getReadableDatabase();
//                    String sql = "SELECT id_canto, timestamp" +
//                            "   FROM CUST_LISTS" +
//                            "  WHERE _id =  1 " +
//                            "    AND position = " + posizioneDaCanc +
////                            "	 AND id_canto = (SELECT _id FROM ELENCO" +
////                            "					WHERE titolo = '" + titoloDaCanc + "')";
//                    Cursor cursor = db.rawQuery(sql, null);
//                    cursor.moveToFirst();
//                    idDaCanc = cursor.getInt(0);
//                    timestampDaCanc = cursor.getString(1);
//                    cursor.close();
                    db.delete("CUST_LISTS", "_id = 1 AND position = " + posizioneDaCanc + " AND id_canto = " + idDaCanc, null);
                    db.close();
                    updateLista();
                    cantoAdapter.notifyItemChanged(longclickedPos);
                    mShareActionProvider.setShareIntent(getDefaultIntent());
                    actionModeOk = true;
                    mode.finish();
                    Snackbar.make(getActivity().findViewById(R.id.main_content), R.string.song_removed, Snackbar.LENGTH_LONG)
                            .setAction(R.string.cancel, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    db = listaCanti.getReadableDatabase();
                                    ContentValues values = new ContentValues();
                                    values.put("_id", 1);
                                    values.put("position", posizioneDaCanc);
                                    values.put("id_canto", idDaCanc);
                                    values.put("timestamp", timestampDaCanc);
                                    db.insert("CUST_LISTS", null, values);
                                    db.close();
                                    updateLista();
                                    cantoAdapter.notifyItemChanged(longclickedPos);
                                    mShareActionProvider.setShareIntent(getDefaultIntent());
                                }
                            })
                            .setActionTextColor(getThemeUtils().accentColor())
                            .show();
                    mSwhitchMode = false;
                    break;
                case R.id.action_switch_item:
                    mSwhitchMode = true;
//                    db = listaCanti.getReadableDatabase();
//                    sql = "SELECT id_canto, timestamp" +
//                            "   FROM CUST_LISTS" +
//                            "  WHERE _id =  1 " +
//                            "    AND position = " + posizioneDaCanc +
//                            "	 AND id_canto = (SELECT _id FROM ELENCO" +
//                            "					WHERE titolo = '" + titoloDaCanc + "')";
//                    cursor = db.rawQuery(sql, null);
//                    cursor.moveToFirst();
//                    idDaCanc = cursor.getInt(0);
//                    timestampDaCanc = cursor.getString(1);
//                    cursor.close();
                    mode.setTitle(R.string.switch_started);
                    Toast.makeText(getActivity()
                            , getResources().getString(R.string.switch_tooltip)
                            , Toast.LENGTH_SHORT).show();
                    break;
            }
            return true;
        }
    }

    private void scambioCanto(View v, int position) {
//        String cantoCliccato = ((TextView) v.findViewById(idText)).getText().toString();
//        cantoCliccato = Utility.duplicaApostrofi(cantoCliccato);
        db = listaCanti.getReadableDatabase();
//        String sql = "SELECT id_canto, timestamp" +
//                "   FROM CUST_LISTS" +
//                "  WHERE _id =  1 " +
//                "    AND position = " + position +
//                "	 AND id_canto = (SELECT _id FROM ELENCO" +
//                "					WHERE titolo = '" + cantoCliccato + "')";
//        Cursor cursor = db.rawQuery(sql, null);
//        cursor.moveToFirst();
        int idNew = Integer.valueOf(((TextView) v.findViewById(R.id.text_id_canto)).getText().toString());
        String timestampNew = ((TextView) v.findViewById(R.id.text_timestamp)).getText().toString();
//        Log.i(getClass().toString(), "positionNew: " + position);
//        Log.i(getClass().toString(), "idNew: " + idNew);
//        Log.i(getClass().toString(), "timestampNew: " + timestampNew);
//        Log.i(getClass().toString(), "posizioneDaCanc: " + posizioneDaCanc);
//        Log.i(getClass().toString(), "idDaCanc: " + idDaCanc);
//        Log.i(getClass().toString(), "timestampDaCanc: " + timestampDaCanc);
        if (idNew != idDaCanc || posizioneDaCanc != position) {

            db.delete("CUST_LISTS", "_id = 1 AND position = " + position + " AND id_canto = " + idNew, null);

            ContentValues values = new ContentValues();
            values.put("id_canto", idNew);
//            values.put("timestamp", timestampNew);
            db.update("CUST_LISTS", values, "_id = 1 AND position = " + posizioneDaCanc + " AND id_canto = " + idDaCanc, null);

            values = new ContentValues();
            values.put("id_canto", idDaCanc);
            values.put("timestamp", timestampNew);
            values.put("_id", 1);
            values.put("position", position);
            db.insert("CUST_LISTS", null, values);
            db.close();

            mSwhitchMode = false;
            actionModeOk = true;
            mMode.finish();
            updateLista();
            View parent = (View) v.getParent().getParent();
            cantoAdapter.notifyItemChanged(longclickedPos);
            cantoAdapter.notifyItemChanged(Integer.valueOf(((TextView) parent.findViewById(R.id.tag)).getText().toString()));
            mShareActionProvider.setShareIntent(getDefaultIntent());
//            Toast.makeText(getActivity()
//                    , getResources().getString(R.string.switch_done)
//                    , Toast.LENGTH_SHORT).show();
            Snackbar.make(getActivity().findViewById(R.id.main_content), R.string.switch_done, Snackbar.LENGTH_SHORT)
                    .show();
        }
        else {
//            Toast.makeText(getActivity()
//                    , getResources().getString(R.string.switch_impossible)
//                    , Toast.LENGTH_SHORT).show();
            Snackbar.make(rootView, R.string.switch_impossible, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private void scambioConVuoto(View parent, int position) {
//        Log.i(getClass().toString(), "posizioneDaCanc: " + posizioneDaCanc);
//        Log.i(getClass().toString(), "idDaCanc: " + idDaCanc);
//        Log.i(getClass().toString(), "timestampDaCanc: " + timestampDaCanc);
        db = listaCanti.getReadableDatabase();
        db.delete("CUST_LISTS", "_id = 1 AND position = " + posizioneDaCanc + " AND id_canto = " + idDaCanc, null);

        ContentValues values = new ContentValues();
        values.put("id_canto", idDaCanc);
        values.put("timestamp", timestampDaCanc);
        values.put("_id", 1);
        values.put("position", position);
        db.insert("CUST_LISTS", null, values);
        db.close();

        mSwhitchMode = false;
        actionModeOk = true;
        mMode.finish();
        updateLista();
        cantoAdapter.notifyItemChanged(longclickedPos);
        cantoAdapter.notifyItemChanged(Integer.valueOf(((TextView) parent.findViewById(R.id.tag)).getText().toString()));
        mShareActionProvider.setShareIntent(getDefaultIntent());
//        Toast.makeText(getActivity()
//                , getResources().getString(R.string.switch_done)
//                , Toast.LENGTH_SHORT).show();
        Snackbar.make(getActivity().findViewById(R.id.main_content), R.string.switch_done, Snackbar.LENGTH_SHORT)
                .show();
    }

}