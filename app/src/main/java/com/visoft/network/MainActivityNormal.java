package com.visoft.network;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.visoft.network.funcionalidades.AccountManager;
import com.visoft.network.funcionalidades.AccountManagerFirebaseNormal;
import com.visoft.network.funcionalidades.HolderCurrentAccountManager;
import com.visoft.network.profiles.ProfileActivityOwnUser;
import com.visoft.network.sign_in.SignInActivity;
import com.visoft.network.tab_chats.ChatsFragment;
import com.visoft.network.tab_contacts.MainContactsFragment;
import com.visoft.network.tab_search.HolderFirstTab;
import com.visoft.network.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class MainActivityNormal extends AppCompatActivity {
    public static final String RECEIVER_INTENT = "RECEIVER_INTENT";

    public static boolean isRunning;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPref;

    private HolderFirstTab holderFirstTab;
    private ChatsFragment chatsFragment;
    private MainContactsFragment mainContactsFragment;

    private BroadcastReceiver broadcastReceiver;
    private TabLayout tabLayout;

    private Menu menu;
    private int currentTab;
    private ViewPager viewPagerMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_normal);

        sharedPref = getSharedPreferences(Constants.SHARED_PREF_NAME, MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            showLogInScreen();
        }

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notifyNewMessage();
            }
        };

        AccountManager accountManager;
        SharedPreferences sharedPref = getSharedPreferences(Constants.SHARED_PREF_NAME, MODE_PRIVATE);
        if (sharedPref.getBoolean("asPro", false)) {
            Intent intent = new Intent(this, MainActivityPro.class);
            startActivity(intent);
            finish();
            return;
        } else {
            accountManager = AccountManagerFirebaseNormal.getInstance(null, this);
        }

        HolderCurrentAccountManager.setCurrent(accountManager);
        accountManager.getCurrentUser(1);

        holderFirstTab = holderFirstTab == null ? new HolderFirstTab() : holderFirstTab;
        chatsFragment = chatsFragment == null ? new ChatsFragment() : chatsFragment;
        mainContactsFragment = mainContactsFragment == null ? new MainContactsFragment() : mainContactsFragment;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            holderFirstTab.setLocation(new LatLng(location.getLatitude(), location.getAltitude()));
                        } else {
                            holderFirstTab.setLocation(null);
                        }
                    }
                });
    }

    public void notifyNewMessage() {
        if (viewPagerMain.getCurrentItem() != 1) {
            View view = tabLayout.getTabAt(1).getCustomView();
            view.findViewById(R.id.notImg).setVisibility(View.VISIBLE);
        }
        sharedPref.edit().putBoolean("unreadMessages", false).apply();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver),
                new IntentFilter(RECEIVER_INTENT)
        );
        isRunning = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        isRunning = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAuth.getCurrentUser() == null) {
            showLogInScreen();
        }
        if (viewPagerMain != null) {
            viewPagerMain.setCurrentItem(currentTab);
        }
    }

    @Override
    public void onBackPressed() {
        if (holderFirstTab != null && holderFirstTab.isVisible()) {
            holderFirstTab.onBackPressed();
        }
    }

    /**
     * If user is signed in it will updateUI accordingly
     */
    private void updateLogIn() {
        FirebaseUser acc = mAuth.getCurrentUser();
        updateUI(acc);
    }

    /**
     * Actualiza la interfaz de acuerdo si el usuario ya está registrado
     *
     * @param user firebase user, puede ser null
     */
    private void updateUI(@Nullable FirebaseUser user) {
        final MenuItem goToProfileItem = menu.findItem(R.id.goToProfile);

        if (user != null) {
            final View view = menu.findItem(R.id.goToProfile).getActionView();
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onOptionsItemSelected(goToProfileItem);
                }
            });
            TextView tvusername = view.findViewById(R.id.tvUsername);
            tvusername.setText(user.getDisplayName());
            tvusername.setVisibility(View.VISIBLE);
            goToProfileItem.setVisible(true);

            viewPagerMain = findViewById(R.id.ViewPagerMain);
            ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

            tabLayout = findViewById(R.id.tabLayoutMain);
            tabLayout.setupWithViewPager(viewPagerMain);

            viewPagerAdapter.addFragment(holderFirstTab, getString(R.string.buscar));
            viewPagerAdapter.addFragment(chatsFragment, getString(R.string.chats));
            viewPagerAdapter.addFragment(mainContactsFragment, getString(R.string.contactos));
            viewPagerMain.setOffscreenPageLimit(3);
            viewPagerMain.setAdapter(viewPagerAdapter);

            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    currentTab = tab.getPosition();
                    if (tab.getPosition() == 1) {
                        View view = tab.getCustomView();
                        if (view != null) {
                            view.findViewById(R.id.notImg).setVisibility(View.INVISIBLE);
                        }
                        sharedPref.edit().putBoolean("unreadMessages", false).apply();
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });

            tabLayout.getTabAt(1).setCustomView(R.layout.chat_notified);

            if (!sharedPref.getBoolean("unreadMessages", false)) {
                tabLayout.getTabAt(1).getCustomView().findViewById(R.id.notImg).setVisibility(View.INVISIBLE);
            }
        }
    }

    private void showLogInScreen() {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.goToProfile:
                Intent intent = new Intent(this, ProfileActivityOwnUser.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        this.menu = menu;
        updateLogIn();
        return true;
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
