package com.example.nexs;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nexs.adapters.SportsNewsAdapter;
import com.example.nexs.models.Article;
import com.example.nexs.models.ArticleResponse;
import com.example.nexs.models.NewCard;
import com.example.nexs.api.NexsApi;
import com.example.nexs.room.AppDatabase;
import com.example.nexs.room.viewmodel.LocalDataViewModel;
import com.example.nexs.utility.LoadingDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private Context context;
    public static Retrofit retrofit;
    public static NexsApi api;
    public static String token = "";
    public static final String BASE_URL = "https://nexs-backend.vercel.app";
//    public static final String BASE_URL = "http://192.168.1.68:8080";

    Toolbar mainToolBar;
    DrawerLayout drawerLayout;
    NavigationView navigationView;

    //Basic Info
    private TextView name, date;

    //Remove this
    Button feedButton;

    //RecyclerViews
    RecyclerView sportsRv;
    RecyclerView eduRv;
    RecyclerView internationalRv;

    RecyclerView.LayoutManager horizontalLayoutManager;
    RecyclerView.LayoutManager horizontalLayoutManager2;
    RecyclerView.LayoutManager horizontalLayoutManager3;
    SportsNewsAdapter sportsNewsAdapter;
    SportsNewsAdapter educationNewsAdapter;
    SportsNewsAdapter internationalNewsAdapter;

    //temporary data
    ArrayList<NewCard> sportsHeadline = new ArrayList<>();
    ArrayList<NewCard> educationHeadline = new ArrayList<>();
    ArrayList<NewCard> intHeadline = new ArrayList<>();

    //All articles
    public static final List<Article> articles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        setTokenListener();
        setRetrofit();

        name = findViewById(R.id.user_name_tv);
        date = findViewById(R.id.date_tv);
        setDate();
        setUsername();

        mainToolBar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_drawer);
        navigationView.setNavigationItemSelectedListener(navigationItemSelectedListener);

        feedButton = findViewById(R.id.feed_button);
        feedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FeedActivity.class));
            }
        });

        sportsRv = findViewById(R.id.sports_news_rv);
        sportsRv.setHasFixedSize(true);

        eduRv = findViewById(R.id.education_news_rv);
        eduRv.setHasFixedSize(true);

        internationalRv = findViewById(R.id.international_news_rv);
        internationalRv.setHasFixedSize(true);

        horizontalLayoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, true);
        horizontalLayoutManager2 = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, true);
        horizontalLayoutManager3 = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, true);
        sportsNewsAdapter = new SportsNewsAdapter(sportsHeadline, context);
        educationNewsAdapter = new SportsNewsAdapter(educationHeadline, context);
        internationalNewsAdapter = new SportsNewsAdapter(intHeadline, context);


        sportsRv.setLayoutManager(horizontalLayoutManager);
        sportsRv.setAdapter(sportsNewsAdapter);

        eduRv.setLayoutManager(horizontalLayoutManager2);
        eduRv.setAdapter(educationNewsAdapter);

        internationalRv.setLayoutManager(horizontalLayoutManager3);
        internationalRv.setAdapter(internationalNewsAdapter);

    }

    private void setUsername() {
        SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.SHARED_PREF_BASIC_INFO, Context.MODE_PRIVATE);
        name.setText(sharedPreferences.getString(LoginActivity.FIRST_NAME, "User"));
    }

    private void setDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM YYYY", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        date.setText(sdf.format(calendar.getTime()));
    }

    private void setRetrofit() {
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(NexsApi.class);
        //will fetch 15 most recent articles
        Call<ArticleResponse> call = api.articleGetAll((long) 0);
        call.enqueue(new Callback<ArticleResponse>() {
            @Override
            public void onResponse(Call<ArticleResponse> call, Response<ArticleResponse> response) {
                //latest articles
                List<Article> responseArticles = response.body().getArticles();
                for (Article a : responseArticles) {
                    articles.add(a);
                    if (a.getCategory().equals("Sports")) {
                        sportsHeadline.add(new NewCard(a.getId(), a.getTitle(), a.getImgUrl()));
                    } else if (a.getCategory().equals("International")) {
                        intHeadline.add(new NewCard(a.getId(), a.getTitle(), a.getImgUrl()));
                    } else {
                        educationHeadline.add(new NewCard(a.getId(), a.getTitle(), a.getImgUrl()));
                    }
                }
                sportsNewsAdapter.notifyDataSetChanged();
                educationNewsAdapter.notifyDataSetChanged();
                internationalNewsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<ArticleResponse> call, Throwable t) {

            }
        });
    }

    private void setTokenListener() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null)
            return;
        FirebaseAuth.IdTokenListener listener = new FirebaseAuth.IdTokenListener() {
            @Override
            public void onIdTokenChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (FirebaseAuth.getInstance().getCurrentUser() == null)
                    return;
                Objects.requireNonNull(firebaseAuth.getCurrentUser()).getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (!task.isSuccessful()) {
                            Log.i("news", "error");
                            return;
                        }
                        token = Objects.requireNonNull(task.getResult()).getToken();
                        Log.i("news", token);
                    }
                });
            }
        };
        FirebaseAuth.getInstance().addIdTokenListener(listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNotLoggedIn()) {
            navigationView.getMenu().getItem(2).setTitle("Log In").setIcon(ContextCompat.getDrawable(this, R.drawable.ic_baseline_login_24));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_menu_search:
                onSearchRequested();
                return true;
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean isNotLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() == null;
    }

    private NavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.nav_profile:
                    Toast.makeText(MainActivity.this, "Profile", Toast.LENGTH_SHORT).show();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    break;
                case R.id.nav_bookmarks:
                    drawerLayout.closeDrawer(GravityCompat.START);
                    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                        Toast.makeText(context, "Please Sign In", Toast.LENGTH_LONG).show();
                        break;
                    }
                    LocalDataViewModel viewModel = new ViewModelProvider(MainActivity.this).get(LocalDataViewModel.class);
                    Intent intent = new Intent(context, FeedActivity.class);
                    viewModel.getBookmarkedIds().observe(MainActivity.this, new Observer<List<String>>() {
                        @Override
                        public void onChanged(List<String> strings) {
                            if (strings.size() > 0) {
                                intent.putExtra("showBookmarks", true);
                                startActivity(intent);
                            } else {
                                Toast.makeText(context, "No Bookmarks", Toast.LENGTH_LONG).show();
                            }
                            viewModel.getBookmarkedIds().removeObserver(this);
                        }
                    });
                    break;
                default:
                    loginOrLogout();
                    drawerLayout.closeDrawer(GravityCompat.START);
            }
            ;
            return false;
        }
    };

    private void loginOrLogout() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.SHARED_PREF_BASIC_INFO, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(LoginActivity.FIRST_NAME);
            editor.apply();
            LocalDataViewModel viewModel = new ViewModelProvider(this).get(LocalDataViewModel.class);
            viewModel.deleteAllBookmarks();
            viewModel.deleteAllLikes();
            FirebaseAuth.getInstance().signOut();
            setUsername();
            navigationView.getMenu().getItem(2).setTitle("Log In").setIcon(ContextCompat.getDrawable(context, R.drawable.ic_baseline_login_24));
        }
    }
}
