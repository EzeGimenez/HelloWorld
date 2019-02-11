package com.visoft.network;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.visoft.network.funcionalidades.AccountManager;
import com.visoft.network.funcionalidades.AccountManagerFirebasePro;
import com.visoft.network.funcionalidades.HolderCurrentAccountManager;
import com.visoft.network.profiles.ProfileActivityOwnUser;
import com.visoft.network.sign_in.SignInActivity;
import com.visoft.network.tab_chats.ChatsFragment;
import com.visoft.network.util.Constants;

public class MainActivityPro extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPref;
    private ChatsFragment chatsFragment;
    private boolean updated = false;

    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_pro);

        sharedPref = getSharedPreferences(Constants.SHARED_PREF_NAME, MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();

        AccountManager.ListenerRequestResult l = new AccountManager.ListenerRequestResult() {
            @Override
            public void onRequestResult(boolean result, int requestCode, Bundle data) {
                if (result && data.getBoolean("isNewUser", false)) {
                    showLogInScreen();
                    mAuth.signOut();
                } else {
                    updateUI(mAuth.getCurrentUser());
                }
            }
        };

        AccountManager accountManager = AccountManagerFirebasePro.getInstance(l, this);
        HolderCurrentAccountManager.setCurrent(accountManager);
        accountManager.getCurrentUser(1);

        chatsFragment = chatsFragment == null ? new ChatsFragment() : chatsFragment;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black_transparent)));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAuth.getCurrentUser() == null) {
            showLogInScreen();
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
        if (!updated && user != null && menu != null) {
            final MenuItem goToProfileItem = menu.findItem(R.id.goToProfile);

            final View view = menu.findItem(R.id.goToProfile).getActionView();
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onOptionsItemSelected(goToProfileItem);
                }
            });

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.ContainerMainPro, chatsFragment)
                    .commit();

            TextView tvusername = view.findViewById(R.id.tvUsername);
            tvusername.setText(user.getDisplayName());
            tvusername.setVisibility(View.VISIBLE);
            goToProfileItem.setVisible(true);

            if (!sharedPref.getBoolean("unreadMessages", false)) {
                //TODO no hay mensajes nuevos
            }
            updated = true;
        }
    }

    private void showLogInScreen() {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        this.menu = menu;
        updateLogIn();
        return true;
    }
}