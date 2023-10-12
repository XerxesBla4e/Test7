package com.example.budgetfoods.Student;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.budgetfoods.Adapter.FoodAdapter;
import com.example.budgetfoods.models.Food;
import com.example.budgetfoods.models.Restaurant;
import com.example.budgetfoods.R;
import com.example.budgetfoods.ViewModel.FoodViewModel;
import com.example.budgetfoods.databinding.ActivityRestaurantFoodsBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class RestaurantFoods extends AppCompatActivity {
    ActivityRestaurantFoodsBinding activityViewRestaurantsBinding;
    RecyclerView recyclerView;
    Restaurant restaurant;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    FoodViewModel foodViewModel;
    FoodAdapter foodAdapter;
    ArrayList<Restaurant> restaurants;
    TextView rateusTV;
    String resID, image, adminID;
    ImageView imageView;
    float rating;
    RatingBar ratingBar;
    String resname, restype;
    TextView resnametv, restypetv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityViewRestaurantsBinding = ActivityRestaurantFoodsBinding.inflate(getLayoutInflater());
        setContentView(activityViewRestaurantsBinding.getRoot());

        initViews(activityViewRestaurantsBinding);

        foodViewModel = new ViewModelProvider(this, (ViewModelProvider.Factory) ViewModelProvider.AndroidViewModelFactory
                .getInstance((Application) this.getApplicationContext())).get(FoodViewModel.class);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        if (intent.hasExtra("restaurantModel")) {
            restaurant = intent.getParcelableExtra("restaurantModel");
            resID = restaurant.getRId();
            image = restaurant.getImage();
            adminID = restaurant.getUid();
            rating = restaurant.getRatings();
            resname = restaurant.getRestaurantname();
            resnametv.setText(resname);
            restype = restaurant.getDescription();
            restypetv.setText(restype);
            if (rating > 0.0) {
                ratingBar.setRating(rating);
            } else {
                ratingBar.setRating(0.0f);
            }
            loadImage(image);

            retrieveRestaurantFood(resID, adminID);
        }

        rateusTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRatingDialog(restaurant);
            }
        });

    }

    private void showRatingDialog(Restaurant restaurant) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(RestaurantFoods.this);
        View dialogView = LayoutInflater.from(RestaurantFoods.this).inflate(R.layout.dialog_rate_restaurant, null);
        dialogBuilder.setView(dialogView);

        RatingBar ratingBarRestaurant = dialogView.findViewById(R.id.ratingBarRestaurant);
        Button btnSubmitRating = dialogView.findViewById(R.id.btnSubmitRating);

        AlertDialog alertDialog = dialogBuilder.create();

        btnSubmitRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float rating = ratingBarRestaurant.getRating();
                updateRestaurantRating(restaurant, rating);
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }


    private void updateRestaurantRating(Restaurant restaurant, float newRating) {
        float currentRating = restaurant.getRatings();
        int totalRatings = restaurant.getTotalratings();

        // Calculate the new average rating and total number of ratings
        float updatedRating = (currentRating * totalRatings + newRating) / (totalRatings + 1);
        int updatedTotalRatings = totalRatings + 1;

        // Update the Firestore document with the new rating and total ratings
        DocumentReference restaurantRef = firestore.collection("users")
                .document(adminID)
                .collection("Restaurants")
                .document(resID);
        restaurantRef.update("ratings", updatedRating, "totalratings", updatedTotalRatings)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "Rating Posted Successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle failure
                    }
                });
    }


    private void loadImage(String image) {
        try {
            if (image != null) {
                Picasso.get().load(image).into(imageView);
            } else {
                Picasso.get().load(R.drawable.kfc).into(imageView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void retrieveRestaurantFood(String resID, String adminID) {
        // Get the reference to the "Food" collection for the specified restaurant
        CollectionReference foodCollection = firestore.collection("users")
                .document(adminID)
                .collection("Restaurants")
                .document(resID)
                .collection("Food");

        // Perform the query
        foodCollection.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Food> foodItemList = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            // Here, FoodItem is a custom class representing a single food item
                            Food foodItem = document.toObject(Food.class);
                            foodItemList.add(foodItem);
                        }
                        // Update the adapter with the retrieved food items using the new constructor
                        foodAdapter = new FoodAdapter(foodViewModel);
                        foodAdapter.updateFoodList(foodItemList); // Set the retrieved food items to the adapter
                        recyclerView.setAdapter(foodAdapter);
                    } else {
                        // Handle errors here
                    }
                });

    }

    @SuppressLint("NotifyDataSetChanged")
    private void initViews(ActivityRestaurantFoodsBinding activityViewRestaurantsBinding) {
        imageView = activityViewRestaurantsBinding.backgroundImage;
        foodAdapter = new FoodAdapter(foodViewModel); // Initialize the adapter with an empty list initially
        restaurants = new ArrayList<>();
        recyclerView = activityViewRestaurantsBinding.resrecview;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        foodAdapter.notifyDataSetChanged();
        ratingBar = activityViewRestaurantsBinding.ratingBar22;
        rateusTV = activityViewRestaurantsBinding.rateUsText;
        resnametv = activityViewRestaurantsBinding.restaurantNameText;
        restypetv = activityViewRestaurantsBinding.foodTypeText;
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }
}