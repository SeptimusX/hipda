package net.jejer.hipda.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.typeface.FontAwesome;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import net.jejer.hipda.R;
import net.jejer.hipda.async.LoginHelper;
import net.jejer.hipda.async.SimpleListLoader;
import net.jejer.hipda.async.UpdateHelper;
import net.jejer.hipda.async.VolleyHelper;
import net.jejer.hipda.bean.HiSettingsHelper;
import net.jejer.hipda.glide.GlideHelper;
import net.jejer.hipda.utils.ACRAUtils;
import net.jejer.hipda.utils.Constants;
import net.jejer.hipda.utils.HiUtils;
import net.jejer.hipda.utils.Logger;

public class MainFrameActivity extends AppCompatActivity {

    private Fragment mOnSwipeCallback = null;
    private int mQuit = 0;

    public Drawer.Result drawerResult;
    private long volleyInitTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.v("onCreate");

        ACRAUtils.init(this);
        HiSettingsHelper.getInstance().init(this);
        GlideHelper.init(this);

        // Init Volley
        volleyInitTime = System.currentTimeMillis();
        VolleyHelper.getInstance().init(this);
        NotifyHelper.getInstance().init(this);

        super.onCreate(savedInstanceState);

        if (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT == HiSettingsHelper.getInstance().getScreenOrietation()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE == HiSettingsHelper.getInstance().getScreenOrietation()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }

        setContentView(R.layout.activity_main_frame);

        Toolbar toolbar = (Toolbar) findViewById(R.id.activity_main_toolbar);
        setSupportActionBar(toolbar);

        drawerResult = new Drawer()
                .withActivity(this)
                .withToolbar(toolbar)
//                .withHeader(R.layout.header)
                .withTranslucentStatusBar(true)
                .addDrawerItems(
                        new SecondaryDrawerItem().withName(R.string.title_drawer_search).withIdentifier(DrawerItem.SEARCH.id).withIcon(GoogleMaterial.Icon.gmd_search),
                        new SecondaryDrawerItem().withName(R.string.title_drawer_mypost).withIdentifier(DrawerItem.MY_POST.id).withIcon(GoogleMaterial.Icon.gmd_grade),
                        new SecondaryDrawerItem().withName(R.string.title_drawer_myreply).withIdentifier(DrawerItem.MY_REPLY.id).withIcon(GoogleMaterial.Icon.gmd_forum),
                        new SecondaryDrawerItem().withName(R.string.title_drawer_favorites).withIdentifier(DrawerItem.MY_FAVORITES.id).withIcon(GoogleMaterial.Icon.gmd_favorite),
                        new SecondaryDrawerItem().withName(R.string.title_drawer_sms).withIdentifier(DrawerItem.SMS.id).withIcon(GoogleMaterial.Icon.gmd_mail).withBadgeTextColor(Color.RED),
                        new SecondaryDrawerItem().withName(R.string.title_drawer_notify).withIdentifier(DrawerItem.THREAD_NOTIFY.id).withIcon(GoogleMaterial.Icon.gmd_notifications).withBadgeTextColor(Color.RED),
                        new SecondaryDrawerItem().withName(R.string.title_drawer_setting)
                                .withIdentifier(DrawerItem.SETTINGS.id)
                                .withIcon(GoogleMaterial.Icon.gmd_settings),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName(HiUtils.getForumName(HiUtils.FID_DISCOVERY))
                                .withIdentifier(HiUtils.FID_DISCOVERY)
                                .withIcon(FontAwesome.Icon.faw_cc_discover),
                        new PrimaryDrawerItem().withName(HiUtils.getForumName(HiUtils.FID_BS))
                                .withIdentifier(HiUtils.FID_BS)
                                .withIcon(FontAwesome.Icon.faw_shopping_cart),
                        new PrimaryDrawerItem().withName(HiUtils.getForumName(HiUtils.FID_GEEK))
                                .withIdentifier(HiUtils.FID_GEEK)
                                .withIcon(FontAwesome.Icon.faw_forumbee),
                        new PrimaryDrawerItem().withName(HiUtils.getForumName(HiUtils.FID_EINK))
                                .withIdentifier(HiUtils.FID_EINK)
                                .withIcon(FontAwesome.Icon.faw_book),
                        new PrimaryDrawerItem().withName(HiUtils.getForumName(HiUtils.FID_ROBOT))
                                .withIdentifier(HiUtils.FID_ROBOT)
                                .withIcon(FontAwesome.Icon.faw_reddit)
                ).addStickyDrawerItems(
                )
                .withOnDrawerItemClickListener(new DrawerItemClickListener())
                .build();

        //fix input layout problem when withTranslucentStatusBar enabled
        drawerResult.keyboardSupportEnabled(this, true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                popFragment(false);
            }
        });

        // Prepare Fragments
        getFragmentManager().addOnBackStackChangedListener(new BackStackChangedListener());
        int lastForumId = HiSettingsHelper.getInstance().getLastForumId();
        ThreadListFragment threadListFragment = new ThreadListFragment();
        Bundle argments = new Bundle();
        if (lastForumId > 0) {
            argments.putInt(ThreadListFragment.ARG_FID_KEY, lastForumId);
            threadListFragment.setArguments(argments);
        }

        getFragmentManager().beginTransaction()
                .replace(R.id.main_frame_container, threadListFragment, ThreadListFragment.class.getName())
                .commit();

        if (LoginHelper.isLoggedIn()) {
            if (HiSettingsHelper.getInstance().isUpdateCheckable()) {
                new UpdateHelper(this, true).check();
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (System.currentTimeMillis() - volleyInitTime > 30 * 1000) {
            VolleyHelper.getInstance().init(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main_frame, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Logger.v("onOptionsItemSelected");
        switch (item.getItemId()) {
            case android.R.id.home:
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                popFragment(false);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        if (drawerResult.isDrawerOpen()) {
            drawerResult.closeDrawer();
            return;
        }

        if (mOnSwipeCallback instanceof ThreadDetailFragment) {
            if (((ThreadDetailFragment) mOnSwipeCallback).hideQuickReply())
                return;
        }

        Fragment postFragment = getFragmentManager().findFragmentByTag(PostFragment.class.getName());
        if (postFragment instanceof PostFragment && ((PostFragment) postFragment).isUserInputted()) {
            Dialog dialog = new AlertDialog.Builder(this)
                    .setTitle("放弃发表？")
                    .setMessage("\n确认放弃已输入的内容吗？\n")
                    .setPositiveButton(getResources().getString(android.R.string.ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    popFragment(true);
                                }
                            })
                    .setNegativeButton(getResources().getString(android.R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).create();
            dialog.show();
        } else {
            if (!popFragment(true)) {
                mQuit++;
                if (mQuit == 1 && HiSettingsHelper.getInstance().getIsLandscape()) {
                    Toast.makeText(this, "再按一次退出HiPDA", Toast.LENGTH_LONG).show();
                } else {
                    finish();
                }
            }
        }

    }

    public boolean popFragment(boolean backPressed) {
        FragmentManager fm = getFragmentManager();
        int count = fm.getBackStackEntryCount();
        if (count > 0) {
            fm.popBackStackImmediate();
            count = fm.getBackStackEntryCount();
            if (count > 0) {
                FragmentManager.BackStackEntry backEntry = getFragmentManager().getBackStackEntryAt(count - 1);
                String str = backEntry.getName();
                Fragment fragment = getFragmentManager().findFragmentByTag(str);

                if (fragment != null) {
                    fragment.setHasOptionsMenu(true);
                }
            } else {
                Fragment fg = fm.findFragmentById(R.id.main_frame_container);
                if (fg != null) {
                    fg.setHasOptionsMenu(true);
                }
            }
            return true;
        } else {
            if (!backPressed) {
                if (drawerResult.isDrawerOpen())
                    drawerResult.closeDrawer();
                else
                    drawerResult.openDrawer();
            }
            return false;
        }

    }

    public void onNotification() {
        if (mOnSwipeCallback instanceof ThreadListFragment) {
            ((ThreadListFragment) mOnSwipeCallback).showNotification();
        }

        int smsCount = NotifyHelper.getInstance().getCntSMS();
        int threadCount = NotifyHelper.getInstance().getCntThread();
        if (smsCount + threadCount > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("您有 ");
            sb.append(smsCount).append(" 条新的短消息");
        }
        int threadNotifyIndex = drawerResult.getPositionFromIdentifier(Constants.DRAWER_THREADNOTIFY);
        if (threadNotifyIndex != -1) {
            if (threadCount > 0) {
                drawerResult.updateBadge(threadCount + "", threadNotifyIndex);
            } else {
                drawerResult.updateBadge("", threadNotifyIndex);
            }
        }
        int smsNotifyIndex = drawerResult.getPositionFromIdentifier(Constants.DRAWER_SMS);
        if (smsNotifyIndex != -1) {
            if (smsCount > 0) {
                drawerResult.updateBadge(smsCount + "", smsNotifyIndex);
            } else {
                drawerResult.updateBadge("", smsNotifyIndex);
            }
        }
    }


    public enum DrawerItem {
        SEARCH(Constants.DRAWER_SEARCH),
        MY_POST(Constants.DRAWER_MYPOST),
        MY_REPLY(Constants.DRAWER_MYREPLY),
        MY_FAVORITES(Constants.DRAWER_FAVORITES),
        SMS(Constants.DRAWER_SMS),
        THREAD_NOTIFY(Constants.DRAWER_THREADNOTIFY),
        SETTINGS(Constants.DRAWER_SETTINGS);

        public final int id;

        DrawerItem(int id) {
            this.id = id;
        }
    }

    private class DrawerItemClickListener implements Drawer.OnDrawerItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l, IDrawerItem iDrawerItem) {
            //clear all backStacks from menu click
            clearBackStacks();

            switch (iDrawerItem.getIdentifier()) {
                case Constants.DRAWER_SEARCH:    // search
                    Bundle searchBundle = new Bundle();
                    searchBundle.putInt(SimpleListFragment.ARG_TYPE, SimpleListLoader.TYPE_SEARCH);
                    SimpleListFragment searchFragment = new SimpleListFragment();
                    searchFragment.setArguments(searchBundle);
                    getFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left, R.anim.slide_out_right)
                            .add(R.id.main_frame_container, searchFragment, SimpleListFragment.class.getName())
                            .addToBackStack(SimpleListFragment.class.getName())
                            .commit();
                    break;
                case Constants.DRAWER_MYPOST:    // my posts
                    Bundle postsBundle = new Bundle();
                    postsBundle.putInt(SimpleListFragment.ARG_TYPE, SimpleListLoader.TYPE_MYPOST);
                    SimpleListFragment postsFragment = new SimpleListFragment();
                    postsFragment.setArguments(postsBundle);
                    getFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left, R.anim.slide_out_right)
                            .add(R.id.main_frame_container, postsFragment, SimpleListFragment.class.getName())
                            .addToBackStack(SimpleListFragment.class.getName())
                            .commit();
                    break;
                case Constants.DRAWER_MYREPLY:    // my reply
                    Bundle replyBundle = new Bundle();
                    replyBundle.putInt(SimpleListFragment.ARG_TYPE, SimpleListLoader.TYPE_MYREPLY);
                    SimpleListFragment replyFragment = new SimpleListFragment();
                    replyFragment.setArguments(replyBundle);
                    getFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left, R.anim.slide_out_right)
                            .add(R.id.main_frame_container, replyFragment, SimpleListFragment.class.getName())
                            .addToBackStack(SimpleListFragment.class.getName())
                            .commit();
                    break;
                case Constants.DRAWER_FAVORITES:    // my favorites
                    Bundle favBundle = new Bundle();
                    favBundle.putInt(SimpleListFragment.ARG_TYPE, SimpleListLoader.TYPE_FAVORITES);
                    SimpleListFragment favFragment = new SimpleListFragment();
                    favFragment.setArguments(favBundle);
                    getFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left, R.anim.slide_out_right)
                            .add(R.id.main_frame_container, favFragment, SimpleListFragment.class.getName())
                            .addToBackStack(SimpleListFragment.class.getName())
                            .commit();
                    break;
                case Constants.DRAWER_SMS:    // sms
                    Bundle smsBundle = new Bundle();
                    smsBundle.putInt(SimpleListFragment.ARG_TYPE, SimpleListLoader.TYPE_SMS);
                    SimpleListFragment smsFragment = new SimpleListFragment();
                    smsFragment.setArguments(smsBundle);
                    getFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left, R.anim.slide_out_right)
                            .add(R.id.main_frame_container, smsFragment, SimpleListFragment.class.getName())
                            .addToBackStack(SimpleListFragment.class.getName())
                            .commit();
                    break;
                case Constants.DRAWER_THREADNOTIFY:    // thread notify
                    Bundle notifyBundle = new Bundle();
                    notifyBundle.putInt(SimpleListFragment.ARG_TYPE, SimpleListLoader.TYPE_THREADNOTIFY);
                    SimpleListFragment notifyFragment = new SimpleListFragment();
                    notifyFragment.setArguments(notifyBundle);
                    getFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_left, R.anim.slide_out_right)
                            .add(R.id.main_frame_container, notifyFragment, SimpleListFragment.class.getName())
                            .addToBackStack(SimpleListFragment.class.getName())
                            .commit();
                    break;
                case Constants.DRAWER_SETTINGS:    // settings
                    getFragmentManager().beginTransaction()
                            .replace(R.id.main_frame_container, new SettingsFragment(), SettingsFragment.class.getName())
                            .addToBackStack(SettingsFragment.class.getName())
                            .commit();
                    break;
                default:
                    //for forums
                    int forumId = iDrawerItem.getIdentifier();
                    ThreadListFragment threadListFragment = new ThreadListFragment();
                    Bundle argments = new Bundle();
                    argments.putInt(ThreadListFragment.ARG_FID_KEY, forumId);
                    threadListFragment.setArguments(argments);

                    getFragmentManager().beginTransaction()
                            .replace(R.id.main_frame_container, threadListFragment, ThreadListFragment.class.getName())
                            .commit();
                    break;
            }
        }
    }

    private void clearBackStacks() {
        FragmentManager fm = getFragmentManager();
//        int backStackCount = fm.getBackStackEntryCount();
//        for (int cnt = 0; cnt < backStackCount; cnt++) {
//            int backStackId = fm.getBackStackEntryAt(cnt).getId();
//            fm.popBackStack(backStackId, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//        }
        while (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate();
        }

    }

    private class BackStackChangedListener implements FragmentManager.OnBackStackChangedListener {

        @Override
        public void onBackStackChanged() {
            // reset back key press counter
            mQuit = 0;

            FragmentManager fm = getFragmentManager();
            Logger.v("getBackStackEntryCount = " + String.valueOf(fm.getBackStackEntryCount()));
            if (!HiSettingsHelper.getInstance().getIsLandscape()) {
                if (fm.getBackStackEntryCount() > 0) {
//                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                } else {
//                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                }
            }
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
//        if (mEnableSwipe) {
//            mSwipeListener.onTouch(null, ev);
//        }
        return super.dispatchTouchEvent(ev);
    }

    public void registOnSwipeCallback(Fragment f) {
        mOnSwipeCallback = f;
    }
}
