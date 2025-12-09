package com.example.stay_healthy; //

import com.google.gson.annotations.SerializedName;

public class AiFoodResult {
    @SerializedName("food_name")
    public String foodName;

    @SerializedName("calories")
    public int calories;
}
