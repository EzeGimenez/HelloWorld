package com.visoft.network.profiles;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.iarcuschin.simpleratingbar.SimpleRatingBar;
import com.visoft.network.R;
import com.visoft.network.funcionalidades.CustomToast;
import com.visoft.network.funcionalidades.GsonerUser;
import com.visoft.network.funcionalidades.LoadingScreen;
import com.visoft.network.objects.QualityInfo;
import com.visoft.network.objects.Review;
import com.visoft.network.objects.User;
import com.visoft.network.objects.UserPro;
import com.visoft.network.util.Constants;
import com.visoft.network.util.Database;

public class UserReviewActivity extends AppCompatActivity {
    private Review review;
    private DatabaseReference database;
    private QualityInfo qualityInfo;
    private UserPro proUserReviewed;
    private SparseArray<View> mapPage;
    private FirebaseAuth mAuth;
    private LoadingScreen loadingScreen;

    //Componentes gráficas
    private Button btnNext, btnPrev;
    private ViewPager viewPager;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_review);

        loadingScreen = new LoadingScreen(this, (ViewGroup) findViewById(R.id.rootView));

        database = Database.getDatabase().getReference();
        mAuth = FirebaseAuth.getInstance();
        mapPage = new SparseArray<>();
        proUserReviewed = (UserPro) getIntent().getSerializableExtra("user");

        database.child(Constants.FIREBASE_QUALITY_CONTAINER_NAME)
                .child(proUserReviewed.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        qualityInfo = dataSnapshot.getValue(QualityInfo.class);
                        if (qualityInfo == null) {
                            qualityInfo = new QualityInfo();
                        }
                        setup();
                        btnNext.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onForthPressed();
                            }
                        });
                        btnPrev.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onBackPressed();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        viewPager = findViewById(R.id.viewPager);
    }

    private void saveReview() {
        review.setReviewerUID(mAuth.getCurrentUser().getUid());
        review.setReviewerUsername(mAuth.getCurrentUser().getDisplayName());

        float atencion = review.getAtencion();
        float tiempoResp = review.getTiempoResp();
        float calidad = review.getCalidad();

        if (atencion >= 4.5) {
            qualityInfo.setAtencion(qualityInfo.getAtencion() + 1);
        } else if (atencion <= 2 && qualityInfo.getAtencion() > 1) {
            qualityInfo.setAtencion(qualityInfo.getAtencion() - 2);
        }

        if (tiempoResp >= 4.5) {
            qualityInfo.setTiempoResp(qualityInfo.getTiempoResp() + 1);
        } else if (tiempoResp <= 2 && qualityInfo.getTiempoResp() > 1) {
            qualityInfo.setTiempoResp(qualityInfo.getTiempoResp() - 2);
        }

        if (calidad >= 4.5) {
            qualityInfo.setCalidad(qualityInfo.getCalidad() + 1);
        } else if (calidad <= 2 && qualityInfo.getCalidad() > 1) {
            qualityInfo.setCalidad(qualityInfo.getCalidad() - 2);
        }

        loadingScreen.show();

        database.child(Constants.FIREBASE_REVIEWS_CONTAINER_NAME)
                .child(proUserReviewed.getUid())
                .child(proUserReviewed.getNumberReviews() + "")
                .setValue(review).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                saveQualityInfo();
            }
        });
    }

    private void saveQualityInfo() {
        database
                .child(Constants.FIREBASE_QUALITY_CONTAINER_NAME)
                .child(proUserReviewed.getUid())
                .setValue(qualityInfo).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                updateProUserRating();
            }
        });
    }

    private void updateProUserRating() {
        final float valueToAdd = review.getRating();
        float newRating;
        if (proUserReviewed.getNumberReviews() > 0) {
            newRating = proUserReviewed.getRating() +
                    ((valueToAdd - proUserReviewed.getRating()) / proUserReviewed.getNumberReviews());
        } else {
            newRating = valueToAdd;
        }
        proUserReviewed.setRating(newRating);
        proUserReviewed.setNumberReviews(proUserReviewed.getNumberReviews() + 1);
        saveProUser(proUserReviewed);
    }

    private void saveProUser(UserPro user) {
        String json = GsonerUser.getGson().toJson(user, User.class);
        database.child(Constants.FIREBASE_USERS_PRO_CONTAINER_NAME)
                .child(proUserReviewed.getUid())
                .setValue(json).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                loadingScreen.hide();
                finish();
            }
        });


        database.child(Constants.COUNTER_CONTRACTS)
                .child("mock")
                .setValue("mock")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        database
                                .child(Constants.COUNTER_CONTRACTS)
                                .child("mock")
                                .removeValue();

                        database.child(Constants.COUNTER_CONTRACTS)
                                .child("cant")
                                .addListenerForSingleValueEvent(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        long cant = (long) dataSnapshot.getValue();
                                        database.child(Constants.COUNTER_CONTRACTS).child("cant").setValue(++cant);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                    }
                });
    }

    public void goBack() {
        CustomToast.makeText(this, getString(R.string.please_finish_review));
    }

    private void onForthPressed() {
        int shown = viewPager.getCurrentItem();

        SimpleRatingBar ratingBar = mapPage.get(shown).findViewById(R.id.ratingBar);
        switch (shown) {
            case 0:

                viewPager.setCurrentItem(shown + 1);
                btnPrev.setVisibility(View.VISIBLE);
                btnPrev.setText(getString(R.string.previo));
                review.setTiempoResp(ratingBar.getRating());

                break;
            case 1:

                viewPager.setCurrentItem(shown + 1);
                review.setAtencion(ratingBar.getRating());

                break;
            case 2:

                viewPager.setCurrentItem(shown + 1);
                review.setCalidad(ratingBar.getRating());
                btnNext.setText(getString(R.string.finalizar));

                break;
            case 3:

                EditText et = mapPage.get(shown).findViewById(R.id.editText);
                review.setMsg(et.getText().toString());
                review.setRating(ratingBar.getRating());
                saveReview();

                break;
        }

    }

    @Override
    public void onBackPressed() {
        int shown = viewPager.getCurrentItem();

        SimpleRatingBar ratingBar = null;
        if (shown > 0) {
            ratingBar = mapPage.get(shown - 1).findViewById(R.id.ratingBar);
        }
        switch (shown) {
            case 0:
                goBack();
                break;
            case 1:
                ratingBar.setRating(review.getTiempoResp());
                btnPrev.setVisibility(View.GONE);
                viewPager.setCurrentItem(shown - 1);
                break;
            case 2:
                ratingBar.setRating(review.getAtencion());
                viewPager.setCurrentItem(shown - 1);
                break;
            case 3:
                ratingBar.setRating(review.getCalidad());
                btnNext.setText(getString(R.string.siguiente));
                viewPager.setCurrentItem(shown - 1);
                break;
        }

    }

    private void setup() {
        review = new Review();
        viewPager.setAdapter(new LayoutAdapter());
    }

    private class LayoutAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            int MAX_SLIDES = 4;
            return MAX_SLIDES;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LayoutInflater inflater = (LayoutInflater) container.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.review_layout, container, false);

            TextView tvQuery = view.findViewById(R.id.tvQuery);
            switch (position) {
                case 0:
                    tvQuery.setText(getString(R.string.tiempoRespReview));
                    break;
                case 1:
                    tvQuery.setText(getString(R.string.atencionReview));
                    break;
                case 2:
                    tvQuery.setText(getString(R.string.calidadTrabajoReview));
                    break;
                case 3:
                    view.findViewById(R.id.editText).setVisibility(View.VISIBLE);
                    tvQuery.setText(getString(R.string.calificacionFinalReview));
                    break;
            }

            mapPage.put(position, view);
            viewPager.addView(view, 0);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}
